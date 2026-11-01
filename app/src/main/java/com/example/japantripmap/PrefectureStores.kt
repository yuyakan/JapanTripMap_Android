package com.example.japantripmap

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 都道府県に紐づくユーザー状態（お気に入り・最近見た・訪問済み）の保存ストア群。
 * iOS 版 FavoritePrefectureStore / RecentPrefectureStore / VisitedPrefectureStore を移植。
 *
 * どれもアプリ全体で 1 つの情報源を共有するシングルトン。Compose が変化を拾えるよう
 * 状態は mutableStateOf で持ち、SharedPreferences に県の enum 名で永続化する。
 * 使う前に MainActivity から [PrefectureStores.init] を一度呼ぶこと。
 */
object PrefectureStores {
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("prefecture_stores", Context.MODE_PRIVATE)
        prefs = p
        FavoritePrefectureStore.load(p)
        RecentPrefectureStore.load(p)
        VisitedPrefectureStore.load(p)
    }

    internal fun save(key: String, prefectures: Collection<Prefecture>) {
        prefs?.edit()?.putString(key, prefectures.joinToString(",") { it.name })?.apply()
    }

    internal fun load(p: SharedPreferences, key: String): List<Prefecture> =
        p.getString(key, null)
            ?.split(",")
            ?.mapNotNull { name -> Prefecture.entries.firstOrNull { it.name == name } }
            ?: emptyList()
}

/** 「お気に入り都道府県」。ホームの「お気に入り」列の元データ。県詳細のハートでトグルする。 */
object FavoritePrefectureStore {
    private const val KEY = "favorite.prefectures"

    var favorites by mutableStateOf<Set<Prefecture>>(emptySet())
        private set

    internal fun load(p: SharedPreferences) {
        favorites = PrefectureStores.load(p, KEY).toSet()
    }

    fun toggle(prefecture: Prefecture) {
        favorites = if (prefecture in favorites) favorites - prefecture else favorites + prefecture
        PrefectureStores.save(KEY, favorites)
    }

    fun isFavorite(prefecture: Prefecture): Boolean = prefecture in favorites
}

/**
 * 「最近見た都道府県」。県詳細を開いた瞬間に [record] する。
 * 順序が意味を持つ（先頭が最も最近・重複は先頭へ繰り上げ・上限 [MAX_COUNT] 件）。
 */
object RecentPrefectureStore {
    private const val KEY = "recent.prefectures"
    private const val MAX_COUNT = 3

    var recents by mutableStateOf<List<Prefecture>>(emptyList())
        private set

    internal fun load(p: SharedPreferences) {
        recents = PrefectureStores.load(p, KEY).take(MAX_COUNT)
    }

    fun record(prefecture: Prefecture) {
        if (recents.firstOrNull() == prefecture) return
        recents = (listOf(prefecture) + recents.filterNot { it == prefecture }).take(MAX_COUNT)
        PrefectureStores.save(KEY, recents)
    }
}

/**
 * 「訪問済み都道府県」。訪問済みマップのタップでトグルし、プランを「旅行済み」にした瞬間に
 * そのプランの県を追加する（後勝ち）。訪問済みマップ・踏破率の唯一の情報源。
 */
object VisitedPrefectureStore {
    private const val KEY = "visited.prefectures"

    var visited by mutableStateOf<Set<Prefecture>>(emptySet())
        private set

    internal fun load(p: SharedPreferences) {
        visited = PrefectureStores.load(p, KEY).toSet()
    }

    fun toggle(prefecture: Prefecture) {
        visited = if (prefecture in visited) visited - prefecture else visited + prefecture
        PrefectureStores.save(KEY, visited)
    }

    fun setVisited(prefecture: Prefecture, isVisited: Boolean) {
        if ((prefecture in visited) != isVisited) toggle(prefecture)
    }

    fun markVisited(prefectures: Collection<Prefecture>) {
        val merged = visited + prefectures
        if (merged.size == visited.size) return
        visited = merged
        PrefectureStores.save(KEY, visited)
    }

    fun isVisited(prefecture: Prefecture): Boolean = prefecture in visited
}

/** 下タブ。並びは iOS 版 ContentView と同じ（真ん中がホーム）。 */
enum class MainTab { MAP, NEARBY, HOME, FESTIVAL, PLAN }

/**
 * タブ切替など「画面をまたぐ遷移」の唯一の共有状態。iOS 版 AppRouter を移植。
 * - ホームの「マイプランへ」→ マイプランタブ（プランが無ければ新規作成シートも予約）
 * - ホームのルーレット導線 → マップタブ
 */
object AppRouter {
    /** 現在のタブ（起動時はホーム）。 */
    var selectedTab by mutableStateOf(MainTab.HOME)

    /** マイプランを開いたら新規作成シートを出す予約。消化したら false に戻す。 */
    var pendingNewPlan by mutableStateOf(false)

    fun openMyPlans(newPlan: Boolean = false) {
        pendingNewPlan = newPlan
        selectedTab = MainTab.PLAN
    }

    fun openRouletteMap() {
        selectedTab = MainTab.MAP
    }
}

/**
 * 地方（都道府県を束ねる単位）。iOS 版 JapanRegion。訪問済みの地方別集計や検索で使う。
 */
enum class JapanRegion(val key: String) {
    HOKKAIDO("hokkaido"), TOHOKU("tohoku"), KANTO("kanto"), KOSHINETSU("koshinetsu"),
    HOKURIKU("hokuriku"), TOKAI("tokai"), KINKI("kinki"), CHUGOKU("chugoku"),
    SHIKOKU("shikoku"), KYUSHU("kyushu"), OKINAWA("okinawa");

    val localizedName: String get() = localizedRegionNameFor(key)

    /** この地方の都道府県（Prefecture の定義順）。 */
    val prefectures: List<Prefecture> get() = Prefecture.entries.filter { it.regionKey == key }
}

/** 検索用のひらがな読み（都・道・府・県を除く）。iOS 版 Prefecture.hiraganaReading。 */
val Prefecture.hiraganaReading: String
    get() = when (this) {
        Prefecture.HOKKAIDO -> "ほっかいどう"
        Prefecture.AOMORI -> "あおもり"
        Prefecture.IWATE -> "いわて"
        Prefecture.AKITA -> "あきた"
        Prefecture.MIYAGI -> "みやぎ"
        Prefecture.YAMAGATA -> "やまがた"
        Prefecture.FUKUSHIMA -> "ふくしま"
        Prefecture.IBARAKI -> "いばらき"
        Prefecture.CHIBA -> "ちば"
        Prefecture.TOCHIGI -> "とちぎ"
        Prefecture.GUNMA -> "ぐんま"
        Prefecture.SAITAMA -> "さいたま"
        Prefecture.TOKYO -> "とうきょう"
        Prefecture.KANAGAWA -> "かながわ"
        Prefecture.NIIGATA -> "にいがた"
        Prefecture.NAGANO -> "ながの"
        Prefecture.YAMANASHI -> "やまなし"
        Prefecture.SHIZUOKA -> "しずおか"
        Prefecture.AICHI -> "あいち"
        Prefecture.MIE -> "みえ"
        Prefecture.GIFU -> "ぎふ"
        Prefecture.FUKUI -> "ふくい"
        Prefecture.ISHIKAWA -> "いしかわ"
        Prefecture.TOYAMA -> "とやま"
        Prefecture.SHIGA -> "しが"
        Prefecture.KYOTO -> "きょうと"
        Prefecture.HYOGO -> "ひょうご"
        Prefecture.NARA -> "なら"
        Prefecture.WAKAYAMA -> "わかやま"
        Prefecture.OSAKA -> "おおさか"
        Prefecture.TOTTORI -> "とっとり"
        Prefecture.OKAYAMA -> "おかやま"
        Prefecture.HIROSHIMA -> "ひろしま"
        Prefecture.YAMAGUCHI -> "やまぐち"
        Prefecture.SHIMANE -> "しまね"
        Prefecture.KAGAWA -> "かがわ"
        Prefecture.TOKUSHIMA -> "とくしま"
        Prefecture.KOCHI -> "こうち"
        Prefecture.EHIME -> "えひめ"
        Prefecture.FUKUOKA -> "ふくおか"
        Prefecture.OITA -> "おおいた"
        Prefecture.MIYAZAKI -> "みやざき"
        Prefecture.KAGOSHIMA -> "かごしま"
        Prefecture.KUMAMOTO -> "くまもと"
        Prefecture.SAGA -> "さが"
        Prefecture.NAGASAKI -> "ながさき"
        Prefecture.OKINAWA -> "おきなわ"
    }

/** カタカナをひらがなへ（ひらがな・英字・漢字はそのまま）。検索の正規化に使う。 */
fun String.katakanaToHiragana(): String = buildString(length) {
    for (c in this@katakanaToHiragana) {
        append(if (c in 'ァ'..'ヶ') c - 0x60 else c)
    }
}

/**
 * 県が検索語に一致するか（表示名・日本語名・ひらがな読み・ローマ字のいずれかに部分一致）。
 * iOS 版 PrefectureSearchView.matches と同じ判定（カタカナはひらがなに寄せ、大文字小文字は無視）。
 */
fun Prefecture.matchesSearch(query: String): Boolean {
    val q = query.trim().katakanaToHiragana().lowercase()
    if (q.isEmpty()) return true
    return listOf(localizedName(), displayName, hiraganaReading, slug).any {
        it.katakanaToHiragana().lowercase().contains(q)
    }
}

/** 検索語で絞り込んだ地方別の県リスト（一致県のある地方だけ）。 */
fun prefecturesByRegion(query: String, excluded: Set<Prefecture> = emptySet()): List<Pair<JapanRegion, List<Prefecture>>> =
    JapanRegion.entries.mapNotNull { region ->
        val matched = region.prefectures.filter { it !in excluded && it.matchesSearch(query) }
        if (matched.isEmpty()) null else region to matched
    }

/** 日本語の表示名（PlanItem.prefectureName 等）から県を引く。 */
fun prefectureFromDisplayName(name: String): Prefecture? =
    Prefecture.entries.firstOrNull { it.displayName == name }
