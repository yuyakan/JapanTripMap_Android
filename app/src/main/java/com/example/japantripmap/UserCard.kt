package com.example.japantripmap

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/** カードを追加するセクションの種類。iOS 版 UserCardKind を移植。 */
@Serializable
enum class UserCardKind {
    ATTRACTION, GOURMET, CAFE, SOUVENIR, ONSEN, NATURE, FESTIVAL, OTHER_FESTIVAL;

    /** プランへ追加したときの項目種別。 */
    val planCategory: PlanItemCategory
        get() = when (this) {
            ATTRACTION -> PlanItemCategory.ATTRACTION
            GOURMET, CAFE -> PlanItemCategory.GOURMET
            SOUVENIR -> PlanItemCategory.SOUVENIR
            ONSEN -> PlanItemCategory.ONSEN
            NATURE -> PlanItemCategory.NATURE
            FESTIVAL, OTHER_FESTIVAL -> PlanItemCategory.FESTIVAL
        }

    /** セクション見出しと揃えたアイコン。 */
    val icon: ImageVector
        get() = when (this) {
            ATTRACTION -> Icons.Filled.Place
            GOURMET -> Icons.Filled.Restaurant
            CAFE -> Icons.Filled.LocalCafe
            SOUVENIR -> Icons.Filled.CardGiftcard
            ONSEN -> Icons.Filled.Spa
            NATURE -> Icons.Filled.Park
            FESTIVAL -> Icons.Filled.Celebration
            OTHER_FESTIVAL -> Icons.Filled.AcUnit
        }

    /** セクション見出しと揃えた基調色。 */
    val accent: Color
        get() = when (this) {
            ATTRACTION, CAFE -> PlanTheme.Primary
            GOURMET -> GourmetAccent
            SOUVENIR -> SouvenirAccent
            ONSEN -> OnsenAccent
            NATURE -> NatureSectionAccent
            FESTIVAL -> FestivalSectionAccent
            OTHER_FESTIVAL -> SeasonalSectionAccent
        }

    /** 食べログ検索を出す種類か（店を探す対象のみ）。 */
    val isRestaurant: Boolean get() = this == GOURMET || this == CAFE
}

/** ユーザーが県詳細の各セクションへ自分で追加する 1 枚のカード。iOS 版 UserCard を移植。 */
@Serializable
data class UserCard(
    val id: String = UUID.randomUUID().toString(),
    /** 県（Prefecture の enum 名）。 */
    val prefectureName: String,
    val kind: UserCardKind,
    val name: String,
    val description: String = "",
    /** 入力された URL。開くときは [normalizedUrl] を通す。 */
    val url: String? = null,
    /** 表紙写真（[UserCardStore] が保存）のファイル名。 */
    val photoFileName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeName: String? = null,
    val address: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val prefecture: Prefecture get() = Prefecture.valueOf(prefectureName)

    val hasCoordinate: Boolean get() = latitude != null && longitude != null

    /** 詳細画面で開ける URL。形式が不正なら null。 */
    val openableUrl: String? get() = url?.let(::normalizedUrl)

    /** 共通の詳細画面へ渡す形にする。観光スポット詳細と同じく常に写真ありの見た目にする。 */
    fun toSpotDetail(): SpotDetail = SpotDetail(
        title = name,
        prefecture = prefecture,
        accent = kind.accent,
        icon = kind.icon,
        description = description,
        planCategory = kind.planCategory,
        tabelogKeyword = if (kind.isRestaurant) name else null,
        alwaysPhotoStyle = true,
        userCardId = id,
    )

    /** 位置カード（[PlanItemLocationCard]）で表示するための項目。 */
    fun toLocationItem(): PlanItem = PlanItem(
        id = id,
        category = kind.planCategory,
        prefectureName = prefecture.displayName,
        name = name,
        latitude = latitude,
        longitude = longitude,
        placeName = placeName,
        address = address,
    )
}

/**
 * 入力された文字列を、開ける Web の URL にする。
 * スキームを省いた入力（example.com）は https を補う。http(s) 以外は開かせない。
 */
fun normalizedUrl(text: String): String? {
    val trimmed = text.trim()
    if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
    val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
    val url = runCatching { URL(withScheme) }.getOrNull() ?: return null
    if (url.protocol.lowercase() !in setOf("http", "https") || '.' !in url.host) return null
    return withScheme
}

/** 指定県・セクションのカードを新しい順で返す。 */
fun List<UserCard>.inSection(prefecture: Prefecture, kind: UserCardKind): List<UserCard> =
    filter { it.prefectureName == prefecture.name && it.kind == kind }.sortedByDescending { it.createdAt }

/**
 * カードの編集をプラン項目へ反映する。項目はカードを名前で引くので改名に追従させ、
 * 共有テキストに使う説明も揃える。
 */
fun List<TravelPlan>.withUserCardSynced(previous: UserCard, updated: UserCard): List<TravelPlan> {
    val category = previous.kind.planCategory
    val prefectureName = previous.prefecture.displayName
    return map { plan ->
        plan.copy(
            items = plan.items.map {
                if (it.category == category && it.prefectureName == prefectureName && it.name == previous.name) {
                    it.copy(name = updated.name, detail = updated.description)
                } else {
                    it
                }
            },
        )
    }
}

/**
 * ユーザーが作成したカードの保存ストア。iOS 版 UserCardStore を移植。
 * カードは SharedPreferences に JSON で、表紙写真は JSON に入れず filesDir に JPEG で保存する。
 * 使う前に MainActivity から [init] を一度呼ぶこと。
 */
object UserCardStore {
    private const val TAG = "UserCardStore"
    private const val KEY = "cards_json"
    private const val MAX_PHOTO_DIMENSION = 1600
    private const val JPEG_QUALITY = 80

    private val json = Json { ignoreUnknownKeys = true }
    private var prefs: SharedPreferences? = null
    private var photoDirectory: File? = null

    var cards by mutableStateOf<List<UserCard>>(emptyList())
        private set

    fun init(context: Context) {
        if (prefs != null) return
        val app = context.applicationContext
        val p = app.getSharedPreferences("user_cards", Context.MODE_PRIVATE)
        prefs = p
        photoDirectory = File(app.filesDir, "user_card_photos")
        val raw = p.getString(KEY, null) ?: return
        cards = runCatching { json.decodeFromString<List<UserCard>>(raw) }.getOrDefault(emptyList())
    }

    private fun persist() {
        prefs?.edit()?.putString(KEY, json.encodeToString<List<UserCard>>(cards))?.apply()
    }

    fun cards(prefecture: Prefecture, kind: UserCardKind): List<UserCard> = cards.inSection(prefecture, kind)

    fun card(id: String): UserCard? = cards.firstOrNull { it.id == id }

    /** プラン項目（種別・県・名前のみ保持）に対応するカードを探す。 */
    fun card(category: PlanItemCategory, prefecture: Prefecture, name: String): UserCard? =
        cards.firstOrNull {
            it.kind.planCategory == category && it.prefectureName == prefecture.name && it.name == name
        }

    fun add(card: UserCard) {
        cards = cards + card
        persist()
    }

    /** カードを更新して、更新前のカードを返す。呼び出し側はこれでプラン項目を同期する。 */
    fun update(card: UserCard): UserCard? {
        val previous = card(card.id) ?: return null
        cards = cards.map { if (it.id == card.id) card else it }
        if (previous.photoFileName != card.photoFileName) deletePhoto(previous.photoFileName)
        persist()
        return previous
    }

    fun delete(card: UserCard) {
        cards = cards.filterNot { it.id == card.id }
        persist()
        deletePhoto(card.photoFileName)
    }

    // MARK: - 表紙写真

    fun photoFile(card: UserCard): File? {
        val name = card.photoFileName ?: return null
        return photoDirectory?.let { File(it, name) }
    }

    /**
     * 写真を縮小・正立させて保存し、ファイル名を返す。保存できなければ null。
     * デコードと書き込みをするのでメインスレッド以外から呼ぶこと。
     */
    fun savePhoto(context: Context, uri: Uri): String? {
        val directory = photoDirectory ?: return null
        return try {
            val bitmap = decodeResized(context, uri) ?: return null
            directory.mkdirs()
            val name = "${UUID.randomUUID()}.jpg"
            File(directory, name).outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
            }
            name
        } catch (e: Exception) {
            Log.w(TAG, "写真を保存できませんでした", e)
            null
        }
    }

    private fun deletePhoto(name: String?) {
        if (name == null) return
        photoDirectory?.let { File(it, name).delete() }
    }

    /** 長辺を上限に収める。カメラの原寸は端末の保存容量とメモリを圧迫するため。 */
    private fun decodeResized(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = photoSampleSize(bounds.outWidth, bounds.outHeight, MAX_PHOTO_DIMENSION)
        }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        val (width, height) = fittedPhotoSize(decoded.width, decoded.height, MAX_PHOTO_DIMENSION)
        val scaled = Bitmap.createScaledBitmap(decoded, width, height, true)

        val rotation = resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        if (rotation == 0) return scaled
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        return Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
    }

    private val ExifInterface.rotationDegrees: Int
        get() = when (getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
}

/** 長辺が上限の 2 倍未満になるまで間引く量。最終的な縮小は [fittedPhotoSize] で行う。 */
internal fun photoSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var sample = 1
    while (max(width, height) / (sample * 2) >= maxDimension) sample *= 2
    return sample
}

/** 長辺を上限に収めた大きさ。上限以下ならそのまま返す。 */
internal fun fittedPhotoSize(width: Int, height: Int, maxDimension: Int): Pair<Int, Int> {
    val longest = max(width, height)
    if (longest <= maxDimension) return width to height
    val ratio = maxDimension.toFloat() / longest
    return (width * ratio).roundToInt() to (height * ratio).roundToInt()
}
