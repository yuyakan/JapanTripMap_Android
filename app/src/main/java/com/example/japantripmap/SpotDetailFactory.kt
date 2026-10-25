package com.example.japantripmap

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.Color

/**
 * 元データ → 単体詳細（[SpotDetail]）の変換を 1 か所にまとめたもの。
 * 県詳細・プラン・ホーム・近く・祭りタブのどこから開いても同じ見た目（写真ヘッダー含む）になるようにする。
 *
 * 写真は iOS と同じく「写真がある項目だけ」ヘッダーに敷く（観光 / グルメ / 自然 / 祭り）。
 * お土産は商品パッケージの著作権に触れるため写真を持たない（iOS と同じ）。
 */

/** 観光の基調色（iOS AttractionDetailView の accent に相当するブルー）。 */
internal val AttractionAccent = Color(0xFF4C8CFA)

fun attractionSpotDetail(prefecture: Prefecture, a: Attraction): SpotDetail = SpotDetail(
    title = a.name,
    prefecture = prefecture,
    accent = AttractionAccent,
    icon = Icons.Filled.LocationOn,
    description = a.description,
    planCategory = PlanItemCategory.ATTRACTION,
    latitude = a.latitude,
    longitude = a.longitude,
    photoResName = ATTRACTION_PHOTOS[a.name],
    // 観光スポット詳細は写真の有無にかかわらず白背景＋カード枠線（iOS AttractionDetailView）。
    alwaysPhotoStyle = true,
)

fun gourmetSpotDetail(prefecture: Prefecture, g: Gourmet): SpotDetail = SpotDetail(
    title = g.name,
    prefecture = prefecture,
    accent = categoryColor(g.category),
    icon = Icons.Filled.Restaurant,
    description = g.description,
    planCategory = PlanItemCategory.GOURMET,
    photoResName = GOURMET_PHOTOS[g.name],
    badge = categoryLabel(g.category),
    popularity = g.popularity,
    infoRows = listOf(
        "価格帯" to g.price,
        "おすすめ時期" to g.bestSeason,
        "カテゴリ" to categoryLabel(g.category),
    ),
    tabelogKeyword = g.name,
)

fun souvenirSpotDetail(prefecture: Prefecture, s: Souvenir): SpotDetail = SpotDetail(
    title = s.name,
    prefecture = prefecture,
    accent = categoryColor(s.category),
    icon = Icons.Filled.CardGiftcard,
    description = s.description,
    planCategory = PlanItemCategory.SOUVENIR,
    badge = categoryLabel(s.category),
    popularity = s.popularity,
    infoRows = listOf(
        "価格帯" to s.price,
        "おすすめ時期" to s.bestSeason,
        "カテゴリ" to categoryLabel(s.category),
    ),
)

fun festivalSpotDetail(prefecture: Prefecture, f: Festival): SpotDetail = SpotDetail(
    title = f.name,
    prefecture = prefecture,
    accent = festColor(f.category),
    icon = festIcon(f.category),
    description = f.description,
    planCategory = PlanItemCategory.FESTIVAL,
    latitude = f.latitude,
    longitude = f.longitude,
    photoResName = FESTIVAL_PHOTOS[f.name],
    badge = festLabel(f.category),
    popularity = f.scale,
    infoRows = listOf(
        "開催地" to "${prefecture.localizedName()}・${localizeData(f.location)}",
        "時期" to localizeData(f.month),
        "期間" to localizeData(f.duration),
    ),
    features = f.features,
)

/** 温泉スポット（県・名前で引き当て）。 */
fun onsenSpotDetail(prefecture: Prefecture, name: String): SpotDetail? =
    SpotRepository.all(SpotKind.ONSEN).firstOrNull { it.prefecture == prefecture && it.name == name }?.toSpotDetail()

/** 自然スポット（県・名前で引き当て）。 */
fun natureSpotDetail(prefecture: Prefecture, name: String): SpotDetail? =
    SpotRepository.all(SpotKind.NATURE).firstOrNull { it.prefecture == prefecture && it.name == name }?.toSpotDetail()
