import re, sys

# TYPE_LABEL / INFO_LABEL（文脈依存のタイプ・カテゴリ・情報行ラベル）を全言語ぶん生成する。
#
# これらは Kotlin データ中の短い日本語（例 "絶景" "療養" "夏祭り"）を、
# 対応する strings.xml リソースの訳で解決する手管理表。iOS の汎用データ表とは別扱い。
# ここでは各日本語キーを strings.xml のリソース名に対応づけ、
# values-xx/strings.xml から訳を読み出して LabelTranslations.kt を生成する。

RES = "/Users/uebetsunawayuuya/AndroidStudioProjects/JapanTripMap/app/src/main/res"
KDIR = "/Users/uebetsunawayuuya/AndroidStudioProjects/JapanTripMap/app/src/main/java/com/example/japantripmap"

# lang サフィックス -> values ディレクトリ名
LANG_DIR = [
    ("EN", "values-en"),
    ("DE", "values-de"),
    ("ES", "values-es"),
    ("FR", "values-fr"),
    ("ID", "values-in"),
    ("KO", "values-ko"),
    ("TH", "values-th"),
    ("VI", "values-vi"),
    ("ZH_HANS", "values-b+zh+Hans"),
    ("ZH_HANT", "values-b+zh+Hant"),
    ("ZH_HK", "values-b+zh+Hant+HK"),
]

# TYPE_LABEL: Kotlin データ中の日本語 -> strings.xml リソース名
# （Localization.kt の既存 TYPE_LABEL_JA_TO_EN と同じ対象。EN 訳と一致するよう対応づけ）
TYPE_LABEL = {
    "絶景": "onsen_type_scenic",
    "歴史": "onsen_type_historical",
    "療養": "onsen_type_therapeutic",
    "リゾート": "onsen_type_resort",
    "山あい": "onsen_type_mountain",
    "山": "spot_tag_mountain",   # "Mountain" 単独（mountain 温泉タイプの短縮タグ）
    "海辺": "onsen_type_seaside",
    "海": "nature_type_sea",
    "スキー": "onsen_type_ski",
    "夜景": "nature_type_night_view",
    "星空": "nature_type_starry_sky",
    "キャンプ": "nature_type_camping",
    "自然": "nature_type_fallback",
    "ラーメン": "food_category_ramen",
    "海鮮": "food_category_seafood",
    "肉": "food_category_meat",
    "スイーツ": "food_category_sweets",
    "郷土料理": "food_category_local",
    "ドリンク": "food_category_drinks",
    "野菜・果物": "food_category_vegetables",
    "食品": "souvenir_category_food",
    "工芸品": "souvenir_category_crafts",
    "織物": "souvenir_category_textiles",
    "陶磁器": "souvenir_category_ceramics",
    "地域特産": "souvenir_category_regional",
    "夏祭り": "festival_category_summer",
    "花火": "festival_category_fireworks",
    "伝統": "festival_category_traditional",
    "踊り": "festival_category_dance",
    "グルメ": "festival_category_food",
    "季節": "festival_category_seasonal",
    "宗教": "festival_category_religious",
    "春": "festival_filter_spring",
    "秋": "festival_filter_autumn",
    "冬": "festival_filter_winter",
    "桜": "festival_filter_sakura",
    "イルミ": "festival_filter_illumination",
    "雪": "festival_filter_snow",
}

# INFO_LABEL: 基本情報行のラベル -> strings.xml リソース名
INFO_LABEL = {
    "価格帯": "info_price_range",
    "おすすめ時期": "info_best_season",
    "カテゴリ": "info_category",
    "泉質タイプ": "info_onsen_quality",
    "種別": "spot_type_kind",
    "開催地": "festival_location",
    "時期": "festival_period",
    "期間": "festival_duration",
}


def parse_res(path):
    d = {}
    with open(path, encoding="utf-8") as f:
        c = f.read()
    for m in re.finditer(r'<string name="([^"]+)">(.*?)</string>', c, re.S):
        name, val = m.group(1), m.group(2)
        # unescape android string escapes we use
        val = val.replace("\\'", "'").replace('\\"', '"').replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        d[name] = val
    return d


res = {}
for suffix, d in LANG_DIR:
    res[suffix] = parse_res(f"{RES}/{d}/strings.xml")

missing = []
for jp, rk in list(TYPE_LABEL.items()) + list(INFO_LABEL.items()):
    for suffix, _ in LANG_DIR:
        if rk not in res[suffix]:
            missing.append((suffix, jp, rk))
if missing:
    for suffix, jp, rk in missing:
        print(f"  MISSING res: [{suffix}] {jp!r} -> {rk}", file=sys.stderr)
    print(f"合計 {len(missing)} 件のリソース欠落。strings.xml に追加が必要。", file=sys.stderr)


def kesc(s):
    return s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$")


def emit_map(name_prefix, mapping):
    out = []
    for suffix, _ in LANG_DIR:
        out.append(f"internal val {name_prefix}_{suffix}: Map<String, String> = mapOf(")
        for jp, rk in mapping.items():
            v = res[suffix].get(rk)
            if v is None:
                continue
            out.append(f'    "{kesc(jp)}" to "{kesc(v)}",')
        out.append(")")
        out.append("")
    return out


lines = ["package com.example.japantripmap", ""]
lines.append("/**")
lines.append(" * タイプ／カテゴリ／情報行ラベルの 日本語 → 各言語 対応表。")
lines.append(" * strings.xml（values-xx）の訳から生成。手編集しないこと。")
lines.append(" * 生成元: tools/gen_label_translations.py")
lines.append(" */")
lines.append("")
lines += emit_map("TYPE_LABEL", TYPE_LABEL)
lines += emit_map("INFO_LABEL", INFO_LABEL)

with open(f"{KDIR}/LabelTranslations.kt", "w", encoding="utf-8") as f:
    f.write("\n".join(lines))
print("Wrote LabelTranslations.kt", file=sys.stderr)
