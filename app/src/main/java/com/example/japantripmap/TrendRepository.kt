package com.example.japantripmap

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * ホーム・県詳細の「トレンド（YouTube Shorts）」1 本分のメタデータ。iOS 版 TrendVideo。
 *
 * YouTube 規約対応（iOS 計画書 §1）:
 * - 要件B: 保持するのは動画 ID とサムネ URL 等のメタデータのみ。動画本体は扱わない。
 * - 要件E: Shorts（60 秒以下）のみが入り、縦型プレイヤーで再生する。
 */
@Serializable
data class TrendVideo(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val channelTitle: String = "",
    val publishedAt: String = "",
    val durationSeconds: Int = 0,
    val isShort: Boolean = true,
) {
    /** 尺表示（"0:42"）。 */
    val durationText: String get() = "%d:%02d".format(durationSeconds / 60, durationSeconds % 60)
}

/** 「都道府県 × カテゴリ」ごとの Shorts のまとまり。iOS 版 TrendGroup。 */
@Serializable
data class TrendGroup(
    /** 県キー（iOS Prefecture.rawValue と同じ。例 "tokyo"）。Android では Prefecture.slug と一致。 */
    val prefKey: String,
    val prefName: String,
    /** バッチが書いたカテゴリ名（日本語固定）。表示は [categoryKey] からローカライズする。 */
    val category: String,
    /** cafe / spot / gourmet。 */
    val categoryKey: String,
    /** 最終更新（エポックミリ秒）。要件A（30 日ルール）の起点。 */
    val updatedAt: Long,
    val videos: List<TrendVideo>,
) {
    val prefecture: Prefecture? get() = Prefecture.entries.firstOrNull { it.slug == prefKey }
}

/** トレンドの読み込み状態。 */
sealed interface TrendLoadState {
    data object Idle : TrendLoadState
    data object Loading : TrendLoadState
    data class Loaded(val groups: List<TrendGroup>) : TrendLoadState
    data class Failed(val isNetwork: Boolean) : TrendLoadState
}

/**
 * Firestore に置かれたトレンド（YouTube Shorts）を読むリポジトリ。iOS 版 TrendRepository + TrendCache。
 *
 * - バッチ（JapanTripMapBatch）が名前付き DB `japantripmap-youtube` の
 *   `trends_bundle/groupA`・`groupB`（47 県を 2 群に分割）へ毎日書き込む。アプリは読むだけ。
 * - Firebase SDK は使わず、公開読み取り可のドキュメントを Firestore REST API で取得する
 *   （セキュリティルールで read: true / write: false。Android 用の設定ファイル登録が不要）。
 * - まずキャッシュを即表示し、毎朝 8 時（JST）の更新境界をまたいだときだけ再取得する（1 日 1 回）。
 * - 要件A: 30 日より古いグループは表示しない。キャッシュ自体も 30 日を超えたら削除する。
 */
object TrendRepository {
    private const val PROJECT_ID = "japantripmap"
    private const val DATABASE_ID = "japantripmap-youtube"
    private const val BASE_URL =
        "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/$DATABASE_ID/documents/trends_bundle"

    /** 要件A: 保持してよい上限（30 日）。表示ガードとキャッシュ削除で共有する。 */
    private const val MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000

    /** バッチ更新後に再取得してよくなる時刻（JST の時）。バッチ遅延を吸収するため 8 時。 */
    private const val REFRESH_HOUR_JST = 8

    private const val CACHE_FILE = "trend_cache.json"
    private const val LAST_FETCH_KEY = "trend.lastFetchAt"

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private var cacheDir: File? = null
    private var prefs: SharedPreferences? = null

    var state by mutableStateOf<TrendLoadState>(TrendLoadState.Idle)
        private set

    fun init(context: Context) {
        if (prefs != null) return
        cacheDir = context.applicationContext.filesDir
        prefs = context.applicationContext.getSharedPreferences("trend_cache", Context.MODE_PRIVATE)
    }

    /** ロード済みの全グループ（未ロードなら空）。 */
    val loadedGroups: List<TrendGroup>
        get() = (state as? TrendLoadState.Loaded)?.groups ?: emptyList()

    /** 指定県のグループ（カテゴリ別）。 */
    fun groups(prefecture: Prefecture): List<TrendGroup> =
        loadedGroups.filter { it.prefKey == prefecture.slug }

    /** 指定県・カテゴリの動画が 1 本以上あるグループ。 */
    fun group(prefecture: Prefecture, categoryKey: String): TrendGroup? =
        groups(prefecture).firstOrNull { it.categoryKey == categoryKey && it.videos.isNotEmpty() }

    /**
     * 画面表示時に呼ぶ。キャッシュ優先で、必要なときだけサーバーから取得する。
     * 既にロード中・ロード済みなら何もしない（ホームと県詳細で重複取得しない）。
     */
    suspend fun loadIfNeeded() {
        if (state !is TrendLoadState.Idle) return
        load()
    }

    private suspend fun load() = mutex.withLock {
        if (state !is TrendLoadState.Idle) return@withLock
        readCache()?.let { cached ->
            val groups = applyGuards(cached)
            if (groups.isNotEmpty()) state = TrendLoadState.Loaded(groups)
        }
        if (!shouldRefresh() && state is TrendLoadState.Loaded) return@withLock
        fetchFromServer()
    }

    /** 取得失敗時の「再試行」用。 */
    suspend fun retry() = mutex.withLock { fetchFromServer() }

    private suspend fun fetchFromServer() {
        if (state !is TrendLoadState.Loaded) state = TrendLoadState.Loading
        try {
            val groups = withContext(Dispatchers.IO) {
                coroutineScope {
                    val a = async { fetchDocument("groupA") }
                    val b = async { fetchDocument("groupB") }
                    a.await() + b.await()
                }
            }
            val guarded = applyGuards(groups)
            writeCache(guarded)
            state = TrendLoadState.Loaded(guarded)
        } catch (e: Exception) {
            if (state is TrendLoadState.Loaded) return
            val cached = readCache()?.let { applyGuards(it) }
            state = if (!cached.isNullOrEmpty()) {
                TrendLoadState.Loaded(cached)
            } else {
                TrendLoadState.Failed(isNetwork = e is IOException)
            }
        }
    }

    // ---- Firestore REST ----

    private fun fetchDocument(name: String): List<TrendGroup> {
        val conn = (URL("$BASE_URL/$name").openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
        }
        try {
            if (conn.responseCode == 404) return emptyList()
            if (conn.responseCode !in 200..299) throw IllegalStateException("HTTP ${conn.responseCode}")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val fields = json.parseToJsonElement(body).jsonObject["fields"]?.jsonObject ?: return emptyList()
            val groups = fields["groups"]?.let { arrayOf(it) } ?: return emptyList()
            return groups.mapNotNull { decodeGroup(mapOf(it)) }
        } finally {
            conn.disconnect()
        }
    }

    private fun decodeGroup(f: JsonObject?): TrendGroup? {
        f ?: return null
        val prefKey = str(f["prefKey"]) ?: return null
        val prefName = str(f["prefName"]) ?: return null
        val category = str(f["category"]) ?: return null
        val categoryKey = str(f["categoryKey"]) ?: return null
        // 要件A: updatedAt が無いデータは信用しない。
        val updatedAt = timestamp(f["updatedAt"]) ?: return null
        val videos = f["videos"]?.let { arrayOf(it) }.orEmpty().mapNotNull { decodeVideo(mapOf(it)) }
        return TrendGroup(prefKey, prefName, category, categoryKey, updatedAt, videos)
    }

    private fun decodeVideo(f: JsonObject?): TrendVideo? {
        f ?: return null
        val videoId = str(f["videoId"])?.takeIf { it.isNotEmpty() } ?: return null
        val title = str(f["title"]) ?: return null
        val duration = int(f["durationSeconds"]) ?: 0
        return TrendVideo(
            videoId = videoId,
            title = title,
            thumbnailUrl = str(f["thumbnailUrl"]) ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
            channelTitle = str(f["channelTitle"]) ?: "",
            publishedAt = str(f["publishedAt"]) ?: "",
            durationSeconds = duration,
            isShort = bool(f["isShort"]) ?: (duration <= 60),
        )
    }

    // Firestore REST の型付き値（{"stringValue": ...} 等）を取り出すヘルパー。
    private fun value(e: JsonElement?, type: String): JsonElement? = (e as? JsonObject)?.get(type)
    private fun str(e: JsonElement?): String? = value(e, "stringValue")?.jsonPrimitive?.content
    private fun int(e: JsonElement?): Int? =
        value(e, "integerValue")?.jsonPrimitive?.content?.toIntOrNull()
            ?: value(e, "doubleValue")?.jsonPrimitive?.content?.toDoubleOrNull()?.toInt()
    private fun bool(e: JsonElement?): Boolean? = value(e, "booleanValue")?.jsonPrimitive?.booleanOrNull
    private fun timestamp(e: JsonElement?): Long? =
        value(e, "timestampValue")?.jsonPrimitive?.content?.let { parseRfc3339(it) }

    /** Firestore の timestampValue（例 "2026-10-24T21:23:11.933369Z"）をエポックミリ秒に。minSdk 24 のため java.time は使わない。 */
    private fun parseRfc3339(text: String): Long? = runCatching {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val base = format.parse(text.substring(0, 19))!!.time
        val fraction = Regex("""\.(\d+)""").find(text.substring(19))?.groupValues?.get(1)
        base + (fraction?.padEnd(3, '0')?.substring(0, 3)?.toLong() ?: 0L)
    }.getOrNull()
    private fun arrayOf(e: JsonElement): JsonArray =
        (value(e, "arrayValue") as? JsonObject)?.get("values")?.jsonArray ?: JsonArray(emptyList())
    private fun mapOf(e: JsonElement): JsonObject? =
        (value(e, "mapValue") as? JsonObject)?.get("fields")?.jsonObject

    // ---- ガード・キャッシュ ----

    /** 要件A ガード＋空グループ除外＋新しい順。 */
    private fun applyGuards(groups: List<TrendGroup>): List<TrendGroup> {
        val now = System.currentTimeMillis()
        return groups
            .filter { now - it.updatedAt <= MAX_AGE_MS }
            .filter { it.videos.isNotEmpty() }
            .sortedByDescending { it.updatedAt }
    }

    private fun cacheFile(): File? = cacheDir?.let { File(it, CACHE_FILE) }

    /**
     * 保存済みのトレンド。要件A: 最終取得から 30 日を超えたキャッシュはファイルごと削除する
     * （「出さない」だけでは不十分で、保存データ自体を更新または削除する必要があるため）。
     */
    private fun readCache(): List<TrendGroup>? {
        val file = cacheFile() ?: return null
        if (!file.exists()) return null
        val last = prefs?.getLong(LAST_FETCH_KEY, 0L) ?: 0L
        if (last > 0 && System.currentTimeMillis() - last > MAX_AGE_MS) {
            purge()
            return null
        }
        return runCatching { json.decodeFromString<List<TrendGroup>>(file.readText()) }
            .getOrElse {
                purge()
                null
            }
    }

    private fun writeCache(groups: List<TrendGroup>) {
        runCatching { cacheFile()?.writeText(json.encodeToString(groups)) }
        prefs?.edit()?.putLong(LAST_FETCH_KEY, System.currentTimeMillis())?.apply()
    }

    private fun purge() {
        cacheFile()?.delete()
        prefs?.edit()?.remove(LAST_FETCH_KEY)?.apply()
    }

    /** 前回取得が直近の更新境界（毎朝 8 時 JST）より前なら再取得する。 */
    private fun shouldRefresh(): Boolean {
        val last = prefs?.getLong(LAST_FETCH_KEY, 0L) ?: 0L
        if (last == 0L) return true
        return last < lastRefreshBoundary()
    }

    private fun lastRefreshBoundary(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"))
        val now = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, REFRESH_HOUR_JST)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (now < cal.timeInMillis) cal.add(Calendar.DAY_OF_YEAR, -1)
        return cal.timeInMillis
    }
}
