package com.example.japantripmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ルーレットのロジック（座標データと重み付き抽選）を検証するユニットテスト。
 */
class RouletteLogicTest {

    @Test
    fun `47都道府県すべてが定義されている`() {
        assertEquals(47, Prefecture.entries.size)
    }

    @Test
    fun `全都道府県が多角形として成立する点数を持つ`() {
        for (prefecture in Prefecture.entries) {
            assertTrue(
                "${prefecture.displayName} の座標点が少なすぎる",
                prefecture.points.size >= 3,
            )
        }
    }

    @Test
    fun `座標は全て0から500の範囲に収まっている`() {
        for (prefecture in Prefecture.entries) {
            for (p in prefecture.points) {
                assertTrue("${prefecture.displayName} x=${p.x}", p.x in 0f..Prefecture.MAP_SIZE)
                assertTrue("${prefecture.displayName} y=${p.y}", p.y in 0f..Prefecture.MAP_SIZE)
            }
        }
    }

    @Test
    fun `沖縄だけが引き出し線を持つ`() {
        for (prefecture in Prefecture.entries) {
            if (prefecture == Prefecture.OKINAWA) {
                assertTrue(prefecture.okinawaLinePoints.isNotEmpty())
            } else {
                assertTrue(prefecture.okinawaLinePoints.isEmpty())
            }
        }
    }

    /** 全県 1.0 の重みマップ。 */
    private fun defaultWeights() =
        Prefecture.entries.associateWith { WeightManager.DEFAULT_WEIGHT }

    @Test
    fun `抽選結果は必ず有効な都道府県の中から返る`() {
        val enabled = setOf(Prefecture.TOKYO, Prefecture.OSAKA, Prefecture.HOKKAIDO)
        repeat(1000) {
            val picked = WeightManager.selectRandomPrefecture(enabled, defaultWeights())
            assertNotNull(picked)
            assertTrue(picked in enabled)
        }
    }

    @Test
    fun `対象が空なら抽選はnullを返す`() {
        assertNull(WeightManager.selectRandomPrefecture(emptySet(), defaultWeights()))
    }

    @Test
    fun `重み0の県は抽選対象から除外される`() {
        val weights = defaultWeights().toMutableMap()
        weights[Prefecture.OSAKA] = 0.0
        val enabled = setOf(Prefecture.TOKYO, Prefecture.OSAKA)
        repeat(500) {
            assertEquals(Prefecture.TOKYO, WeightManager.selectRandomPrefecture(enabled, weights))
        }
    }

    @Test
    fun `重みが大きい県ほど高確率で選ばれる`() {
        val weights = defaultWeights().toMutableMap()
        weights[Prefecture.TOKYO] = 9.0 // 東京 : 大阪 = 9 : 1 を狙う
        weights[Prefecture.OSAKA] = 1.0
        val enabled = setOf(Prefecture.TOKYO, Prefecture.OSAKA)

        var tokyoCount = 0
        val trials = 10000
        repeat(trials) {
            if (WeightManager.selectRandomPrefecture(enabled, weights) == Prefecture.TOKYO) tokyoCount++
        }
        // 理論値 90%。乱数のばらつきを見て 85%〜95% に収まればよしとする。
        val ratio = tokyoCount.toDouble() / trials
        assertTrue("東京の選出率が想定外: $ratio", ratio in 0.85..0.95)
    }

    @Test
    fun `getProbabilityは重み比に応じた確率を返す`() {
        val weights = defaultWeights().toMutableMap()
        weights[Prefecture.TOKYO] = 3.0
        weights[Prefecture.OSAKA] = 1.0
        val enabled = setOf(Prefecture.TOKYO, Prefecture.OSAKA)
        assertEquals(75.0, WeightManager.getProbability(Prefecture.TOKYO, enabled, weights), 0.001)
        assertEquals(25.0, WeightManager.getProbability(Prefecture.OSAKA, enabled, weights), 0.001)
    }

    @Test
    fun `地方グルーピングは全県を11地方に分類する`() {
        val grouped = Prefecture.groupedByRegion()
        assertEquals(11, grouped.size)
        assertEquals(47, grouped.sumOf { it.second.size })
        // 先頭は北海道地方（1県）、末尾は沖縄地方（1県）。
        assertEquals("北海道", grouped.first().first)
        assertEquals(1, grouped.first().second.size)
        assertEquals("沖縄", grouped.last().first)
    }
}
