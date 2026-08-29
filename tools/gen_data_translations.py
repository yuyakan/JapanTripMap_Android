import re, glob, sys

# iOS 版 MapRoulette の Localizable.strings（全言語共通キー）から、
# 日本語データ文字列 → 各言語文字列 の対応表を生成し、DataTranslations.kt に書き出す。
#
# 仕組み:
#   1. ja.lproj のキー→日本語値 から「日本語値→キー群」を作る。
#   2. Kotlin コード中で実際に使われている日本語文字列だけを対象にする。
#   3. 各言語 lproj について、同じキーを引いて訳語を得る（訳が日本語と同一 or 空ならスキップ）。
#
# 生成先: app/src/main/java/com/example/japantripmap/DataTranslations.kt
# 手編集しないこと。翻訳を直したい場合は iOS の Localizable.strings を直すか、
# Localization.kt 側の TYPE_LABEL / INFO_LABEL（手管理表）で上書きする。

BASE = "/Users/uebetsunawayuuya/MapRoulette/MapRoulette/Base"
KDIR = "/Users/uebetsunawayuuya/AndroidStudioProjects/JapanTripMap/app/src/main/java/com/example/japantripmap"

# iOS 言語コード → Kotlin 側の識別子サフィックス（AppLocale と対応）。
LANGS = [
    ("en", "EN"),
    ("de", "DE"),
    ("es", "ES"),
    ("fr", "FR"),
    ("id", "ID"),
    ("ko", "KO"),
    ("th", "TH"),
    ("vi", "VI"),
    ("zh-Hans", "ZH_HANS"),
    ("zh-Hant", "ZH_HANT"),
    ("zh-HK", "ZH_HK"),
]


def parse_strings(path):
    d = {}
    with open(path, encoding="utf-8") as f:
        content = f.read()
    for m in re.finditer(r'"((?:[^"\\]|\\.)*)"\s*=\s*"((?:[^"\\]|\\.)*)"\s*;', content):
        key = m.group(1)
        val = m.group(2).replace('\\"', '"').replace('\\n', '\n')
        d[key] = val
    return d


ja = parse_strings(f"{BASE}/ja.lproj/Localizable.strings")

# 日本語値 → キー群
val2keys = {}
for k, v in ja.items():
    val2keys.setdefault(v, []).append(k)

# Kotlin コードで実際に使われている日本語文字列を収集
jp = re.compile(r"[ぁ-んァ-ヶ一-龠]")
strlit = re.compile(r'"((?:[^"\\]|\\.)*)"')
GENERATED = ("DataTranslations.kt", "LabelTranslations.kt")
used = set()
for path in glob.glob(f"{KDIR}/*.kt"):
    if path.endswith(GENERATED):
        continue
    with open(path, encoding="utf-8") as f:
        content = f.read()
    for m in strlit.finditer(content):
        s = m.group(1)
        if jp.search(s):
            used.add(s)

print(f"Kotlin 内の日本語文字列（ユニーク）: {len(used)}", file=sys.stderr)


# JVM の 64KB メソッドサイズ上限（MethodTooLargeException）を避けるため、
# hashMapOf(...) の巨大な static initializer は使わない。
# 各言語の対応表を「JA訳JA訳…」の 1 本の文字列定数として持ち、
# 実行時に split して Map に組み立てる（文字列定数はメソッドサイズに含まれない）。
UNIT = "\u0002"  # レコード区切り
KV = "\u0001"    # キー／値 区切り


def sesc(s):
    # Kotlin 文字列リテラル用。制御文字は \uXXXX にする（区切り文字と衝突させない）。
    out = []
    for ch in s:
        if ch == "\\":
            out.append("\\\\")
        elif ch == '"':
            out.append('\\"')
        elif ch == "$":
            out.append("\\$")
        elif ch == "\n":
            out.append("\\n")
        elif ch == "\r":
            out.append("\\r")
        elif ch == "\t":
            out.append("\\t")
        elif ord(ch) < 0x20:
            out.append(f"\\u{ord(ch):04x}")
        else:
            out.append(ch)
    return "".join(out)


lines = []
lines.append("package com.example.japantripmap")
lines.append("")
lines.append("/**")
lines.append(" * 日本語データ文字列 → 各言語文字列の対応表。")
lines.append(" *")
lines.append(" * iOS 版 MapRoulette の Localizable.strings（全言語で共通のローカライズキー）から、")
lines.append(" * 共通キーを介して機械生成したもの（手編集しないこと）。")
lines.append(" * [localizeData] が端末ロケールに応じて該当言語の表でデータ文字列を差し替える。")
lines.append(" *")
lines.append(" * JVM のメソッドサイズ上限を避けるため、各表は 1 本の文字列定数として保持し、")
lines.append(" * [decodeTable] で実行時に Map へ展開する（初回アクセス時に lazy 構築）。")
lines.append(" *")
lines.append(" * 生成元: tools/gen_data_translations.py")
lines.append(" */")
lines.append("")
lines.append("private const val KV = '\\u0001'")
lines.append("private const val UNIT = '\\u0002'")
lines.append("")
lines.append("private fun decodeTable(encoded: String): Map<String, String> {")
lines.append("    if (encoded.isEmpty()) return emptyMap()")
lines.append("    val parts = encoded.split(UNIT)")
lines.append("    val m = HashMap<String, String>(parts.size * 2)")
lines.append("    for (p in parts) {")
lines.append("        val i = p.indexOf(KV)")
lines.append("        if (i > 0) m[p.substring(0, i)] = p.substring(i + 1)")
lines.append("    }")
lines.append("    return m")
lines.append("}")
lines.append("")

# 1 チャンクの最大「文字数」。日本語などは 1 文字最大 3 バイトなので、
# CONSTANT_Utf8 の 65535 バイト上限に対し 18000 文字（最大 54KB）に抑える。
CHUNK_CHARS = 18000


def chunk_string(s):
    return [s[i:i + CHUNK_CHARS] for i in range(0, len(s), CHUNK_CHARS)]


per_lang_names = []
for lang, suffix in LANGS:
    L = parse_strings(f"{BASE}/{lang}.lproj/Localizable.strings")
    table = {}
    for s in sorted(used):
        keys = val2keys.get(s)
        if not keys:
            continue
        for k in keys:
            v = L.get(k)
            if v and v != s:
                table[s] = v
                break
    name = f"DATA_JA_TO_{suffix}"
    per_lang_names.append((suffix, name))
    print(f"  {lang:8s} -> {name}: {len(table)} entries", file=sys.stderr)
    encoded = UNIT.join(f"{s}{KV}{table[s]}" for s in sorted(table))
    chunks = chunk_string(encoded)
    # チャンクごとに append する関数（1 メソッド内の呼び出しも多くなりすぎないよう
    # 各 append は独立文で、文字列定数は個別に定数プールへ入る）。
    lines.append(f"private fun {name.lower()}Enc(): String {{")
    lines.append("    val sb = StringBuilder()")
    for c in chunks:
        lines.append(f'    sb.append("{sesc(c)}")')
    lines.append("    return sb.toString()")
    lines.append("}")
    lines.append(f"internal val {name}: Map<String, String> by lazy {{ decodeTable({name.lower()}Enc()) }}")
    lines.append("")

out = "\n".join(lines)
with open(f"{KDIR}/DataTranslations.kt", "w", encoding="utf-8") as f:
    f.write(out)
print(f"Wrote DataTranslations.kt with {len(per_lang_names)} language maps (encoded)", file=sys.stderr)
