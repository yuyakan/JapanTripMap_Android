package com.example.japantripmap

/**
 * 広告まわりの設定。iOS 版 `secretConfig.swift` に対応する。
 *
 * 広告ユニット ID は「デバッグビルドは Google 公式のテスト ID／リリースビルドは本番 ID」を
 * 出し分ける（[BuildConfig.DEBUG] で判定）。これにより開発中の自己クリックによる
 * アカウント警告を避けつつ、本番でのみ収益化する。
 *
 * アプリ ID（App ID）は AndroidManifest 側で設定する（build.gradle.kts の
 * manifestPlaceholders["ADMOB_APP_ID"]）。ここで持つのはあくまでユニット ID。
 */
object AdConfig {

    /**
     * スクリーンショット撮影用の一時スイッチ。true にするとバナー（枠・余白ごと）と
     * インタースティシャル広告を完全に非表示にする。ストア用スクショ撮影後は必ず false に戻すこと。
     */
    const val ADS_HIDDEN = false

    // --- インタースティシャル ---
    private const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    private const val PROD_INTERSTITIAL = "ca-app-pub-3155724310732667/7536101398"

    // --- バナー（セクション間） ---
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/9214589741"
    private const val PROD_BANNER = "ca-app-pub-3155724310732667/4762598394"

    // --- 詳細カード画面（観光地・グルメ・お土産・温泉・祭り）専用のバナー ---
    // iOS 版と同じく、セクション間バナーと ID を分けることで画面別に eCPM・収益を計測できる。
    // 現状、画面に配置しているバナーはすべてこの detailBanner 枠を使用する。
    private const val TEST_DETAIL_BANNER = "ca-app-pub-3940256099942544/9214589741"
    private const val PROD_DETAIL_BANNER = "ca-app-pub-3155724310732667/6434772710"

    /** 実際に使うインタースティシャル広告ユニット ID。 */
    val interstitialUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL else PROD_INTERSTITIAL

    /** 実際に使うバナー（セクション間）広告ユニット ID。 */
    val bannerUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER else PROD_BANNER

    /** 実際に使う詳細画面バナー広告ユニット ID。 */
    val detailBannerUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_DETAIL_BANNER else PROD_DETAIL_BANNER
}
