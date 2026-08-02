package com.example.japantripmap

/**
 * 都道府県ごとの重みを使った抽選・確率計算のロジック。
 *
 * iOS 版 MapRoulette の WeightManager.swift を移植したもの。
 * 状態（weights / enabled）は呼び出し側（ViewModel）が保持し、ここは純粋な計算に徹する。
 * 重みは 0.0 以上の Double で、0 の県は抽選対象から除外される。
 */
object WeightManager {

    const val DEFAULT_WEIGHT = 1.0
    const val MIN_WEIGHT = 0.1
    const val MAX_WEIGHT = 10.0

    /**
     * 有効な都道府県の中から、重みに比例した確率で 1 つを抽選する。
     * 重みが 0 の県は除外する。対象が無ければ null を返す。
     */
    fun selectRandomPrefecture(
        enabled: Set<Prefecture>,
        weights: Map<Prefecture, Double>,
    ): Prefecture? {
        val valid = enabled.filter { (weights[it] ?: DEFAULT_WEIGHT) > 0.0 }
        if (valid.isEmpty()) return null

        val totalWeight = valid.sumOf { weights[it] ?: DEFAULT_WEIGHT }
        val randomValue = Math.random() * totalWeight

        var current = 0.0
        for (prefecture in valid) {
            current += weights[prefecture] ?: DEFAULT_WEIGHT
            if (randomValue < current) {
                return prefecture
            }
        }
        return valid.last()
    }

    /** 有効な都道府県の中で、指定県が選ばれる確率（%）を返す。 */
    fun getProbability(
        prefecture: Prefecture,
        enabled: Set<Prefecture>,
        weights: Map<Prefecture, Double>,
    ): Double {
        if (prefecture !in enabled) return 0.0

        val valid = enabled.filter { (weights[it] ?: DEFAULT_WEIGHT) > 0.0 }
        val totalWeight = valid.sumOf { weights[it] ?: DEFAULT_WEIGHT }
        return if (totalWeight > 0.0) {
            (weights[prefecture] ?: DEFAULT_WEIGHT) / totalWeight * 100.0
        } else {
            0.0
        }
    }
}
