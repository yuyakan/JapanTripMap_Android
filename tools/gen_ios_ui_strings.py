#!/usr/bin/env python3
"""
iOS 版 MapRoulette の Localizable.strings（全 12 言語）から、Android の strings.xml へ
UI 文言を取り込む。iOS 3.0.0 刷新で増えた画面（ホーム・近く・訪問済み・費用・検索など）の
文言を、iOS と同じ訳で全言語に揃えるためのもの。

- ENTRIES の (Android キー, iOS キー, モード) を各言語の values-xx/strings.xml に書き込む。
  モード "add" は未定義のときだけ追加、"set" は既存値も iOS の値で上書きする
  （iOS 側で文言が変わったキー用）。
- 書式指定子は iOS → Android に変換する（%@ → %s、%1$@ → %1$s）。
- 生成後も手で書いた既存キーはそのまま残る（このスクリプトは ENTRIES のキーしか触らない）。

使い方: python3 tools/gen_ios_ui_strings.py
"""
import os
import re
import sys

BASE = "/Users/uebetsunawayuuya/MapRoulette/MapRoulette/Base"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = f"{ROOT}/app/src/main/res"

# iOS lproj → Android values ディレクトリ
LANGS = [
    ("ja", "values"),
    ("en", "values-en"),
    ("de", "values-de"),
    ("es", "values-es"),
    ("fr", "values-fr"),
    ("id", "values-in"),
    ("ko", "values-ko"),
    ("th", "values-th"),
    ("vi", "values-vi"),
    ("zh-Hans", "values-b+zh+Hans"),
    ("zh-Hant", "values-b+zh+Hant"),
    ("zh-HK", "values-b+zh+Hant+HK"),
]

ENTRIES = [
    # タブ
    ("tab_map", "tab.map", "add"),
    ("tab_nearby", "tab.nearby", "add"),
    ("tab_home", "tab.home", "add"),
    ("tab_onsen_place", "tab.onsen", "add"),
    ("tab_band_prefecture", "tab.prefecture", "add"),
    ("tab_band_nature", "tab.nature", "add"),
    # 共通
    ("common_edit", "common.edit", "add"),
    ("common_add", "common.add", "add"),
    ("common_delete", "common.delete", "add"),
    ("common_create", "common.create", "add"),
    ("common_ok", "common.ok", "add"),
    ("common_readmore", "common.readmore", "add"),
    ("common_collapse", "common.collapse", "add"),
    # 県詳細
    ("tourism_favorite", "tourism_detail_favorite", "add"),
    ("tourism_unfavorite", "tourism_detail_unfavorite", "add"),
    ("tourism_section_nature", "tourism_detail_nature", "add"),
    ("tourism_section_festival", "festival.summer_fireworks", "add"),
    ("tourism_section_festival_subtitle", "festival.experience", "add"),
    ("tourism_section_seasons", "festival.seasons", "add"),
    ("tourism_section_seasons_subtitle", "festival.experience_seasons", "add"),
    # 近く
    ("nearby_filter_all", "nearby.filter.all", "add"),
    ("nearby_filter_tourism", "nearby.filter.tourism", "add"),
    ("nearby_distance_km", "nearby.distance.km", "add"),
    ("nearby_distance_m", "nearby.distance.m", "add"),
    ("nearby_no_results", "nearby.noResults", "add"),
    ("nearby_search_this_area", "nearby.searchThisArea", "add"),
    # 県検索
    ("prefecture_search_placeholder", "prefecture.search.placeholder", "add"),
    ("prefecture_search_empty", "prefecture.search.empty", "add"),
    ("prefecture_search_onsen_chip", "prefecture.search.onsen_chip", "add"),
    # ルーレット設定: 規約・プライバシー
    ("legal", "legal", "add"),
    ("legal_privacy_policy", "legal.privacy_policy", "add"),
    ("legal_youtube_terms", "legal.youtube_terms", "add"),
    ("legal_google_privacy", "legal.google_privacy", "add"),
    # ホーム
    ("home_section_ongoing", "home.section.ongoing", "add"),
    ("home_section_upcoming", "home.section.upcoming", "add"),
    ("home_ongoing_empty_title", "home.ongoing.empty.title", "add"),
    ("home_ongoing_empty_subtitle", "home.ongoing.empty.subtitle", "add"),
    ("home_ongoing_empty_action", "home.ongoing.empty.action", "add"),
    ("home_tab_recent", "home.tab.recent", "add"),
    ("home_tab_favorite", "home.tab.favorite", "add"),
    ("home_tab_recommended", "home.tab.recommended", "add"),
    ("home_roulette_title", "home.roulette.title", "add"),
    ("home_theme_japan_three_views", "home.theme.japanThreeViews.title", "add"),
    ("home_theme_three_famous_onsen", "home.theme.threeFamousOnsen.title", "add"),
    ("home_theme_three_gardens", "home.theme.threeGardens.title", "add"),
    ("home_theme_three_night_views", "home.theme.threeNightViews.title", "add"),
    ("home_theme_sea_and_beach", "home.theme.seaAndBeach.title", "add"),
    ("home_trend_group_title", "home.trend.group.title", "add"),
    ("home_trend_source_note", "home.trend.source.note", "add"),
    ("home_trend_source_developed", "home.trend.source.developed", "add"),
    ("home_trend_category_cafe", "home.trend.category.cafe", "add"),
    ("home_trend_category_spot", "home.trend.category.spot", "add"),
    ("home_trend_category_gourmet", "home.trend.category.gourmet", "add"),
    ("shorts_offline_title", "shorts.offline.title", "add"),
    ("shorts_offline_message", "shorts.offline.message", "add"),
    # マイプラン（一覧・状態・訪問済み）
    ("plan_section_list", "plan.section.list", "add"),
    ("plan_section_visited", "plan.section.visited", "add"),
    ("plan_status_upcoming", "plan.status.upcoming", "add"),
    ("plan_status_ongoing", "plan.status.ongoing", "add"),
    ("plan_status_completed", "plan.status.completed", "add"),
    ("plan_status_label", "plan.status.label", "add"),
    ("plan_create_title", "plan.create", "add"),
    ("plan_title_label", "plan.title.label", "add"),
    ("plan_title_placeholder", "plan.title.placeholder", "add"),
    ("plan_card_empty", "plan.card.empty", "add"),
    ("plan_daycount_short", "plan.daycount.short", "add"),
    ("plan_itemcount_format", "plan.itemcount.format", "add"),
    ("visited_state_toggle", "visited.state.toggle", "add"),
    ("visited_related_plans", "visited.related.plans", "add"),
    ("visited_related_none", "visited.related.none", "add"),
    ("visited_region_header", "visited.region.header", "add"),
    ("visited_region_count_format", "visited.region.count.format", "add"),
    ("visited_empty_title", "visited.empty.title", "add"),
    ("visited_empty_message", "visited.empty.message", "add"),
    # プラン詳細
    ("plan_section_itinerary", "plan.section.itinerary", "add"),
    ("plan_section_cost", "plan.section.cost", "add"),
    ("plan_detail_empty", "plan.detail.empty", "add"),
    ("plan_detail_empty_edithint", "plan.detail.empty.edithint", "add"),
    ("plan_prefectures_add", "plan.prefectures.add", "add"),
    ("plan_prefectures_derived", "plan.prefectures.derived", "add"),
    ("plan_prefectures_add_count", "plan.prefectures.add.count", "add"),
    ("plan_spot_add", "plan.spot.add", "add"),
    ("plan_spot_suggested", "plan.spot.suggested", "add"),
    ("plan_spot_change_prefecture", "plan.spot.change_prefecture", "add"),
    ("plan_spot_empty_category", "plan.spot.empty_category", "add"),
    ("plan_item_editmemo", "plan.item.editmemo", "add"),
    ("plan_item_addmemo", "plan.item.addmemo", "add"),
    ("plan_item_memo_title", "plan.item.memo", "add"),
    ("plan_memo_label", "plan.memo.label", "add"),
    ("plan_day_empty", "plan.day.empty", "add"),
    ("plan_timeblock_delete_confirm", "plan.timeblock.delete.confirm", "add"),
    ("plan_timeblock_delete_message", "plan.timeblock.delete.message", "add"),
    ("plan_timeblock_settime", "plan.timeblock.settime", "add"),
    ("plan_timeblock_empty", "plan.timeblock.empty", "add"),
    ("plan_timeblock_title_label", "plan.timeblock.title.label", "add"),
    ("plan_timeblock_title_placeholder", "plan.timeblock.title.placeholder", "add"),
    ("plan_category_expense", "plan.category.expense", "add"),
    # iOS 側で文言が変わった既存キー（Android の既存訳を iOS の新しい文言で上書き）
    ("plan_add_custom", "plan.day.addcustom", "set"),
    ("plan_add_time_block", "plan.timeblock.add", "set"),
    ("plan_time_block", "plan.timeblock.edit", "set"),
    ("plan_delete_this_block", "plan.timeblock.delete", "set"),
    ("plan_delete_block", "plan.timeblock.delete", "set"),
    ("plan_split_by_day", "plan.grouping.day.toggle", "set"),
    ("plan_days_count", "plan.daycount.format", "set"),
    ("plan_location_add_optional", "plan.custom.location.add", "set"),
    ("plan_start_time", "plan.timeblock.starttime", "set"),
    ("detail_add_location", "plan.locatable.add", "set"),
    ("plan_day_format", "plan.day.format", "set"),
    ("plan_unassigned", "plan.day.unassigned", "set"),
    ("detail_change_location", "plan.locatable.change", "set"),
    # グルメ詳細「おすすめの楽しみ方」
    ("gourmet_recommended_enjoyment", "recommended_enjoyment", "add"),
    ("ramen_tip_1", "ramen_tip_1", "add"),
    ("ramen_tip_2", "ramen_tip_2", "add"),
    ("ramen_tip_3", "ramen_tip_3", "add"),
    ("seafood_tip_1", "seafood_tip_1", "add"),
    ("seafood_tip_2", "seafood_tip_2", "add"),
    ("seafood_tip_3", "seafood_tip_3", "add"),
    ("meat_tip_1", "meat_tip_1", "add"),
    ("meat_tip_2", "meat_tip_2", "add"),
    ("meat_tip_3", "meat_tip_3", "add"),
    ("sweets_tip_1", "sweets_tip_1", "add"),
    ("sweets_tip_2", "sweets_tip_2", "add"),
    ("sweets_tip_3", "sweets_tip_3", "add"),
    ("local_tip_1", "local_tip_1", "add"),
    ("local_tip_2", "local_tip_2", "add"),
    ("local_tip_3", "local_tip_3", "add"),
    ("drinks_tip_1", "drinks_tip_1", "add"),
    ("drinks_tip_2", "drinks_tip_2", "add"),
    ("drinks_tip_3", "drinks_tip_3", "add"),
    ("vegetables_tip_1", "vegetables_tip_1", "add"),
    ("vegetables_tip_2", "vegetables_tip_2", "add"),
    ("vegetables_tip_3", "vegetables_tip_3", "add"),
    # 費用・割り勘
    ("plan_cost_total", "plan.cost.total", "add"),
    ("plan_cost_empty", "plan.cost.empty", "add"),
    ("plan_members_label", "plan.members.label", "add"),
    ("plan_members_add", "plan.members.add", "add"),
    ("plan_members_placeholder", "plan.members.placeholder", "add"),
    ("plan_members_count_format", "plan.members.count.format", "add"),
    ("plan_members_empty", "plan.members.empty", "add"),
    ("plan_expense_add", "plan.expense.add", "add"),
    ("plan_expense_edit", "plan.expense.edit", "add"),
    ("plan_expense_title_placeholder", "plan.expense.title.placeholder", "add"),
    ("plan_custom_title_label", "plan.custom.title.label", "add"),
    ("plan_cost_subtab_list", "plan.cost.subtab.list", "add"),
    ("plan_cost_subtab_settlement", "plan.cost.subtab.settlement", "add"),
    ("plan_split_payer_label", "plan.split.payer.label", "add"),
    ("plan_split_sharers_label", "plan.split.sharers.label", "add"),
    ("plan_split_perhead_format", "plan.split.perhead.format", "add"),
    ("plan_split_selectall", "plan.split.selectall", "add"),
    ("plan_split_selectnone", "plan.split.selectnone", "add"),
    ("plan_settlement_balances", "plan.settlement.balances", "add"),
    ("plan_settlement_transfers", "plan.settlement.transfers", "add"),
    ("plan_settlement_even", "plan.settlement.even", "add"),
    ("plan_settlement_empty", "plan.settlement.empty", "add"),
    # 県詳細のユーザーカード
    ("usercard_add", "usercard.add", "add"),
    ("usercard_add_short", "usercard.add.short", "add"),
    ("usercard_edit", "usercard.edit", "add"),
    ("usercard_delete", "usercard.delete", "add"),
    ("usercard_delete_confirm", "usercard.delete.confirm", "add"),
    ("usercard_menu", "usercard.menu", "add"),
    ("usercard_name_placeholder", "usercard.name.placeholder", "add"),
    ("usercard_description_placeholder", "usercard.description.placeholder", "add"),
    ("usercard_photo_add", "usercard.photo.add", "add"),
    ("usercard_photo_change", "usercard.photo.change", "add"),
    ("usercard_photo_remove", "usercard.photo.remove", "add"),
    ("usercard_url_label", "usercard.url.label", "add"),
    ("usercard_url_placeholder", "usercard.url.placeholder", "add"),
    ("usercard_url_invalid", "usercard.url.invalid", "add"),
    ("usercard_url_open", "usercard.url.open", "add"),
    ("usercard_location_clear", "plan.custom.location.clear", "add"),
]


def parse_strings(path):
    d = {}
    with open(path, encoding="utf-8") as f:
        content = f.read()
    for m in re.finditer(r'^\s*"((?:[^"\\]|\\.)*)"\s*=\s*"((?:[^"\\]|\\.)*)"\s*;', content, re.M):
        d[m.group(1)] = m.group(2)
    return d


def to_android(value):
    # iOS の書式 → Android（%@ → %s、%1$@ → %1$s）
    v = re.sub(r"%(\d+\$)?@", lambda m: "%" + (m.group(1) or "") + "s", value)
    # iOS のエスケープを解いてから XML / Android 用にエスケープし直す
    v = v.replace('\\"', '"').replace("\\n", "\n")
    v = v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    v = v.replace("\\", "\\\\").replace("'", "\\'").replace('"', '\\"')
    v = v.replace("\n", "\\n")
    if v.startswith("@") or v.startswith("?"):
        v = "\\" + v
    # 書式指定子が 2 つ以上で位置指定が無い場合は位置指定に直す（Android lint 対策）
    specs = re.findall(r"%(?!%)(\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[sdf]", v)
    if len(specs) >= 2 and not any(s for s in specs):
        idx = [0]

        def repl(m):
            idx[0] += 1
            return f"%{idx[0]}${m.group(1)}"
        v = re.sub(r"%(?!%)([-#+ 0,(]*\d*(?:\.\d+)?[sdf])", repl, v)
    return v


def main():
    missing = []
    for ios_lang, values_dir in LANGS:
        ios = parse_strings(f"{BASE}/{ios_lang}.lproj/Localizable.strings")
        ja = parse_strings(f"{BASE}/ja.lproj/Localizable.strings")
        en = parse_strings(f"{BASE}/en.lproj/Localizable.strings")
        path = f"{RES}/{values_dir}/strings.xml"
        xml = open(path, encoding="utf-8").read()
        added = updated = 0
        for akey, ikey, mode in ENTRIES:
            raw = ios.get(ikey)
            if raw is None:
                # 未訳は英語 → 日本語の順でフォールバック（iOS の挙動と同じ）
                raw = en.get(ikey) if ios_lang != "ja" else None
                raw = raw if raw is not None else ja.get(ikey)
                missing.append((ios_lang, ikey))
            if raw is None:
                print(f"!! iOS に {ikey} がありません", file=sys.stderr)
                continue
            value = to_android(raw)
            pattern = re.compile(r'(\s*<string name="' + re.escape(akey) + r'"[^>]*>)(.*?)(</string>)', re.S)
            m = pattern.search(xml)
            if m:
                if mode == "set" and m.group(2) != value:
                    xml = xml[:m.start(2)] + value + xml[m.end(2):]
                    updated += 1
                continue
            line = f'    <string name="{akey}">{value}</string>\n'
            xml = xml.replace("</resources>", line + "</resources>")
            added += 1
        open(path, "w", encoding="utf-8").write(xml)
        print(f"{values_dir}: +{added} / 更新 {updated}")
    if missing:
        print(f"iOS 側で未訳のため英語/日本語で補ったキー: {sorted(set(missing))}", file=sys.stderr)


if __name__ == "__main__":
    main()
