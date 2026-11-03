package com.example.japantripmap

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** ユーザーカードの保存形式・URL・プラン同期・写真の縮小サイズを検証する。 */
class UserCardTest {

    private fun card(
        name: String,
        prefecture: Prefecture = Prefecture.TOKYO,
        kind: UserCardKind = UserCardKind.CAFE,
        createdAt: Long = 0,
    ) = UserCard(prefectureName = prefecture.name, kind = kind, name = name, createdAt = createdAt)

    @Test
    fun `県とセクションごとに新しい順で絞り込む`() {
        val cards = listOf(
            card("古い店", createdAt = 100),
            card("新しい店", createdAt = 200),
            card("別のセクション", kind = UserCardKind.SOUVENIR),
            card("別の県", prefecture = Prefecture.ISHIKAWA),
        )
        assertEquals(listOf("新しい店", "古い店"), cards.inSection(Prefecture.TOKYO, UserCardKind.CAFE).map { it.name })
    }

    @Test
    fun `URLはスキームを補いWeb以外や形式の不正なものは開かせない`() {
        assertEquals("https://example.com/menu", normalizedUrl("example.com/menu"))
        assertEquals("http://example.com", normalizedUrl(" http://example.com "))
        assertNull(normalizedUrl("javascript://example.com"))
        assertNull(normalizedUrl("not a url"))
        assertNull(normalizedUrl("example"))
        assertNull(normalizedUrl(""))
    }

    @Test
    fun `JSONを経由しても全項目を保つ`() {
        val original = card("展望台", kind = UserCardKind.OTHER_FESTIVAL).copy(
            description = "夕日がきれい",
            url = "example.com",
            photoFileName = "a.jpg",
            latitude = 35.0,
            longitude = 135.0,
            placeName = "展望台",
            address = "東京都",
        )
        val encoded = Json.encodeToString<List<UserCard>>(listOf(original))
        assertEquals(listOf(original), Json.decodeFromString<List<UserCard>>(encoded))
    }

    @Test
    fun `カードの編集は同じ種別と県の項目にだけ反映する`() {
        val previous = card("古い店")
        val updated = previous.copy(name = "新しい店", description = "説明")
        val target = PlanItem(category = PlanItemCategory.GOURMET, prefectureName = "東京都", name = "古い店")
        val otherPrefecture = target.copy(id = "a", prefectureName = "石川県")
        val otherCategory = target.copy(id = "b", category = PlanItemCategory.SOUVENIR)
        val plans = listOf(TravelPlan(title = "旅", items = listOf(target, otherPrefecture, otherCategory)))

        val items = plans.withUserCardSynced(previous, updated).single().items

        assertEquals("新しい店", items[0].name)
        assertEquals("説明", items[0].detail)
        assertEquals(target.id, items[0].id)
        assertEquals(otherPrefecture, items[1])
        assertEquals(otherCategory, items[2])
    }

    @Test
    fun `表紙写真は長辺を上限に収める`() {
        assertEquals(1600 to 800, fittedPhotoSize(3200, 1600, 1600))
        assertEquals(800 to 1600, fittedPhotoSize(1600, 3200, 1600))
        assertEquals(1200 to 900, fittedPhotoSize(1200, 900, 1600))
    }

    @Test
    fun `間引きは上限を下回らない範囲に留める`() {
        assertEquals(1, photoSampleSize(3199, 1000, 1600))
        assertEquals(2, photoSampleSize(3200, 1000, 1600))
        assertEquals(4, photoSampleSize(6400, 1000, 1600))
    }
}
