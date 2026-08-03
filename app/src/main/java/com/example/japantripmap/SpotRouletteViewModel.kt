package com.example.japantripmap

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 温泉／自然の「スポット単位ルーレット」の状態・設定・アニメーションを管理する ViewModel。
 *
 * iOS 版 OnsenMapView / NatureSpotMapView の @State 群と OnsenWeightManager /
 * NatureSpotWeightManager を Android にまとめたもの。
 * - 有効スポット集合・重み・（自然のみ）表示タイプフィルタを保持
 * - スピン／停止シーケンスは iOS と同じ 22 ステップの減速カーブ
 * - 設定内容は SharedPreferences に永続化する（iOS はメモリのみだが端末保存にする）
 *
 * @param kind 温泉か自然か。
 */
class SpotRouletteViewModel(app: Application, val kind: SpotKind) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("spot_roulette_${kind.name.lowercase()}", 0)

    /** この分類の全スポット。 */
    val allSpots: List<RouletteSpot> = SpotRepository.all(kind)

    private val spotsById: Map<String, RouletteSpot> = allSpots.associateBy { it.id }

    /** この分類で選べるタイプ一覧。 */
    val allTypes: List<SpotTypeMeta> = kind.allTypes()

    // ---- 設定状態 ----

    /** 有効なスポット（ルーレット対象）。初期は全スポット。 */
    var enabledSpots by mutableStateOf<Set<RouletteSpot>>(emptySet())
        private set

    /** スポットごとの重み。observable Map。初期値は全スポット 1.0。 */
    val weights: SnapshotStateMap<String, Double> = mutableStateMapOf()

    /**
     * 自然タブの「表示アイコン（タイプ）」フィルタ。iOS の selectedDisplayTypes に対応。
     * 温泉タブでは常に全タイプ（未使用）。
     */
    var selectedDisplayTypes by mutableStateOf<Set<String>>(emptySet())
        private set

    // ---- ルーレット状態 ----

    /** 現在ハイライトされている（＝抽選で当たっている）スポット。 */
    var selectedSpot by mutableStateOf<RouletteSpot?>(null)
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

    /** 地図表示（false）かリスト表示（true）か。iOS の isListView に対応。 */
    var isListView by mutableStateOf(false)

    private var spinJob: Job? = null

    init {
        load()
    }

    // ---- 派生状態 ----

    /** 表示タイプフィルタを反映した「地図・リストに出すスポット」。温泉は全有効スポット。 */
    val visibleEnabledSpots: List<RouletteSpot>
        get() = when (kind) {
            SpotKind.ONSEN -> enabledSpots.toList()
            SpotKind.NATURE -> enabledSpots.filter { it.typeKey in selectedDisplayTypes }
        }

    /** 重み付けがカスタマイズされているか（UI 表示用）。 */
    val hasCustomWeights: Boolean
        get() = weights.values.any { it != WeightManager.DEFAULT_WEIGHT }

    /** ルーレットを開始できる（有効スポットが 2 件以上）か。 */
    val canSpin: Boolean get() = enabledSpots.size >= 2

    // ---- 設定 API ----

    fun toggleSpot(spot: RouletteSpot) {
        enabledSpots = if (spot in enabledSpots) enabledSpots - spot else enabledSpots + spot
        persist()
    }

    /** このスポット集合をまとめて有効化。 */
    fun enableSpots(spots: Collection<RouletteSpot>) {
        enabledSpots = enabledSpots + spots
        persist()
    }

    /** このスポット集合をまとめて無効化。 */
    fun disableSpots(spots: Collection<RouletteSpot>) {
        enabledSpots = enabledSpots - spots.toSet()
        persist()
    }

    /** 全スポットを対象にする。 */
    fun selectAll() {
        enabledSpots = allSpots.toSet()
        persist()
    }

    /** すべて対象外にする。 */
    fun deselectAll() {
        enabledSpots = emptySet()
        persist()
    }

    /** 自然タブ: 表示タイプの ON/OFF を切り替える。ついでにそのタイプのスポットも有効/無効化（iOS 準拠）。 */
    fun toggleDisplayType(typeKey: String) {
        val spotsForType = allSpots.filter { it.typeKey == typeKey }
        if (typeKey in selectedDisplayTypes) {
            selectedDisplayTypes = selectedDisplayTypes - typeKey
            enabledSpots = enabledSpots - spotsForType.toSet()
        } else {
            selectedDisplayTypes = selectedDisplayTypes + typeKey
            enabledSpots = enabledSpots + spotsForType
        }
        persist()
    }

    fun setWeight(spot: RouletteSpot, weight: Double) {
        weights[spot.id] = weight
        persist()
    }

    /** クイック調整（±0.5, 範囲 0.1〜10.0）。iOS の +/- ボタン相当。 */
    fun adjustWeight(spot: RouletteSpot, delta: Double) {
        val current = weights[spot.id] ?: WeightManager.DEFAULT_WEIGHT
        weights[spot.id] = (current + delta).coerceIn(WeightManager.MIN_WEIGHT, WeightManager.MAX_WEIGHT)
        persist()
    }

    fun weightOf(spot: RouletteSpot): Double = weights[spot.id] ?: WeightManager.DEFAULT_WEIGHT

    /** すべての重みを初期値（1.0）に戻す。 */
    fun resetWeights() {
        weights.clear()
        persist()
    }

    /** 指定スポットが選ばれる確率（%）。iOS の重みスライダー行の確率表示に対応。 */
    fun probabilityOf(spot: RouletteSpot): Double {
        if (spot !in enabledSpots) return 0.0
        val total = enabledSpots.sumOf { weightOf(it) }
        return if (total > 0.0) weightOf(spot) / total * 100.0 else 0.0
    }

    // ---- ルーレット制御（iOS と同じ 22 ステップ減速） ----

    fun startSpin() {
        if (isSpinning || !canSpin) return
        isSpinning = true
        isStopping = false
        showResultModal = false

        spinJob = viewModelScope.launch {
            while (isSpinning && !isStopping) {
                selectedSpot = pickRandom()
                delay(SPIN_INTERVAL_MS)
            }
        }
    }

    fun stopSpin() {
        if (!isSpinning || isStopping) return
        isStopping = true
        spinJob?.cancel()

        spinJob = viewModelScope.launch {
            var interval = SPIN_INTERVAL_MS
            var count = 0
            while (count < STOP_STEPS) {
                selectedSpot = pickRandom()
                delay(interval)
                count++
                interval += stopIntervalIncrement(count)
            }
            selectedSpot = pickRandom()
            isSpinning = false
            delay(RESULT_DELAY_MS)
            showResultModal = true
        }
    }

    /** 結果モーダルを閉じて次のルーレットに備える。 */
    fun reset() {
        showResultModal = false
        selectedSpot = null
        isStopping = false
    }

    /** 重みに比例した抽選（0 の重みは除外）。iOS の selectRandomOnsen / selectRandomSpot 相当。 */
    private fun pickRandom(): RouletteSpot? {
        val valid = enabledSpots.filter { weightOf(it) > 0.0 }
        if (valid.isEmpty()) return null
        val total = valid.sumOf { weightOf(it) }
        val r = Math.random() * total
        var current = 0.0
        for (spot in valid) {
            current += weightOf(spot)
            if (r < current) return spot
        }
        return valid.last()
    }

    private fun stopIntervalIncrement(count: Int): Long = when {
        count > 20 -> 500L
        count > 17 -> 200L
        count > 15 -> 100L
        count > 12 -> 75L
        count > 8 -> 50L
        count > 5 -> 20L
        else -> 0L
    }

    // ---- 永続化 ----

    private fun load() {
        // 有効スポット。未保存（初回）なら全スポットを有効にする。
        val savedEnabled = prefs.getStringSet(KEY_ENABLED, null)
        enabledSpots = if (savedEnabled == null) {
            allSpots.toSet()
        } else {
            savedEnabled.mapNotNull { spotsById[it] }.toSet()
        }

        // 重み（"id=weight" の集合）。
        prefs.getStringSet(KEY_WEIGHTS, null)?.forEach { entry ->
            val idx = entry.lastIndexOf('=')
            if (idx > 0) {
                val id = entry.substring(0, idx)
                val w = entry.substring(idx + 1).toDoubleOrNull()
                if (id in spotsById && w != null) weights[id] = w
            }
        }

        // 自然タブの表示タイプ。未保存なら全タイプを表示。
        selectedDisplayTypes = prefs.getStringSet(KEY_DISPLAY_TYPES, null)
            ?: allTypes.map { it.key }.toSet()
    }

    private fun persist() {
        prefs.edit()
            .putStringSet(KEY_ENABLED, enabledSpots.map { it.id }.toSet())
            .putStringSet(
                KEY_WEIGHTS,
                weights.entries
                    .filter { it.value != WeightManager.DEFAULT_WEIGHT }
                    .map { "${it.key}=${it.value}" }
                    .toSet(),
            )
            .putStringSet(KEY_DISPLAY_TYPES, selectedDisplayTypes)
            .apply()
    }

    override fun onCleared() {
        super.onCleared()
        spinJob?.cancel()
    }

    companion object {
        private const val SPIN_INTERVAL_MS = 50L
        private const val STOP_STEPS = 22
        private const val RESULT_DELAY_MS = 500L

        private const val KEY_ENABLED = "enabled_spot_ids"
        private const val KEY_WEIGHTS = "spot_weights"
        private const val KEY_DISPLAY_TYPES = "display_types"

        /** 分類指定で ViewModel を生成する Factory。 */
        fun factory(app: Application, kind: SpotKind): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SpotRouletteViewModel(app, kind) as T
            }
    }
}
