package com.example.japantripmap

// 生成ファイル: tools/gen_other_festival_names.py（手編集しない）。

/** iOS OtherFestivalView（県詳細の「春・秋・冬の祭り」）に属する祭りの日本語名。 */
val OTHER_FESTIVAL_NAMES: Set<String> = setOf(
    "さっぽろ雪まつり",
    "函館五稜郭祭",
    "弘前さくらまつり",
    "八戸えんぶり",
    "北上展勝地さくらまつり",
    "一目千本桜まつり",
    "なまはげ柴灯まつり",
    "蔵王樹氷まつり",
    "大内宿雪まつり",
    "ひたち海浜公園ネモフィラまつり",
    "偕楽園梅まつり",
    "足利フラワーパーク藤まつり",
    "草津温泉感謝祭",
    "秩父夜祭",
    "佐倉チューリップフェスタ",
    "上野桜まつり",
    "明治神宮外苑いちょう祭り",
    "明治神宮初詣",
    "小田原梅まつり",
    "箱根駅伝",
    "高田城百万人観桜会",
    "となみチューリップフェア",
    "兼六園桜まつり",
    "富士芝桜まつり",
    "高遠城址公園桜まつり",
    "白川郷ライトアップ",
    "河津桜まつり",
    "熱海梅園梅まつり",
    "香嵐渓もみじまつり",
    "なばなの里イルミネーション",
    "葵祭",
    "京都花灯路",
    "造幣局桜の通り抜け",
    "神戸ルミナリエ",
    "吉野山桜まつり",
    "若草山山焼き",
    "南部梅林",
    "松江城桜まつり",
    "岡山後楽園桜まつり",
    "宮島桜まつり",
    "宮島紅葉まつり",
    "錦帯橋桜まつり",
    "勝浦町ビッグひな祭り",
    "栗林公園桜まつり",
    "松山城桜まつり",
    "高知城桜まつり",
    "福岡城桜まつり",
    "有田陶器市",
    "ハウステンボス花まつり",
    "ハウステンボス光の王国",
    "熊本城桜まつり",
    "高千穂夜神楽",
    "琉球海炎祭",
    "プロ野球キャンプ",
)

/** 四季の祭りにしか無いカテゴリ（traditional は夏祭り側にもあるので含めない）。 */
private val SEASONAL_ONLY_CATEGORIES = setOf("spring", "autumn", "winter", "sakura", "illumination", "snow")

/**
 * 県詳細の 2 セクションへの振り分け: first = 夏祭り・花火大会（iOS FestivalView）、
 * second = 春・秋・冬の祭り（iOS OtherFestivalView）。
 * 同じ名前が両方にある祭り（例: 秩父夜祭）は、四季専用カテゴリの方を四季側にする。
 */
fun Prefecture.splitFestivals(): Pair<List<Festival>, List<Festival>> {
    val all = festivals
    val seasonal = all.filter { f ->
        f.name in OTHER_FESTIVAL_NAMES &&
            (all.count { it.name == f.name } == 1 || f.category in SEASONAL_ONLY_CATEGORIES)
    }
    return all.filterNot { it in seasonal } to seasonal
}
