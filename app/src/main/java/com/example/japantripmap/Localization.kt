package com.example.japantripmap

import java.util.Locale

/**
 * データ層（スポット名・説明文・グルメ・お土産・祭りなど、コードに日本語で直書きされた
 * 値）を、端末ロケールに応じて日本語／英語で出し分けるためのヘルパ。
 *
 * UI 文言は strings.xml / values-en に切り出して stringResource で解決するが、
 * データは約 3,100 件と量が多いため、iOS の翻訳資産から生成した [DATA_JA_TO_EN] を
 * 使って表示時に差し替える方式をとる。iOS 版 MapRoulette と同じ英訳になる。
 */

/** アプリが英語表示すべきロケールかどうか。日本語以外はすべて英語にフォールバックする。 */
fun isEnglishLocale(): Boolean =
    Locale.getDefault().language != Locale.JAPANESE.language

/**
 * データ文字列を現在のロケール向けに解決する。
 *
 * - 日本語ロケール: そのまま日本語を返す。
 * - 英語ロケール: [DATA_JA_TO_EN] に対応があればその英訳を、
 *   なければ価格表記（例: "1,000-1,800円" → "¥1,000-1,800"）を機械変換し、
 *   それでも変換できなければ元の日本語をそのまま返す（欠落しても壊れないように）。
 */
fun localizeData(ja: String): String {
    if (!isEnglishLocale()) return ja
    DATA_JA_TO_EN[ja]?.let { return it }
    return localizePriceRange(ja) ?: ja
}

/** リストの各要素をデータ文字列として英訳する。グルメ・お土産名の配列などに使う。 */
fun localizeDataList(list: List<String>): List<String> =
    if (isEnglishLocale()) list.map { localizeData(it) } else list

/**
 * タイプ／カテゴリのラベル（温泉タイプ・自然タイプ・グルメ／お土産カテゴリなど）専用の英訳。
 *
 * これらは文脈依存の語（例: "絶景" は温泉タイプでは "Scenic Hot Spring" だが、
 * 説明文中では "Spectacular view"）なので、汎用の [DATA_JA_TO_EN] は使わず、
 * iOS のフル名称（onsen_type.* / nature_type.* / *_category）に対応させた専用表で解決する。
 * SpotTypeMeta.name / tagName やカテゴリラベルの表示時に使う。非対応なら元の文字列を返す。
 */
fun localizeTypeLabel(ja: String): String {
    if (!isEnglishLocale()) return ja
    return TYPE_LABEL_JA_TO_EN[ja] ?: ja
}

/**
 * 「基本情報」行のラベル（価格帯・カテゴリ・泉質タイプ等）を英訳する。
 *
 * SpotDetail.infoRows は非 Composable な場所（toSpotDetail など）で組み立てるため、
 * stringResource ではなくこの固定表で解決する。strings.xml の同名リソースと訳を揃える。
 */
fun localizeInfoLabel(ja: String): String {
    if (!isEnglishLocale()) return ja
    return INFO_LABEL_JA_TO_EN[ja] ?: ja
}

private val INFO_LABEL_JA_TO_EN: Map<String, String> = mapOf(
    "価格帯" to "Price Range",
    "おすすめ時期" to "Best Season",
    "カテゴリ" to "Category",
    "泉質タイプ" to "Spring Type",
    "種別" to "Type",
    "開催地" to "Location",
    "時期" to "Period",
    "期間" to "Duration",
)

// タイプ／カテゴリラベルの日本語 → 英語（iOS フル名称）。
private val TYPE_LABEL_JA_TO_EN: Map<String, String> = mapOf(
    // 温泉タイプ（onsen_type.*）。name と tagName（短縮）の両方を含む。
    "絶景" to "Scenic Hot Spring",
    "歴史" to "Historical Hot Spring",
    "療養" to "Therapeutic Hot Spring",
    "リゾート" to "Resort",
    "山あい" to "Mountain Hot Spring",
    "山" to "Mountain",
    "海辺" to "Seaside Hot Spring",
    "海" to "Sea",
    "スキー" to "Ski Hot Spring",
    // 自然タイプ（nature_type.*）
    "夜景" to "Night View",
    "星空" to "Starry Sky",
    "キャンプ" to "Camping",
    "自然" to "Nature",
    // グルメカテゴリ（food_category_*）
    "ラーメン" to "Ramen",
    "海鮮" to "Seafood",
    "肉" to "Meat Dishes",
    "スイーツ" to "Sweets",
    "郷土料理" to "Local Cuisine",
    "ドリンク" to "Beverages",
    "野菜・果物" to "Vegetables & Fruits",
    // お土産カテゴリ（souvenirCategory.*）
    "食品" to "Food",
    "工芸品" to "Crafts",
    "織物" to "Textiles / Clothing",
    "陶磁器" to "Ceramics",
    "地域特産" to "Regional Specialties",
    // 祭りカテゴリ（festival.*_tag）
    "夏祭り" to "Summer",
    "花火" to "Fireworks",
    "伝統" to "Traditional",
    "踊り" to "Dance",
    "グルメ" to "Food",
    "季節" to "Seasonal",
    "宗教" to "Religious",
    "春" to "Spring",
    "秋" to "Autumn",
    "冬" to "Winter",
    "桜" to "Cherry Blossom",
    "イルミ" to "Illumination",
    "雪" to "Snow",
)

// 価格レンジ（例: "1,000-1,800円" / "800-1,200円"）を "¥1,000-1,800" 形式へ。
// iOS の価格表記に合わせる。翻訳表に無い価格帯のフォールバック用。
private val PRICE_RANGE = Regex("""^([\d,]+)-([\d,]+)円$""")

private fun localizePriceRange(ja: String): String? {
    val m = PRICE_RANGE.matchEntire(ja) ?: return null
    return "¥${m.groupValues[1]}-${m.groupValues[2]}"
}
