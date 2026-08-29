package com.example.japantripmap

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * UMP（User Messaging Platform）による GDPR 同意フローを管理する。
 * iOS 版 `ConsentManager.swift` に対応する。
 *
 * ## フロー（Google 推奨の順序）
 * 1. アプリ起動時に [gatherConsentThen] を呼ぶ。
 * 2. `requestConsentInfoUpdate` で最新の同意情報を取得する。
 * 3. 必要な地域（EEA・英国・スイスなど）でだけ `loadAndShowConsentFormIfRequired` が
 *    同意フォームを表示する。対象外の地域では何も表示せず即座に完了する。
 * 4. フォーム完了後（または更新失敗後）、`canRequestAds()` が true であれば
 *    [onCanRequestAds] を一度だけ呼ぶ。ここで [MobileAds] を初期化して広告を出す。
 *
 * これにより、EEA 等では「同意を取ってから広告」、それ以外では従来どおり即広告、が実現できる。
 * プライバシーポリシーの「§4 広告に関する同意」の記述とも一致する。
 *
 * 注意:
 * - 実際にフォームが出るか・どの地域で出るかは AdMob 管理画面の同意メッセージ設定と
 *   UMP の所在地判定に基づく。テスト時は [ConsentDebugSettings] で地域を強制できる（本実装では未使用）。
 * - `canRequestAds()` は複数のタイミングで true になり得るため、[onCanRequestAds] の
 *   多重呼び出しを防ぐガードを設けている。
 */
object ConsentManager {

    private const val TAG = "ConsentManager"

    /** 広告初期化コールバックを一度しか呼ばないためのガード。 */
    private var adsRequested = false

    /**
     * 同意情報を更新し、必要なら同意フォームを表示する。完了後、広告リクエストが許可されていれば
     * [onCanRequestAds] を一度だけ呼ぶ。
     *
     * @param activity フォーム表示に使う Activity。
     * @param onCanRequestAds 広告を出してよい状態になったときに呼ばれる（MobileAds 初期化はここで行う）。
     */
    fun gatherConsentThen(activity: Activity, onCanRequestAds: () -> Unit) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        // 本番はデフォルト設定でよい（対象地域の判定は UMP に任せる）。
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // 更新成功。必要な地域なら同意フォームを表示し、そうでなければ即 dismiss される。
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "同意フォームでエラー: ${formError.errorCode} ${formError.message}")
                    }
                    // 同意が済んだ（または不要だった）ので、許可されていれば広告へ進む。
                    maybeRequestAds(consentInformation, onCanRequestAds)
                }
            },
            { requestError ->
                // 更新失敗。広告なしにするとユーザー体験を損なうため、直近のキャッシュ状態で
                // 許可されていれば広告に進む（Google 公式サンプルと同じ考え方）。
                Log.w(TAG, "同意情報の更新に失敗: ${requestError.errorCode} ${requestError.message}")
                maybeRequestAds(consentInformation, onCanRequestAds)
            },
        )
    }

    /** 広告リクエストが許可されていて、まだ広告初期化していなければ [onCanRequestAds] を一度だけ呼ぶ。 */
    private fun maybeRequestAds(
        consentInformation: ConsentInformation,
        onCanRequestAds: () -> Unit,
    ) {
        if (adsRequested) return
        if (consentInformation.canRequestAds()) {
            adsRequested = true
            onCanRequestAds()
        }
    }
}
