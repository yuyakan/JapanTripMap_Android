package com.example.japantripmap

import android.content.Intent

/**
 * Play ストアのスクリーンショット撮影専用モード（デバッグ版のみ）。iOS 版 ScreenshotMode。
 *
 * 【なぜ要るか】
 * トレンド機能は他人の YouTube 動画のサムネ・タイトル・チャンネル名を表示する。アプリ内での表示は
 * 規約上問題ない（公式埋め込み・出典表記・30日ルール）が、ストアの販促素材に第三者のコンテンツを
 * 載せるのは別問題（サムネは CC ライセンスではなく、YouTube のブランド利用も販促素材では制約が別）。
 * そこで撮影のあいだだけ、サムネを自前のモック画像に、タイトル・チャンネル名をダミーに差し替える。
 *
 * 【安全側の設計】
 * - このファイルとモック画像（res/drawable-nodpi/mock_*）は src/debug にだけ置いてある。
 *   リリース版は src/release の空実装（常に無効）でビルドされるので、有効なまま提出用 APK/AAB を
 *   作ることは構造的に起きない（リリース版にはモック画像もダミー文言も入らない）。
 * - 既定は無効。起動インテントのフラグでしか有効にならない（UI もビルド設定も介さない）。
 *
 * 【使い方】
 *   adb shell am start -n com.kanbe1365.japantripmap/com.example.japantripmap.MainActivity --ez screenshot_mode true
 * 撮影が終わったらアプリを終了して通常どおり起動し直すだけ（コードは元に戻さなくてよい）。
 * 広告も消したい場合は AdConfig.ADS_HIDDEN を一時的に true にする（iOS の adsHidden と同じく別スイッチ）。
 *
 * モックは CC0 / パブリックドメインの同梱写真だけから作った iOS と同じ画像
 * （MapRoulette/tools/mock_thumbnails/build_from_bundled.py）。人物・ロゴ・文字を含まない。
 */
object ScreenshotMode {

    /** 撮影モードが有効か。 */
    var isEnabled: Boolean = false
        private set

    /** 起動インテントからフラグを読む（MainActivity の onCreate で呼ぶ）。 */
    fun init(intent: Intent?) {
        isEnabled = intent?.getBooleanExtra(EXTRA, false) == true
    }

    private const val EXTRA = "screenshot_mode"

    /** 棚（カテゴリ）ごとのモック画像。「沖縄県のグルメ」に花火が並ぶといった不一致を避ける。 */
    private val mockByShelf: Map<String, List<Int>> = mapOf(
        "gourmet" to listOf(
            R.drawable.mock_gourmet_1, R.drawable.mock_gourmet_2, R.drawable.mock_gourmet_3, R.drawable.mock_gourmet_4,
            R.drawable.mock_gourmet_5, R.drawable.mock_gourmet_6, R.drawable.mock_gourmet_7, R.drawable.mock_gourmet_8,
        ),
        "spot" to listOf(
            R.drawable.mock_spot_1, R.drawable.mock_spot_2, R.drawable.mock_spot_3, R.drawable.mock_spot_4,
            R.drawable.mock_spot_5, R.drawable.mock_spot_6, R.drawable.mock_spot_7, R.drawable.mock_spot_8,
        ),
        "cafe" to listOf(
            R.drawable.mock_cafe_1, R.drawable.mock_cafe_2, R.drawable.mock_cafe_3,
            R.drawable.mock_cafe_4, R.drawable.mock_cafe_5, R.drawable.mock_cafe_6,
        ),
    )

    /** 棚が特定できないとき（Shorts 全画面など）の全体の母集団。 */
    private val allMocks: List<Int> = mockByShelf.keys.sorted().flatMap { mockByShelf.getValue(it) }

    /**
     * サムネの代わりに出すモック画像の drawable ID。無効時は 0。
     * position（棚の中の並び順）があれば先頭から順に配る（母集団を使い切るまで同じ絵が隣り合わない）。
     * 無ければ URL の安定ハッシュで選ぶ（同じ動画には常に同じ絵）。
     */
    fun mockThumbnail(url: String, shelf: String? = null, position: Int? = null): Int {
        if (!isEnabled) return 0
        val pool = shelf?.let { mockByShelf[it] } ?: allMocks
        val index = position?.let { ((it % pool.size) + pool.size) % pool.size } ?: stableIndex(url, pool.size)
        return pool[index]
    }

    private val mockTitles = listOf(
        "絶景スポットめぐり",
        "旅先のおすすめグルメ",
        "週末の小さな旅",
        "この街のいちばん好きな場所",
        "朝いちばんの景色",
        "夜まで楽しむ町歩き",
        "名湯でととのう休日",
        "季節の風景をたずねて",
        "地元で人気の一皿",
    )

    /** 実在のチャンネルを想起させない一般的な名前だけ。 */
    private val mockChannels = listOf(
        "旅のしおり",
        "ぶらり日本",
        "週末トラベル",
        "にっぽん散歩",
        "旅と温泉",
        "季節めぐり",
    )

    /** 画面に出すタイトル（撮影モードでは動画 ID で決まるダミー）。 */
    fun title(video: TrendVideo): String =
        if (isEnabled) mockTitles[stableIndex(video.videoId, mockTitles.size)] else video.title

    /** 画面に出すチャンネル名（撮影モードではダミー）。 */
    fun channel(video: TrendVideo): String =
        if (isEnabled) mockChannels[stableIndex(video.videoId, mockChannels.size)] else video.channelTitle

    /** djb2 ベースの安定ハッシュ（iOS と同じ式。実行をまたいでも同じ値）。 */
    private fun stableIndex(key: String, count: Int): Int {
        var hash = 5381UL
        for (b in key.toByteArray(Charsets.UTF_8)) {
            hash = hash * 33UL + b.toUByte().toULong()
        }
        return (hash % count.toULong()).toInt()
    }
}
