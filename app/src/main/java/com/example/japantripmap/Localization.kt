package com.example.japantripmap

import java.util.Locale

/**
 * データ層（スポット名・説明文・グルメ・お土産・祭りなど、コードに日本語で直書きされた
 * 値）を、端末ロケールに応じて各言語で出し分けるためのヘルパ。
 *
 * UI 文言は strings.xml / values-xx に切り出して stringResource で解決するが、
 * データは約 3,100 件と量が多いため、iOS の翻訳資産から生成した言語別マップ
 * （[DATA_JA_TO_EN] など）を使って表示時に差し替える方式をとる。
 * iOS 版 MapRoulette が対応する 12 言語（ja/en/de/es/fr/id/ko/th/vi/
 * 簡体字/繁体字/香港中国語）に対応する。
 *
 * 生成物:
 * - DataTranslations.kt: 汎用データ表（DATA_JA_TO_*）… tools/gen_data_translations.py
 * - LabelTranslations.kt: タイプ／情報ラベル表（TYPE_LABEL_* / INFO_LABEL_*）… tools/gen_label_translations.py
 */

/** アプリが対応する表示言語。日本語がデフォルト（＝データ原文）。 */
enum class AppLocale { JA, EN, DE, ES, FR, ID, KO, TH, VI, ZH_HANS, ZH_HANT, ZH_HK }

/**
 * 端末ロケールを [AppLocale] に解決する。
 *
 * 中国語は script（Hans/Hant）と region（HK/TW/MO）から簡体・繁体・香港を判定する。
 * 未対応の言語はすべて英語にフォールバックする（iOS 版と同じ挙動）。
 */
fun currentAppLocale(): AppLocale {
    val loc = Locale.getDefault()
    return when (loc.language) {
        Locale.JAPANESE.language -> AppLocale.JA
        "de" -> AppLocale.DE
        "es" -> AppLocale.ES
        "fr" -> AppLocale.FR
        "in", "id" -> AppLocale.ID // Android の Indonesian は legacy "in"
        "ko" -> AppLocale.KO
        "th" -> AppLocale.TH
        "vi" -> AppLocale.VI
        "zh" -> resolveChinese(loc)
        else -> AppLocale.EN
    }
}

private fun resolveChinese(loc: Locale): AppLocale {
    val script = loc.script // "Hans" / "Hant"（無いこともある）
    val region = loc.country.uppercase(Locale.ROOT)
    return when {
        script == "Hans" -> AppLocale.ZH_HANS
        script == "Hant" -> if (region == "HK" || region == "MO") AppLocale.ZH_HK else AppLocale.ZH_HANT
        region == "CN" || region == "SG" -> AppLocale.ZH_HANS
        region == "HK" || region == "MO" -> AppLocale.ZH_HK
        region == "TW" -> AppLocale.ZH_HANT
        else -> AppLocale.ZH_HANS // 既定は簡体字
    }
}

/** その言語の汎用データ表（日本語→訳）。日本語ロケールは null（原文のまま）。 */
private fun dataMapFor(locale: AppLocale): Map<String, String>? = when (locale) {
    AppLocale.JA -> null
    AppLocale.EN -> DATA_JA_TO_EN
    AppLocale.DE -> DATA_JA_TO_DE
    AppLocale.ES -> DATA_JA_TO_ES
    AppLocale.FR -> DATA_JA_TO_FR
    AppLocale.ID -> DATA_JA_TO_ID
    AppLocale.KO -> DATA_JA_TO_KO
    AppLocale.TH -> DATA_JA_TO_TH
    AppLocale.VI -> DATA_JA_TO_VI
    AppLocale.ZH_HANS -> DATA_JA_TO_ZH_HANS
    AppLocale.ZH_HANT -> DATA_JA_TO_ZH_HANT
    AppLocale.ZH_HK -> DATA_JA_TO_ZH_HK
}

private fun typeLabelMapFor(locale: AppLocale): Map<String, String>? = when (locale) {
    AppLocale.JA -> null
    AppLocale.EN -> TYPE_LABEL_EN
    AppLocale.DE -> TYPE_LABEL_DE
    AppLocale.ES -> TYPE_LABEL_ES
    AppLocale.FR -> TYPE_LABEL_FR
    AppLocale.ID -> TYPE_LABEL_ID
    AppLocale.KO -> TYPE_LABEL_KO
    AppLocale.TH -> TYPE_LABEL_TH
    AppLocale.VI -> TYPE_LABEL_VI
    AppLocale.ZH_HANS -> TYPE_LABEL_ZH_HANS
    AppLocale.ZH_HANT -> TYPE_LABEL_ZH_HANT
    AppLocale.ZH_HK -> TYPE_LABEL_ZH_HK
}

private fun infoLabelMapFor(locale: AppLocale): Map<String, String>? = when (locale) {
    AppLocale.JA -> null
    AppLocale.EN -> INFO_LABEL_EN
    AppLocale.DE -> INFO_LABEL_DE
    AppLocale.ES -> INFO_LABEL_ES
    AppLocale.FR -> INFO_LABEL_FR
    AppLocale.ID -> INFO_LABEL_ID
    AppLocale.KO -> INFO_LABEL_KO
    AppLocale.TH -> INFO_LABEL_TH
    AppLocale.VI -> INFO_LABEL_VI
    AppLocale.ZH_HANS -> INFO_LABEL_ZH_HANS
    AppLocale.ZH_HANT -> INFO_LABEL_ZH_HANT
    AppLocale.ZH_HK -> INFO_LABEL_ZH_HK
}

/**
 * データ文字列を現在のロケール向けに解決する。
 *
 * - 日本語ロケール: そのまま日本語を返す。
 * - それ以外: 該当言語の [dataMapFor] に対応があればその訳を、
 *   なければ価格表記（例 "1,000-1,800円"）を機械変換し、
 *   それでも変換できなければ元の日本語をそのまま返す（欠落しても壊れないように）。
 *   中国語圏では未訳の固有名詞が漢字のまま残ることがあるが表示上は自然。
 */
fun localizeData(ja: String): String {
    val map = dataMapFor(currentAppLocale()) ?: return ja
    map[ja]?.let { return it }
    return localizePriceRange(ja) ?: ja
}

/** リストの各要素をデータ文字列として翻訳する。グルメ・お土産名の配列などに使う。 */
fun localizeDataList(list: List<String>): List<String> {
    if (dataMapFor(currentAppLocale()) == null) return list
    return list.map { localizeData(it) }
}

/**
 * タイプ／カテゴリのラベル（温泉タイプ・自然タイプ・グルメ／お土産カテゴリなど）専用の翻訳。
 *
 * これらは文脈依存の語（例: "絶景" は温泉タイプでは "Scenic Hot Spring" だが、
 * 説明文中では "Spectacular view"）なので、汎用の [dataMapFor] は使わず、
 * strings.xml のフル名称に対応させた専用表（[typeLabelMapFor]）で解決する。
 * SpotTypeMeta.name / tagName やカテゴリラベルの表示時に使う。非対応なら元の文字列を返す。
 */
fun localizeTypeLabel(ja: String): String {
    val map = typeLabelMapFor(currentAppLocale()) ?: return ja
    return map[ja] ?: ja
}

/**
 * 「基本情報」行のラベル（価格帯・カテゴリ・泉質タイプ等）を翻訳する。
 *
 * SpotDetail.infoRows は非 Composable な場所（toSpotDetail など）で組み立てるため、
 * stringResource ではなくこの固定表で解決する。strings.xml の同名リソースと訳を揃える。
 */
fun localizeInfoLabel(ja: String): String {
    val map = infoLabelMapFor(currentAppLocale()) ?: return ja
    return map[ja] ?: ja
}

// 価格レンジ（例: "1,000-1,800円" / "800-1,200円"）を "¥1,000-1,800" 形式へ。
// iOS の価格表記に合わせる。翻訳表に無い価格帯のフォールバック用（言語非依存）。
private val PRICE_RANGE = Regex("""^([\d,]+)-([\d,]+)円$""")

private fun localizePriceRange(ja: String): String? {
    val m = PRICE_RANGE.matchEntire(ja) ?: return null
    return "¥${m.groupValues[1]}-${m.groupValues[2]}"
}
