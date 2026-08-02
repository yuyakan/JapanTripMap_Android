package com.example.japantripmap

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** ルーレットの表示モード。iOS 版 DisplayMode に対応。 */
enum class RouletteMode {
    /** 全国モード（全 47 都道府県）。 */
    TOURISM,
    /** 温泉モード（温泉がある県のみ）。 */
    ONSEN,
    /** 自然モード（自然スポットがある県のみ）。 */
    NATURE,
}

/**
 * 都道府県ルーレットの状態とアニメーションを管理する ViewModel。
 *
 * iOS 版 JapanMapView.swift のスピン／停止シーケンスを Kotlin coroutines で再現している。
 * - Start: 一定間隔で選択県をランダムに切り替え続ける
 * - Stop:  22 ステップかけて徐々に間隔を伸ばし、最後に停止して結果を確定する
 *
 * @param mode 全国モードか温泉モードか。温泉モードでは温泉がある県のみを対象にする。
 */
class RouletteViewModel(val mode: RouletteMode = RouletteMode.TOURISM) : ViewModel() {

    /** このモードで選択肢になりうる県の母集団。 */
    val availablePrefectures: List<Prefecture> = when (mode) {
        RouletteMode.TOURISM -> Prefecture.entries.toList()
        RouletteMode.ONSEN -> PREFECTURES_WITH_ONSEN
        RouletteMode.NATURE -> PREFECTURES_WITH_NATURE
    }

    /** このモードで選べるタイプ（温泉7種・自然4種）。全国モードは空＝タイプフィルタ無し。 */
    val availableTypes: List<String> = when (mode) {
        RouletteMode.ONSEN -> listOf("scenic", "historical", "therapeutic", "resort", "mountain", "seaside", "ski")
        RouletteMode.NATURE -> listOf("night_view", "starry_sky", "sea", "camping")
        RouletteMode.TOURISM -> emptyList()
    }

    /** タイプのラベル（UI 表示用）。 */
    val typeLabels: Map<String, String> = when (mode) {
        RouletteMode.ONSEN -> mapOf(
            "scenic" to "絶景", "historical" to "歴史", "therapeutic" to "療養",
            "resort" to "リゾート", "mountain" to "山あい", "seaside" to "海辺", "ski" to "スキー",
        )
        RouletteMode.NATURE -> mapOf(
            "night_view" to "夜景", "starry_sky" to "星空", "sea" to "海", "camping" to "キャンプ",
        )
        RouletteMode.TOURISM -> emptyMap()
    }

    /** 選択中のタイプ（空なら全タイプ対象）。 */
    var selectedTypes by mutableStateOf<Set<String>>(emptySet())
        private set

    /** 指定県がこのモードで持つタイプ集合。 */
    private fun typesOf(prefecture: Prefecture): Set<String> = when (mode) {
        RouletteMode.ONSEN -> prefecture.onsenTypes
        RouletteMode.NATURE -> prefecture.natureTypes
        RouletteMode.TOURISM -> emptySet()
    }

    /** タイプフィルタを反映した実効対象県（enabled かつ 選択タイプに合致）。 */
    val effectiveEnabled: Set<Prefecture>
        get() = if (selectedTypes.isEmpty()) {
            enabledPrefectures
        } else {
            enabledPrefectures.filter { typesOf(it).any { t -> t in selectedTypes } }.toSet()
        }

    /** タイプの ON/OFF を切り替える。 */
    fun toggleType(type: String) {
        selectedTypes = if (type in selectedTypes) selectedTypes - type else selectedTypes + type
    }

    /** 都道府県ごとの重み。Compose が再描画できるよう observable な Map で保持する。初期値は全県 1.0。 */
    val weights: SnapshotStateMap<Prefecture, Double> = mutableStateMapOf<Prefecture, Double>().apply {
        Prefecture.entries.forEach { this[it] = WeightManager.DEFAULT_WEIGHT }
    }

    /** 現在ハイライトされている（＝抽選で当たっている）都道府県。 */
    var selectedPrefecture by mutableStateOf<Prefecture?>(null)
        private set

    /** スピン中（Start 後 Stop 完了まで）。 */
    var isSpinning by mutableStateOf(false)
        private set

    /** 停止シーケンス中（Stop を押してから減速して止まるまで）。 */
    var isStopping by mutableStateOf(false)
        private set

    /** 結果モーダルを表示中。 */
    var showResultModal by mutableStateOf(false)
        private set

    /** ルーレット対象として有効な都道府県。初期はこのモードの母集団すべて。 */
    var enabledPrefectures by mutableStateOf(availablePrefectures.toSet())
        private set

    private var spinJob: Job? = null

    /** 重み付けがカスタマイズされているか（UI 表示用）。 */
    val hasCustomWeights: Boolean get() = weights.values.any { it != WeightManager.DEFAULT_WEIGHT }

    /** ルーレットを開始できる（実効対象が 2 県以上）か。 */
    val canSpin: Boolean get() = effectiveEnabled.size >= 2

    // ---- 設定画面向け API ----

    /** 指定県の ON/OFF を切り替える。 */
    fun togglePrefecture(prefecture: Prefecture) {
        enabledPrefectures = if (prefecture in enabledPrefectures) {
            enabledPrefectures - prefecture
        } else {
            enabledPrefectures + prefecture
        }
    }

    /** このモードの全都道府県を対象にする。 */
    fun selectAllPrefectures() {
        enabledPrefectures = availablePrefectures.toSet()
    }

    /** すべての都道府県を対象外にする。 */
    fun deselectAllPrefectures() {
        enabledPrefectures = emptySet()
    }

    /** 指定県の重みを更新する。 */
    fun setWeight(prefecture: Prefecture, weight: Double) {
        weights[prefecture] = weight
    }

    /** すべての重みを初期値（1.0）に戻す。 */
    fun resetWeights() {
        Prefecture.entries.forEach { weights[it] = WeightManager.DEFAULT_WEIGHT }
    }

    /** 指定県が選ばれる確率（%）。 */
    fun probabilityOf(prefecture: Prefecture): Double =
        WeightManager.getProbability(prefecture, enabledPrefectures, weights)

    // ---- ルーレット制御 ----

    /** スピン開始。高速で選択県を切り替え続ける。 */
    fun startSpin() {
        if (isSpinning || !canSpin) return

        isSpinning = true
        isStopping = false
        showResultModal = false

        spinJob = viewModelScope.launch {
            while (isSpinning && !isStopping) {
                selectedPrefecture = WeightManager.selectRandomPrefecture(effectiveEnabled, weights)
                delay(SPIN_INTERVAL_MS)
            }
        }
    }

    /** 停止要求。ここから減速シーケンスに入る。 */
    fun stopSpin() {
        if (!isSpinning || isStopping) return

        isStopping = true
        spinJob?.cancel()

        spinJob = viewModelScope.launch {
            var interval = SPIN_INTERVAL_MS
            var count = 0

            // iOS 版と同じ 22 ステップの減速カーブ。
            while (count < STOP_STEPS) {
                selectedPrefecture = WeightManager.selectRandomPrefecture(effectiveEnabled, weights)
                delay(interval)

                count++
                interval += stopIntervalIncrement(count)
            }

            // 最終結果を確定。
            selectedPrefecture = WeightManager.selectRandomPrefecture(effectiveEnabled, weights)
            isSpinning = false

            delay(RESULT_DELAY_MS)
            showResultModal = true
        }
    }

    /** 結果モーダルを閉じて次のルーレットに備える。 */
    fun reset() {
        showResultModal = false
        selectedPrefecture = null
        isStopping = false
    }

    /** 停止時、ステップ数に応じて間隔をどれだけ伸ばすか（iOS 版と同じ）。 */
    private fun stopIntervalIncrement(count: Int): Long = when {
        count > 20 -> 500L
        count > 17 -> 200L
        count > 15 -> 100L
        count > 12 -> 75L
        count > 8 -> 50L
        count > 5 -> 20L
        else -> 0L
    }

    override fun onCleared() {
        super.onCleared()
        spinJob?.cancel()
    }

    companion object {
        /** スピン中の切り替え間隔（ms）。iOS 版の 0.05 秒に合わせる。 */
        private const val SPIN_INTERVAL_MS = 50L

        /** 減速して停止するまでのステップ数。 */
        private const val STOP_STEPS = 22

        /** 停止後、結果モーダルを出すまでの待ち（ms）。 */
        private const val RESULT_DELAY_MS = 500L

        /** モード指定で ViewModel を生成する Factory。 */
        fun factory(mode: RouletteMode): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    RouletteViewModel(mode) as T
            }
    }
}
