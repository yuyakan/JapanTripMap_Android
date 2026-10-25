#!/usr/bin/env python3
"""
iOS 版 MapRoulette の同梱写真（Assets.xcassets の Attractions / Gourmet / Nature / Festivals）を
Android の res/drawable-nodpi へ WebP で取り込み、日本語名 → drawable 名の対応表と
写真クレジットを Kotlin に生成する。

- 画像は 1080px 幅に縮小して JPEG(q=62) で再圧縮する（APK サイズ対策。iOS 原本は 1600px）。
  変換は macOS 標準の sips を使う（追加ツール不要）。
  drawable-nodpi に置くのは、端末密度による拡大デコード（xxhdpi で 3 倍）を避けるため。
- 対応表のキーは Android データ（Kotlin 直書きの日本語名）。iOS のローカライズキーを
  ja.lproj で日本語に解決して作る。
- 写真クレジットは iOS の Settings.bundle/PhotoCredits.plist（photo_credits.json から
  gen_credits.py で生成された全文）をそのまま使う。

生成物:
  app/src/main/res/drawable-nodpi/*.jpg
  app/src/main/java/com/example/japantripmap/AttractionPhotoData.kt
  app/src/main/java/com/example/japantripmap/PhotoCreditsData.kt

使い方: python3 tools/import_ios_photos.py
"""
import glob
import os
import plistlib
import re
import subprocess
import sys

IOS = "/Users/uebetsunawayuuya/MapRoulette/MapRoulette"
ASSETS = f"{IOS}/Assets.xcassets"
JA = f"{IOS}/Base/ja.lproj/Localizable.strings"
PLIST = f"{IOS}/Settings.bundle/PhotoCredits.plist"

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = f"{ROOT}/app/src/main/res"
KDIR = f"{ROOT}/app/src/main/java/com/example/japantripmap"
OUT_DIR = f"{RES}/drawable-nodpi"

MAX_WIDTH = 1080
QUALITY = 62


def load_ja():
    names = {}
    for line in open(JA, encoding="utf-8"):
        m = re.match(r'\s*"((?:[^"\\]|\\.)+)"\s*=\s*"((?:[^"\\]|\\.)*)"\s*;', line)
        if m:
            names[m.group(1)] = m.group(2).replace('\\"', '"')
    return names


def image_of(imageset):
    for ext in ("jpg", "jpeg", "png", "JPG"):
        files = glob.glob(f"{imageset}/*.{ext}")
        if files:
            return files[0]
    return None


def convert(src, name):
    dst = f"{OUT_DIR}/{name}.jpg"
    if os.path.exists(dst) and os.path.getmtime(dst) >= os.path.getmtime(src):
        return
    width = int(re.search(r"pixelWidth: (\d+)",
                          subprocess.check_output(["sips", "-g", "pixelWidth", src], text=True)).group(1))
    cmd = ["sips", "-s", "format", "jpeg", "-s", "formatOptions", str(QUALITY)]
    if width > MAX_WIDTH:
        cmd += ["--resampleWidth", str(MAX_WIDTH)]
    subprocess.check_call(cmd + [src, "--out", dst], stdout=subprocess.DEVNULL)


def kotlin_data_text():
    text = ""
    for f in glob.glob(f"{KDIR}/*.kt"):
        if f.endswith(("DataTranslations.kt", "LabelTranslations.kt", "AttractionPhotoData.kt",
                       "PhotoCreditsData.kt")):
            continue
        text += open(f, encoding="utf-8").read()
    return text


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    ja = load_ja()
    data_text = kotlin_data_text()

    maps = {"ATTRACTION": {}, "BEACH": {}, "GOURMET": {}, "NATURE": {}, "FESTIVAL": {}}
    unresolved = []
    not_in_data = []

    def add(kind, ja_name, res_name):
        if ja_name in maps[kind] and maps[kind][ja_name] != res_name:
            print(f"  ! 重複 {kind} {ja_name}: {maps[kind][ja_name]} / {res_name}", file=sys.stderr)
            return
        maps[kind][ja_name] = res_name
        if f'"{ja_name}"' not in data_text:
            not_in_data.append((kind, ja_name, res_name))

    for folder in ("Attractions", "Gourmet", "Nature", "Festivals"):
        for imageset in sorted(glob.glob(f"{ASSETS}/{folder}/*.imageset")):
            name = os.path.basename(imageset)[: -len(".imageset")]
            src = image_of(imageset)
            if src is None:
                continue
            convert(src, name)
            old = f"{RES}/drawable/{name}.jpg"
            if os.path.exists(old):
                os.remove(old)  # 旧配置（drawable/ 原寸 JPEG）は nodpi 版に置き換える

            if name.startswith("attraction_"):
                key, kind = name, "ATTRACTION"
            elif name.startswith("beach_"):
                key, kind = "beach." + name[len("beach_"):], "BEACH"
            elif name.startswith("gourmet_"):
                key, kind = name, "GOURMET"
            elif name.startswith("nature_"):
                key, kind = name[len("nature_"):] + "_name", "NATURE"
            elif name.startswith("festival_"):
                key, kind = name[len("festival_"):] + "_name", "FESTIVAL"
            else:
                continue
            if key not in ja:
                unresolved.append((name, key))
                continue
            add(kind, ja[key], name)

    # 取り込み対象外になった（iOS で削除された）古い jpg を掃除する。
    for old in glob.glob(f"{RES}/drawable/attraction_*.jpg"):
        name = os.path.basename(old)[:-4]
        if not os.path.exists(f"{OUT_DIR}/{name}.jpg"):
            print(f"  - 削除（iOS に無い）: {name}", file=sys.stderr)
            os.remove(old)

    write_photo_data(maps)
    write_credits()

    for kind, m in maps.items():
        print(f"{kind}: {len(m)} 件")
    if unresolved:
        print("ja.lproj で解決できなかった imageset:", unresolved, file=sys.stderr)
    if not_in_data:
        print("Android データに名前が見つからない写真:", file=sys.stderr)
        for row in not_in_data:
            print("   ", row, file=sys.stderr)


def kt_map(name, doc, m):
    lines = [f"/** {doc} */", f"val {name}: Map<String, String> = mapOf("]
    for k in sorted(m):
        lines.append(f'    "{k}" to "{m[k]}",')
    lines.append(")")
    return "\n".join(lines)


def write_photo_data(maps):
    header = '''package com.example.japantripmap

// 生成ファイル: tools/import_ios_photos.py（手編集しない）。
//
// 日本語名 -> drawable リソース名（拡張子なし）の対応表。
// iOS 版 MapRoulette の Assets.xcassets（Attractions / Gourmet / Nature / Festivals）を移植。
// 画像は res/drawable-nodpi/*.jpg。CC0 / パブリックドメインと CC BY が混在し、
// CC BY の帰属は [PHOTO_CREDITS_TEXT]（[PhotoCreditsScreen]）で表示している。
'''
    body = "\n\n".join([
        kt_map("ATTRACTION_PHOTOS", "観光スポット名 -> 写真。iOS AttractionPhoto。", maps["ATTRACTION"]),
        kt_map("BEACH_PHOTOS", "ビーチ名（ホームの海テーマ・自然の海スポット） -> 写真。iOS BeachPhoto。", maps["BEACH"]),
        kt_map("GOURMET_PHOTOS", "グルメ名 -> 写真。iOS GourmetPhoto。", maps["GOURMET"]),
        kt_map("NATURE_PHOTOS", "自然スポット名 -> 写真。iOS NaturePhoto。", maps["NATURE"]),
        kt_map("FESTIVAL_PHOTOS", "祭り名 -> 写真。iOS FestivalPhoto。", maps["FESTIVAL"]),
    ])
    with open(f"{KDIR}/AttractionPhotoData.kt", "w", encoding="utf-8") as f:
        f.write(header + "\n" + body + "\n")


def write_credits():
    plist = plistlib.load(open(PLIST, "rb"))
    footer = plist["PreferenceSpecifiers"][0]["FooterText"]
    # 1 つの文字列リテラルは UTF-8 で 65,535 バイトまで（クラスファイルの定数プール上限）。
    # 全文はそれを超えるので、行単位で 15,000 文字程度ずつに分けて連結する。
    chunks, cur = [], ""
    for line in footer.split("\n"):
        piece = line + "\n"
        if len(cur) + len(piece) > 15000 and cur:
            chunks.append(cur)
            cur = ""
        cur += piece
    if cur:
        chunks.append(cur.rstrip("\n"))
    literals = ",\n".join('    """' + c.replace("$", "${'$'}") + '"""' for c in chunks)
    content = f'''package com.example.japantripmap

// 生成ファイル: tools/import_ios_photos.py（手編集しない）。

/**
 * 写真（観光スポット・グルメ・自然スポット・祭り）の帰属情報（フォトクレジット）。
 *
 * iOS 版 Settings.bundle/PhotoCredits.plist の全文をそのまま移植。
 * CC BY ライセンスの作品は帰属表示が法的に必須なので、この文言は削除・改変しないこと。
 * 文字列リテラルの長さ上限を避けるため、複数のリテラルに分けて連結している。
 */
val PHOTO_CREDITS_TEXT: String by lazy {{
    listOf(
{literals},
    ).joinToString("")
}}
'''
    with open(f"{KDIR}/PhotoCreditsData.kt", "w", encoding="utf-8") as f:
        f.write(content)


if __name__ == "__main__":
    main()
