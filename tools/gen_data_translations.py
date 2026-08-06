import re, json, glob, sys

with open("/private/tmp/claude-501/-Users-uebetsunawayuuya-AndroidStudioProjects-JapanTripMap/0dc91cd7-5430-4083-b07e-a37e9208352a/scratchpad/ja2en.json",encoding='utf-8') as f:
    ja2en = json.load(f)

DIR="/Users/uebetsunawayuuya/AndroidStudioProjects/JapanTripMap/app/src/main/java/com/example/japantripmap"
# Collect ALL Japanese string literals across the whole codebase (data + any UI we might miss),
# but for the data table we only need what data files use. Scan all .kt for max reuse.
jp = re.compile(r'[ぁ-んァ-ヶ一-龠]')
strlit = re.compile(r'"((?:[^"\\]|\\.)*)"')

used = set()
for path in glob.glob(f"{DIR}/*.kt"):
    with open(path, encoding='utf-8') as f:
        content = f.read()
    for m in strlit.finditer(content):
        s = m.group(1)
        if jp.search(s):
            used.add(s)

# Only keep mappings for strings actually used, and where EN differs from JA
table = {}
for s in sorted(used):
    if s in ja2en:
        en = ja2en[s]
        if en and en != s:
            table[s] = en

print(f"Used JP strings in codebase: {len(used)}, table entries: {len(table)}", file=sys.stderr)

def kesc(s):
    return (s.replace('\\','\\\\').replace('"','\\"')
             .replace('\n','\\n').replace('\r','\\r').replace('\t','\\t').replace('$','\\$'))

lines = []
lines.append("package com.example.japantripmap")
lines.append("")
lines.append("/**")
lines.append(" * 日本語データ文字列 → 英語文字列の対応表。")
lines.append(" *")
lines.append(" * iOS 版 MapRoulette の ja.lproj / en.lproj の Localizable.strings から、")
lines.append(" * 共通ローカライズキーを介して機械的に生成したもの（手編集しないこと）。")
lines.append(" * 端末ロケールが英語のとき、[localizeData] がこの表でデータ文字列を英訳に差し替える。")
lines.append(" *")
lines.append(" * 生成元: tools/gen_data_translations.py")
lines.append(" */")
lines.append("internal val DATA_JA_TO_EN: Map<String, String> = hashMapOf(")
for s in sorted(table):
    lines.append(f'    "{kesc(s)}" to "{kesc(table[s])}",')
lines.append(")")
lines.append("")

out = "\n".join(lines)
with open(f"{DIR}/DataTranslations.kt","w",encoding='utf-8') as f:
    f.write(out)
print(f"Wrote DataTranslations.kt with {len(table)} entries", file=sys.stderr)
