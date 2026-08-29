package com.example.japantripmap

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.google.android.play.core.review.ReviewException
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.model.ReviewErrorCode
import com.google.android.play.core.review.testing.FakeReviewManager
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview

/**
 * アプリ内レビュー（Play In-App Review）フローの起動だけを担う薄いラッパー。
 *
 * 「いつレビューを促すか」の発火条件は [InterstitialAdManager] 側が持つ（iOS 版
 * `InterstitialViewModel.requestSystemReview` と同じく、広告の発火ロジックに集約する）。
 * ここは iOS の `AppStore.requestReview(in:)` 相当で、呼ばれたら Play にフロー起動を依頼するだけ。
 *
 * 注意（Play In-App Review の仕様）:
 * - 実際にダイアログが出るか・どのくらいの頻度で出せるかは Google 側が制御する。
 *   requestReviewFlow / launchReviewFlow を呼んでも表示されないことがあるが、それは正常
 *   （iOS の SKStoreReviewController と同じく OS 側が頻度を間引く前提で呼んでよい）。
 * - Play ストア経由でインストールされた実機でのみ本番フローが動く。
 *   エミュレータや未署名ビルドでは基本的に何も表示されない（テスト時は Internal Testing 等を使う）。
 */
object AppReviewManager {

    private const val TAG = "AppReviewManager"

    /**
     * Play のレビューフローを起動する。
     * suspend。Activity が取れない／フロー起動に失敗しても例外は投げず握りつぶす（レビューは付随的な体験のため）。
     */
    suspend fun launchReviewFlow(context: Context) {
        val activity = context.findActivity()
        if (activity == null) {
            Log.w(TAG, "Activity が取得できないためレビューフローを起動できません")
            return
        }
        // デバッグビルドでは FakeReviewManager を使い、フロー起動までを検証できるようにする。
        val manager = if (BuildConfig.DEBUG) {
            FakeReviewManager(context)
        } else {
            ReviewManagerFactory.create(context)
        }
        try {
            // review-ktx の suspend 拡張。内部で Play Services Task を await する。
            val reviewInfo = manager.requestReview()
            manager.launchReview(activity, reviewInfo)
        } catch (e: ReviewException) {
            @ReviewErrorCode val code = e.errorCode
            Log.w(TAG, "レビューフローの起動に失敗しました (errorCode=$code)", e)
        } catch (e: Exception) {
            Log.w(TAG, "レビューフローの起動に失敗しました", e)
        }
    }
}

/** Compose の [Context] から所属する [Activity] を辿る。見つからなければ null。 */
fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
