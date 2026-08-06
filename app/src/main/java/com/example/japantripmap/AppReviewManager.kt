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
 * アプリ内レビュー（Play In-App Review）の起動と、起動条件となる「ルーレット完走回数」を管理する。
 *
 * 「3 回ルーレットを回して結果画面から次に進むたび」にレビューを促す。
 * 完走回数は全ルーレット（都道府県／温泉／自然）で合算し、SharedPreferences に永続化する。
 * 3 の倍数（3・6・9…）に達したタイミングで Play のレビューフローを起動する。
 *
 * 注意（Play In-App Review の仕様）:
 * - 実際にダイアログが出るか・どのくらいの頻度で出せるかは Google 側が制御する。
 *   requestReviewFlow / launchReviewFlow を呼んでも表示されないことがあるが、それは正常。
 * - Play ストア経由でインストールされた実機でのみ本番フローが動く。
 *   エミュレータや未署名ビルドでは基本的に何も表示されない（テスト時は Internal Testing 等を使う）。
 */
object AppReviewManager {

    private const val TAG = "AppReviewManager"
    private const val PREFS = "app_review"
    private const val KEY_COMPLETION_COUNT = "roulette_completion_count"

    /** 何回の完走ごとにレビューを促すか。 */
    private const val REVIEW_INTERVAL = 3

    /**
     * ルーレットを 1 回完走した（結果画面から次へ進んだ）ことを記録する。
     * 完走回数が [REVIEW_INTERVAL] の倍数になったら true を返す。
     */
    fun recordCompletionAndShouldReview(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_COMPLETION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_COMPLETION_COUNT, count).apply()
        return count % REVIEW_INTERVAL == 0
    }

    /**
     * ルーレット完走を記録し、条件を満たしていれば Play のレビューフローを起動する。
     * suspend。Activity が取れない／フロー起動に失敗しても例外は投げず握りつぶす（レビューは付随的な体験のため）。
     */
    suspend fun onRouletteCompleted(context: Context) {
        if (!recordCompletionAndShouldReview(context)) return
        launchReviewFlow(context)
    }

    private suspend fun launchReviewFlow(context: Context) {
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
