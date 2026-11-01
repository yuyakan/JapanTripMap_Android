#!/usr/bin/env python3
"""
iOS の OtherFestivalView.swift（四季の祭り＝県詳細「春・秋・冬の祭り」セクション）に載っている
祭りの日本語名一覧を Kotlin に書き出す。

Android の FestivalData.kt は iOS の FestivalView（夏祭り・花火）と OtherFestivalView（四季の祭り）を
1 つのリストに統合しているため、県詳細で 2 セクションに分けるにはどちらの出自かを名前で判定する
（カテゴリ "traditional" が両方にあり、カテゴリだけでは分けられない）。

生成先: app/src/main/java/com/example/japantripmap/OtherFestivalNames.kt
"""
import os
import re

IOS = "/Users/uebetsunawayuuya/MapRoulette/MapRoulette"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

ja = {}
for line in open(f"{IOS}/Base/ja.lproj/Localizable.strings", encoding="utf-8"):
    m = re.match(r'\s*"((?:[^"\\]|\\.)+)"\s*=\s*"((?:[^"\\]|\\.)*)"\s*;', line)
    if m:
        ja[m.group(1)] = m.group(2)

src = open(f"{IOS}/Views/Tourism/OtherFestivalView.swift", encoding="utf-8").read()
body = src[src.index("var otherFestivalItems"):]
names = []
for m in re.finditer(r'OtherFestivalItem\(\s*name:\s*NSLocalizedString\("([^"]+)"', body):
    name = ja.get(m.group(1))
    if name and name not in names:
        names.append(name)

lines = ["package com.example.japantripmap", "",
         "// 生成ファイル: tools/gen_other_festival_names.py（手編集しない）。", "",
         "/** iOS OtherFestivalView（県詳細の「春・秋・冬の祭り」）に属する祭りの日本語名。 */",
         "val OTHER_FESTIVAL_NAMES: Set<String> = setOf("]
lines += [f'    "{n}",' for n in names]
lines += [")", "",
          "/** 四季の祭りにしか無いカテゴリ（traditional は夏祭り側にもあるので含めない）。 */",
          'private val SEASONAL_ONLY_CATEGORIES = setOf("spring", "autumn", "winter", "sakura", "illumination", "snow")',
          "",
          "/**",
          " * 県詳細の 2 セクションへの振り分け: first = 夏祭り・花火大会（iOS FestivalView）、",
          " * second = 春・秋・冬の祭り（iOS OtherFestivalView）。",
          " * 同じ名前が両方にある祭り（例: 秩父夜祭）は、四季専用カテゴリの方を四季側にする。",
          " */",
          "fun Prefecture.splitFestivals(): Pair<List<Festival>, List<Festival>> {",
          "    val all = festivals",
          "    val seasonal = all.filter { f ->",
          "        f.name in OTHER_FESTIVAL_NAMES &&",
          "            (all.count { it.name == f.name } == 1 || f.category in SEASONAL_ONLY_CATEGORIES)",
          "    }",
          "    return all.filterNot { it in seasonal } to seasonal",
          "}", ""]
open(f"{ROOT}/app/src/main/java/com/example/japantripmap/OtherFestivalNames.kt", "w", encoding="utf-8").write("\n".join(lines))
print(len(names), "件")
