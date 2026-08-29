package com.example.japantripmap

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

private const val TAG = "AdBanner"

/**
 * ロード済み [AdView] をプロセス内でキャッシュするストア。
 *
 * バナーは `LazyColumn` の item として置くと、スクロールで画面外に大きく離れたとき
 * コンポジションごと破棄され、再入場で作り直し＆再ロードが起きる（＝「消えて再表示」）。
 * これを防ぐため AdView をキャッシュキー単位で保持し、同じキーなら常に同じロード済み
 * インスタンスを返す。これにより広告は保持され、再読み込み・空白・無駄なインプレッションが起きない。
 *
 * キャッシュキーは「広告ユニット ID ＋ サイズ指定」。アダプティブは幅で高さが変わるため幅も含める。
 * View は 1 つの親にしか属せないので、再アタッチ時は [attachTo] で前の親から切り離してから使う。
 */
private object BannerAdCache {
    private val views = HashMap<String, AdView>()

    /**
     * キャッシュから取得。無ければ [create] で作ってロードし、以後使い回す。
     * MobileAds が未初期化（UMP 同意フロー完了前）のうちは AdView.loadAd が例外になるため
     * 生成せず null を返す。初期化後の再コンポーズで改めて生成・キャッシュされる。
     */
    fun getOrCreate(key: String, create: () -> AdView): AdView? {
        views[key]?.let { return it }
        if (!MobileAds.isInitialized) return null
        return create().also { views[key] = it }
    }
}

/** 保持済み AdView を新しい親に付け直す前に、前の親から確実に切り離す（already has a parent 回避）。 */
private fun AdView.detachFromParent(): AdView {
    (parent as? ViewGroup)?.removeView(this)
    return this
}

/**
 * コンテンツ内に埋め込むインライン型アダプティブバナー広告。
 * iOS 版 `AdaptiveBannerAdView`（Banner.swift）に対応する。
 *
 * 固定サイズ(320x50)より表示面積が広く広告在庫が増えるため、AdMob が推奨する形式であり
 * 収益(eCPM)が上がりやすい。指定幅（画面幅 − 左右余白）に収まり、高さはロード後に確定する。
 *
 * スクロールで画面外に出ても [BannerAdCache] で AdView を保持するため、広告は再読み込みされず
 * 戻ると即座に同じ広告が表示される。
 *
 * @param adUnitId 使用する広告ユニット ID。既定はセクション間バナー用。
 *   詳細画面では [AdConfig.detailBannerUnitId] を渡して収益を分けて計測する。
 */
@Composable
fun AdaptiveBannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.bannerUnitId,
    horizontalPadding: Int = 8,
    verticalPadding: Int = 8,
) {
    // スクショ撮影用に広告を枠ごと非表示にする（余白も出さない）。
    if (AdConfig.ADS_HIDDEN) return

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    // 画面幅から左右余白を引いた dp 幅でアダプティブサイズを算出する。
    val adWidthDp = (configuration.screenWidthDp - horizontalPadding * 2).coerceAtLeast(1)

    // MobileAds 未初期化（UMP 同意フロー完了前）のうちは広告を出さない。
    val adView = BannerAdCache.getOrCreate("adaptive:$adUnitId:$adWidthDp") {
        createAdView(context, ViewGroup.LayoutParams.WRAP_CONTENT, adUnitId) {
            AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, adWidthDp)
        }
    } ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding.dp, vertical = verticalPadding.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { adView.detachFromParent() },
        )
    }
}

/**
 * ミディアムレクタングル(300x250)広告。iOS 版 `MediumRectangleAdView` に対応する。
 *
 * バナー(320x50)より大きく eCPM が高い傾向があるが、画面占有が大きい分、誤クリックのリスクも高い。
 * AdMob 公式ガイドに従い、上下に十分な余白を取って周囲のタップ要素と明確に離して配置すること。
 * サイズは固定なので高さ確定は不要。
 *
 * スクロールで画面外に出ても [BannerAdCache] で AdView を保持し、再読み込みされない。
 *
 * @param adUnitId 使用する広告ユニット ID。既定はセクション間バナー用。
 */
@Composable
fun MediumRectangleAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.bannerUnitId,
    verticalPadding: Int = 8,
) {
    // スクショ撮影用に広告を枠ごと非表示にする（余白も出さない）。
    if (AdConfig.ADS_HIDDEN) return

    val context = LocalContext.current

    // MobileAds 未初期化（UMP 同意フロー完了前）のうちは広告を出さない。
    val adView = BannerAdCache.getOrCreate("mrec:$adUnitId") {
        createAdView(context, ViewGroup.LayoutParams.MATCH_PARENT, adUnitId) { AdSize.MEDIUM_RECTANGLE }
    } ?: return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AndroidView(
            // 300x250 固定。中央寄せ。
            modifier = Modifier
                .width(300.dp)
                .height(250.dp),
            factory = { adView.detachFromParent() },
        )
    }
}

/**
 * [AdView] を生成して広告をロードする。ロード完了時に活動中の Activity へ登録して表示する。
 * Activity が取れなければロードしない（表示もされない）。
 *
 * AdView 自体はプロセス内キャッシュに長く残るため、Activity ではなく applicationContext で
 * 生成して Activity リークを避ける。サイズ算出（アダプティブ）は Activity の Context を使う。
 */
private inline fun createAdView(
    context: Context,
    heightLayoutParam: Int,
    adUnitId: String,
    adSize: () -> AdSize,
): AdView = AdView(context.applicationContext).apply {
    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightLayoutParam)
    val activity = context.findActivity() ?: return@apply
    val request = BannerAdRequest.Builder(adUnitId, adSize()).build()
    loadAd(
        request,
        object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                registerBannerAd(ad, activity)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.w(TAG, "バナーの読み込みに失敗: ${adError.message}")
            }
        },
    )
}
