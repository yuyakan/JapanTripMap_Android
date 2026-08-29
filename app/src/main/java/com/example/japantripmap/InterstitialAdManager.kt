package com.example.japantripmap

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * インタースティシャル広告とアプリ内レビュー誘導の発火タイミングを管理する。
 * iOS 版 `InterstitialViewModel`（Interstitial.swift）をそのまま移植したもの。
 *
 * ## 発火の考え方（iOS 版と同一）
 * - ポイント制の [count] を貯め、[THRESHOLD] 以上で「広告（初回だけレビュー）」を発火する。
 * - ルーレットを回すと +5、詳細画面を開く／戻ると +2。
 * - 直前に広告を出したフレームでは二重発火を避けるため加算をスキップする。
 * - [COOLDOWN_MS] 未満の連続発火はしない（count は維持して次の機会に持ち越す）。
 * - 初回の発火だけは広告の代わりに Play のレビューフローを試行する。
 * - さらに、累計スピンが [REVIEW_SPIN_THRESHOLD] 以上になったら、広告が出ないフレームで
 *   毎スピン Play レビューを試行する（実際に出すかは OS 任せ／間引かれる前提）。
 *
 * ## Android 固有の注意
 * - 状態（count / lastAdShownAt / hasShownReview / reviewSpinCount）はプロセス生存中は
 *   メモリで持ち、永続化が要るもの（初回レビュー済みフラグ・累計スピン）は SharedPreferences に保存する。
 *   iOS は count/lastAdShownAt を static（＝アプリ起動中のみ）で持つのでそれに合わせる。
 * - シングルトン（object）にして、全ルーレット・全詳細画面から同じカウンタを共有する。
 */
object InterstitialAdManager {

    private const val TAG = "InterstitialAd"

    // MARK: - 表示制御の設定（iOS と同値）

    /** 発火しきい値。count がこの値以上になると広告（または初回レビュー）を出す。 */
    private const val THRESHOLD = 10
    /** 広告のクールダウン。前回広告からこの時間未満は count がしきい値を超えても発火しない。 */
    private const val COOLDOWN_MS = 90_000L
    /** ルーレット 1 スピンあたりの加算ポイント。 */
    private const val SPIN_POINTS = 5
    /** 詳細画面の開閉あたりの加算ポイント。 */
    const val DETAIL_POINTS = 2

    /**
     * 累計スピン回数がこの値以上になると、Play レビューの表示を毎スピン試行する。
     * 実際に表示されるかは OS 任せ（Play が頻度を間引くため呼びすぎの害はない）。
     */
    private const val REVIEW_SPIN_THRESHOLD = 7

    private const val PREFS = "interstitial_ad"
    private const val KEY_REVIEW_SHOWN = "reviewShown"
    private const val KEY_REVIEW_SPIN_COUNT = "reviewSpinCount"

    // MARK: - 実行時状態（アプリ起動中のみ・iOS の static 相当）

    /** 貯まっているポイント。 */
    private var count = 0
    /** 直前のフレームで広告／レビューを発火したか。次のスピンで二重加算を避けるフラグ。 */
    private var isShowAd = false
    /** 前回広告を表示した時刻（[SystemClock.elapsedRealtime]）。クールダウン判定に使う。null なら未表示。 */
    private var lastAdShownAt: Long? = null

    /** 先読み済みのインタースティシャル広告。表示後は null に戻す。 */
    private var interstitialAd: InterstitialAd? = null
    /** ロード中の多重リクエストを防ぐ。 */
    private var isLoading = false

    /** 広告の読み込み・表示・レビュー起動に使う軽量スコープ。 */
    private val scope: CoroutineScope = MainScope()

    // MARK: - 永続化する状態

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 初回発火でレビューを出したか（永続化）。 */
    private fun hasShownReview(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REVIEW_SHOWN, false)

    private fun setReviewShown(context: Context) {
        prefs(context).edit().putBoolean(KEY_REVIEW_SHOWN, true).apply()
    }

    /** 累計スピン回数（永続化）。 */
    private fun reviewSpinCount(context: Context): Int =
        prefs(context).getInt(KEY_REVIEW_SPIN_COUNT, 0)

    private fun incrementReviewSpinCount(context: Context): Int {
        val next = reviewSpinCount(context) + 1
        prefs(context).edit().putInt(KEY_REVIEW_SPIN_COUNT, next).apply()
        return next
    }

    // MARK: - 外部から呼ぶ発火 API

    /**
     * ルーレットを回した（結果画面から次へ進んだ）ときに呼ぶ。iOS `registerRouletteSpin` 相当。
     * 直前に広告を出していれば二重発火を避けて加算をスキップし、そうでなければ +5 して発火判定を行う。
     */
    fun registerRouletteSpin(context: Context) {
        if (isShowAd) {
            isShowAd = false
        } else {
            count += SPIN_POINTS
        }

        // 累計スピン数を加算。Play レビューのしきい値判定に使う。
        val spins = incrementReviewSpinCount(context)

        // 自作の発火（広告／初回レビュー）が起きたフレームでは Play レビューを重ねない。
        if (maybePresent(context)) return

        // 何も発火していないフレームでのみ、累計スピンがしきい値以上なら Play レビューを試行する。
        if (spins >= REVIEW_SPIN_THRESHOLD) {
            requestSystemReview(context)
        }
    }

    /**
     * ルーレット画面が前面に来た（onResume/onAppear）ときに呼ぶ。iOS `handleMapAppear` 相当。
     * 発火したら次のスピンで二重発火しないよう [isShowAd] を立て、あわせて次の広告を先読みする。
     */
    fun handleScreenAppear(context: Context) {
        if (maybePresent(context)) {
            isShowAd = true
        }
        loadAd(context)
    }

    /** 詳細画面を開く／戻るときに呼ぶ。iOS の `InterstitialViewModel.count += 2` 相当。 */
    fun addDetailPoints() {
        count += DETAIL_POINTS
    }

    /**
     * しきい値・クールダウンを満たしていれば広告（初回はレビュー）を出し、count をリセットする。
     * 実際に発火したら true。iOS `maybePresent` 相当。
     */
    private fun maybePresent(context: Context): Boolean {
        // スクショ撮影用に広告・レビュー誘導を一切発火させない。
        if (AdConfig.ADS_HIDDEN) return false

        if (count < THRESHOLD) return false

        // クールダウン中は発火しない（count は維持して次の機会に持ち越す）。
        val last = lastAdShownAt
        if (last != null && SystemClock.elapsedRealtime() - last < COOLDOWN_MS) {
            return false
        }

        count = 0
        lastAdShownAt = SystemClock.elapsedRealtime()

        // 初回の発火だけは広告の代わりに Play レビュー（アプリ内で完結）を試行する。
        if (!hasShownReview(context)) {
            setReviewShown(context)
            requestSystemReview(context)
        } else {
            showAd(context)
        }
        return true
    }

    // MARK: - レビュー誘導

    /** Play のレビューフローを試行する。実際に表示されるかは OS 任せ。iOS `requestSystemReview` 相当。 */
    private fun requestSystemReview(context: Context) {
        val appContext = context.applicationContext
        scope.launch { AppReviewManager.launchReviewFlow(appContext) }
    }

    // MARK: - 広告のロード／表示

    /** 次のインタースティシャル広告を先読みする。iOS `loadAd` 相当。 */
    fun loadAd(context: Context) {
        if (AdConfig.ADS_HIDDEN) return
        // UMP の同意フロー完了前（MobileAds 未初期化）に呼ばれると
        // 「MobileAds.initialize must be called before ...」で落ちるため、ここでガードする。
        // 初期化完了後に MainActivity から改めて loadAd されるので取りこぼしはない。
        if (!MobileAds.isInitialized) return
        if (interstitialAd != null || isLoading) return
        isLoading = true
        val appContext = context.applicationContext
        InterstitialAd.load(
            AdRequest.Builder(AdConfig.interstitialUnitId).build(),
            object : AdLoadCallback<InterstitialAd> {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    interstitialAd = ad
                    ad.adEventCallback = object : InterstitialAdEventCallback {
                        override fun onAdDismissedFullScreenContent() {
                            // 表示が終わったら破棄し、次を先読みする。
                            interstitialAd = null
                            loadAd(appContext)
                        }

                        override fun onAdFailedToShowFullScreenContent(
                            fullScreenContentError: FullScreenContentError
                        ) {
                            interstitialAd = null
                            loadAd(appContext)
                        }
                    }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                    Log.w(TAG, "インタースティシャルの読み込みに失敗: ${adError.message}")
                }
            },
        )
    }

    /** 先読み済み広告があれば表示する。iOS `showAd` 相当。 */
    private fun showAd(context: Context) {
        val ad = interstitialAd
        val activity = context.findActivity()
        if (ad == null || activity == null) {
            Log.d(TAG, "広告が未準備のため表示をスキップ")
            // 次の機会に出せるよう先読みしておく。
            loadAd(context)
            return
        }
        ad.show(activity)
    }
}
