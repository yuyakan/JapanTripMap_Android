package com.example.japantripmap

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * データ層の英語ローカライズ（localizeData / localizeTypeLabel / localizeInfoLabel）を検証する。
 * 端末ロケールを英語／日本語に切り替えて期待どおり出し分けられることを確認する。
 */
class LocalizationTest {

    private lateinit var original: Locale

    @Before
    fun saveLocale() {
        original = Locale.getDefault()
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(original)
    }

    @Test
    fun `日本語ロケールではデータをそのまま返す`() {
        Locale.setDefault(Locale.JAPANESE)
        assertEquals("登別温泉", localizeData("登別温泉"))
        assertEquals("絶景", localizeTypeLabel("絶景"))
        assertEquals("価格帯", localizeInfoLabel("価格帯"))
    }

    @Test
    fun `英語ロケールではスポット名を英訳する`() {
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("Noboribetsu Onsen", localizeData("登別温泉"))
        // 都道府県名・地方名も対応表で解決できる。
        assertEquals("Hokkaido", localizeData("北海道"))
        assertEquals("Tohoku", localizeData("東北"))
    }

    @Test
    fun `英語ロケールでは価格帯を機械変換でフォールバックする`() {
        Locale.setDefault(Locale.ENGLISH)
        // 翻訳表に無い価格帯は "X-Y円" -> "¥X-Y" に変換する。
        assertEquals("¥1,000-1,800", localizeData("1,000-1,800円"))
    }

    @Test
    fun `タイプラベルは文脈に応じたフル名称になる`() {
        Locale.setDefault(Locale.ENGLISH)
        // 温泉タイプの "絶景" は "Spectacular view" ではなく "Scenic Hot Spring"。
        assertEquals("Scenic Hot Spring", localizeTypeLabel("絶景"))
        assertEquals("Night View", localizeTypeLabel("夜景"))
        assertEquals("Ramen", localizeTypeLabel("ラーメン"))
        // 一方で一般データとしての "絶景" は説明文向けの訳になる。
        assertEquals("Spectacular view", localizeData("絶景"))
    }

    @Test
    fun `infoRowsのラベルを英訳する`() {
        Locale.setDefault(Locale.ENGLISH)
        assertEquals("Price Range", localizeInfoLabel("価格帯"))
        assertEquals("Spring Type", localizeInfoLabel("泉質タイプ"))
        assertEquals("Category", localizeInfoLabel("カテゴリ"))
    }

    @Test
    fun `未知の文字列は英語ロケールでもそのまま返す`() {
        Locale.setDefault(Locale.ENGLISH)
        // ユーザー入力（プランのカスタム項目名など）は英訳対象に無いのでそのまま。
        assertEquals("My custom note", localizeData("My custom note"))
        assertEquals("存在しない架空スポット", localizeData("存在しない架空スポット"))
    }

    @Test
    fun `実データのスポット名がほぼ全て英訳表に載っている`() {
        Locale.setDefault(Locale.ENGLISH)
        val onsenNames = SpotRepository.allOnsens.map { it.name }
        val untranslated = onsenNames.filter { localizeData(it) == it }
        // 温泉スポット名は iOS 資産で全件カバーされているはず。
        assertTrue(
            "英訳されない温泉スポット名: $untranslated",
            untranslated.isEmpty(),
        )
    }
}
