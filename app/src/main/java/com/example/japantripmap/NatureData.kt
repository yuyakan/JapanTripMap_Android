package com.example.japantripmap

/** 自然スポット。iOS 版 NatureSpotMapView.swift を移植（日本語解決済み）。 */
data class NatureSpot(
    val name: String,
    val description: String,
    /** タイプ: night_view/starry_sky/sea/camping */
    val type: String,
    val popularity: Int,
    val latitude: Double,
    val longitude: Double,
)

/** 都道府県 -> 自然スポット（ある県のみ）。 */
val NATURE_INFO: Map<Prefecture, List<NatureSpot>> = mapOf(
    Prefecture.HOKKAIDO to listOf(
        NatureSpot("函館山", "100万ドルの夜景で有名な北海道の代表的な夜景スポット", "night_view", 5, 41.7637, 140.7108),
        NatureSpot("藻岩山", "札幌市街の夜景を一望できる北海道の夜景スポット", "night_view", 4, 43.0239, 141.3239),
        NatureSpot("大雪山", "北海道の最高峰での雄大な星空体験", "starry_sky", 5, 43.6639, 142.8539),
        NatureSpot("津別峠", "北海道屈指の星空観測地", "starry_sky", 4, 43.6139, 144.1339),
        NatureSpot("襟裳岬", "北海道の最南端での美しい星空観測スポット", "starry_sky", 4, 41.9239, 143.2539),
        NatureSpot("洞爺湖畔キャンプ場", "北海道の温泉湖でのキャンプ", "camping", 5, 42.5968, 140.7524),
        NatureSpot("支笏湖畔キャンプ場", "北海道の透明度の高い湖でのキャンプ", "camping", 4, 42.7439, 141.3739),
        NatureSpot("摩周湖畔キャンプ場", "北海道の美しい湖でのキャンプ", "camping", 5, 43.5739, 144.5639),
        NatureSpot("富良野・美瑛エリアキャンプ場", "北海道の花畑と丘陵地帯でのキャンプ", "camping", 5, 43.3542, 142.3834),
        NatureSpot("ニセコ周辺キャンプ場", "北海道のリゾート地でのキャンプ", "camping", 4, 42.8048, 140.6874),
        NatureSpot("襟裳岬", "北海道の最南端での雄大な海景", "sea", 4, 41.9239, 143.2539),
        NatureSpot("積丹半島・神威岬", "北海道の美しい海岸線と絶景岬", "sea", 5, 43.3339, 140.6139),
        NatureSpot("知床半島", "世界自然遺産の雄大な海岸線", "sea", 5, 44.0739, 145.1139),
    ),
    Prefecture.AOMORI to listOf(
        NatureSpot("十和田湖畔キャンプ場", "青森県・秋田県の美しい湖畔でのキャンプ", "camping", 5, 40.4439, 140.9239),
        NatureSpot("種差海岸", "青森県の美しい海岸線", "sea", 4, 40.5039, 141.5639),
    ),
    Prefecture.IWATE to listOf(
        NatureSpot("三陸海岸", "岩手県・宮城県の美しいリアス式海岸", "sea", 4, 39.6439, 141.9439),
    ),
    Prefecture.MIYAGI to listOf(
        NatureSpot("松島", "宮城県の日本三景の一つ", "sea", 5, 38.3739, 141.0639),
    ),
    Prefecture.YAMAGATA to listOf(
        NatureSpot("蔵王", "山形県・宮城県の境界にある星空観測スポット", "starry_sky", 4, 38.1439, 140.4439),
        NatureSpot("蔵王坊平キャンプ場", "山形県・宮城県の山岳地帯でのキャンプ", "camping", 4, 38.1439, 140.4439),
    ),
    Prefecture.FUKUSHIMA to listOf(
        NatureSpot("磐梯高原キャンプ場", "福島県の高原でのキャンプ", "camping", 4, 37.6639, 140.0839),
        NatureSpot("裏磐梯五色沼キャンプ場", "福島県の神秘的な沼群でのキャンプ", "camping", 4, 37.6500, 140.0667),
    ),
    Prefecture.CHIBA to listOf(
        NatureSpot("九十九里浜", "千葉県の長大な砂浜海岸", "sea", 3, 35.5539, 140.4239),
    ),
    Prefecture.TOCHIGI to listOf(
        NatureSpot("那須高原キャンプ場", "栃木県の高原リゾートでのキャンプ", "camping", 4, 37.1439, 140.0239),
        NatureSpot("奥日光キャンプ場", "栃木県の中禅寺湖周辺でのキャンプ", "camping", 4, 36.7239, 139.4939),
    ),
    Prefecture.GUNMA to listOf(
        NatureSpot("赤城山", "群馬県の山頂での星空観測地", "starry_sky", 4, 36.5439, 139.1939),
    ),
    Prefecture.TOKYO to listOf(
        NatureSpot("東京スカイツリー・東京タワー", "東京を代表する2つのタワーからの絶景夜景", "night_view", 5, 35.7101, 139.8107),
        NatureSpot("奥多摩キャンプ場群", "東京都の自然豊かなキャンプ地", "camping", 4, 35.8039, 139.0539),
    ),
    Prefecture.KANAGAWA to listOf(
        NatureSpot("湘南平", "湘南海岸を一望できる神奈川県の夜景スポット", "night_view", 4, 35.3139, 139.4819),
        NatureSpot("みなとみらい21", "横浜の代表的な夜景エリア、観覧車とビル群の美景", "night_view", 4, 35.4537, 139.6317),
        NatureSpot("丹沢湖キャンプ場", "神奈川県の山間部と湖でのキャンプ", "camping", 4, 35.4639, 139.1639),
        NatureSpot("箱根キャンプ場", "神奈川県の温泉リゾートでのキャンプ", "camping", 4, 35.2043, 139.0235),
        NatureSpot("湘南海岸", "神奈川県の代表的な海岸リゾート", "sea", 4, 35.3139, 139.4819),
        NatureSpot("城ヶ島", "神奈川県三浦半島の景勝地", "sea", 3, 35.1339, 139.6139),
        NatureSpot("江の島", "湘南を代表する海のリゾート", "sea", 4, 35.2989, 139.4819),
    ),
    Prefecture.NAGANO to listOf(
        NatureSpot("阿智村", "日本一の星空の村として認定された長野県の星空観測地", "starry_sky", 5, 35.4539, 137.6539),
        NatureSpot("野辺山高原", "標高1300mの高原での美しい星空観測地", "starry_sky", 4, 35.9239, 138.4739),
        NatureSpot("美ヶ原高原", "八ヶ岳連峰を望む絶好の星空観測地", "starry_sky", 5, 36.2019, 138.1819),
        NatureSpot("八ヶ岳", "長野県・山梨県にまたがる星空観測の名所", "starry_sky", 5, 35.9139, 138.3439),
        NatureSpot("霧ヶ峰高原", "長野県の高原地帯での美しい星空スポット", "starry_sky", 4, 36.1039, 138.1639),
        NatureSpot("上高地キャンプ場", "北アルプスの玄関口でのキャンプ", "camping", 5, 36.2539, 137.6339),
        NatureSpot("白馬岩岳キャンプ場", "長野県の北アルプスでのキャンプ", "camping", 4, 36.7039, 137.8639),
        NatureSpot("軽井沢キャンプ場", "長野県の避暑地でのキャンプ", "camping", 4, 36.3439, 138.6239),
        NatureSpot("志賀高原キャンプ場", "長野県の高原リゾートでのキャンプ", "camping", 4, 36.7439, 138.5139),
    ),
    Prefecture.YAMANASHI to listOf(
        NatureSpot("富士五湖キャンプ場群", "富士山を望む絶好のキャンプエリア", "camping", 5, 35.4739, 138.7339),
        NatureSpot("清里高原キャンプ場", "山梨県の八ヶ岳南麓でのキャンプ", "camping", 4, 35.9239, 138.4439),
    ),
    Prefecture.SHIZUOKA to listOf(
        NatureSpot("伊豆・白浜海岸", "静岡県の美しい白砂のビーチ", "sea", 4, 34.6639, 138.9439),
        NatureSpot("熱海海岸", "静岡県の温泉リゾート海岸", "sea", 3, 35.0939, 139.0639),
    ),
    Prefecture.MIE to listOf(
        NatureSpot("伊勢志摩", "三重県の美しい海岸とリアス式地形", "sea", 4, 34.3239, 136.8239),
    ),
    Prefecture.ISHIKAWA to listOf(
        NatureSpot("能登半島・千里浜なぎさドライブウェイ", "車で走れる日本唯一の砂浜", "sea", 4, 36.8839, 136.6439),
    ),
    Prefecture.SHIGA to listOf(
        NatureSpot("琵琶湖畔キャンプ場", "滋賀県の日本最大の湖でのキャンプ", "camping", 4, 35.3717, 136.1069),
    ),
    Prefecture.KYOTO to listOf(
        NatureSpot("天橋立", "京都府の日本三景の一つ", "sea", 5, 35.5739, 135.1939),
    ),
    Prefecture.HYOGO to listOf(
        NatureSpot("摩耶山掬星台", "神戸の街並みと大阪湾を一望できる関西屈指の夜景スポット", "night_view", 5, 34.7622, 135.2358),
        NatureSpot("六甲山", "1000万ドルの夜景と称される関西随一の夜景スポット", "night_view", 5, 34.7622, 135.2358),
        NatureSpot("竹野海岸", "兵庫県の美しい海岸線", "sea", 4, 35.6339, 134.7239),
    ),
    Prefecture.NARA to listOf(
        NatureSpot("若草山", "奈良盆地の夜景を一望できる古都の夜景スポット", "night_view", 4, 34.6839, 135.8539),
        NatureSpot("大台ヶ原", "近畿地方最高の星空観測地として知られる奈良県の高原", "starry_sky", 4, 34.1739, 136.1039),
        NatureSpot("大台ヶ原キャンプ場", "奈良県・三重県の山岳地帯でのキャンプ", "camping", 4, 34.1739, 136.1039),
    ),
    Prefecture.WAKAYAMA to listOf(
        NatureSpot("白良浜", "和歌山県の白砂の美しいビーチ", "sea", 4, 33.6859, 135.3439),
    ),
    Prefecture.OSAKA to listOf(
        NatureSpot("天保山", "大阪港の夜景を楽しめるベイエリアスポット", "night_view", 3, 34.6539, 135.4339),
    ),
    Prefecture.TOTTORI to listOf(
        NatureSpot("鳥取砂丘", "日本最大級の砂丘と日本海の絶景", "sea", 4, 35.5439, 134.2339),
    ),
    Prefecture.OKAYAMA to listOf(
        NatureSpot("蒜山高原キャンプ場", "岡山県の高原でのキャンプ体験", "camping", 4, 35.3139, 133.6639),
    ),
    Prefecture.YAMAGUCHI to listOf(
        NatureSpot("角島大橋", "エメラルドグリーンの海に架かる絶景の橋", "sea", 5, 34.4139, 130.8739),
    ),
    Prefecture.TOKUSHIMA to listOf(
        NatureSpot("剣山キャンプ場", "徳島県の四国第二の高峰でのキャンプ", "camping", 4, 33.8739, 134.1139),
        NatureSpot("鳴門海峡", "徳島県の渦潮で有名な海峡", "sea", 4, 34.2339, 134.6139),
    ),
    Prefecture.KOCHI to listOf(
        NatureSpot("四万十川キャンプ場", "高知県の清流四万十川でのキャンプ", "camping", 4, 33.2339, 133.2139),
        NatureSpot("桂浜", "高知県の月の名所として知られる美しい海岸", "sea", 4, 33.4939, 133.5739),
        NatureSpot("室戸岬", "高知県の太平洋に突き出した岬", "sea", 4, 33.2439, 134.1639),
        NatureSpot("足摺岬", "高知県最南端の絶景岬", "sea", 4, 32.7239, 133.0139),
    ),
    Prefecture.FUKUOKA to listOf(
        NatureSpot("皿倉山", "新日本三大夜景のひとつ、北九州市の夜景を一望", "night_view", 4, 33.8183, 130.6883),
        NatureSpot("宗像・沖ノ島", "福岡県の世界遺産の島", "sea", 4, 34.2439, 130.1039),
    ),
    Prefecture.OITA to listOf(
        NatureSpot("久住高原", "九州の大分県にある高原での星空観測地", "starry_sky", 4, 33.0839, 131.2539),
        NatureSpot("くじゅう高原キャンプ場", "大分県の九重連山でのキャンプ", "camping", 4, 33.0839, 131.2539),
    ),
    Prefecture.MIYAZAKI to listOf(
        NatureSpot("日南海岸", "宮崎県の美しい海岸線", "sea", 4, 31.5739, 131.4239),
    ),
    Prefecture.KAGOSHIMA to listOf(
        NatureSpot("城山公園", "桜島と鹿児島市街を望む南九州の夜景名所", "night_view", 4, 31.5839, 130.5539),
        NatureSpot("霧島高原キャンプ場", "鹿児島県・宮崎県の火山群でのキャンプ", "camping", 4, 31.9300, 130.8642),
        NatureSpot("屋久島キャンプ場", "鹿児島県の世界遺産の島でのキャンプ", "camping", 5, 30.3000, 130.5000),
    ),
    Prefecture.KUMAMOTO to listOf(
        NatureSpot("阿蘇草千里キャンプ場", "熊本県の雄大なカルデラでのキャンプ", "camping", 5, 32.8847, 131.1040),
        NatureSpot("天草", "熊本県の美しい島々", "sea", 4, 32.4539, 130.1939),
    ),
    Prefecture.NAGASAKI to listOf(
        NatureSpot("稲佐山", "世界新三大夜景に選ばれた長崎の美しい夜景", "night_view", 5, 32.7645, 129.8608),
        NatureSpot("壱岐・対馬", "長崎県の歴史ある離島", "sea", 4, 34.1439, 129.2839),
    ),
    Prefecture.OKINAWA to listOf(
        NatureSpot("石垣島", "南十字星も見える沖縄県の星空観測スポット", "starry_sky", 5, 24.3439, 124.1569),
        NatureSpot("西表島", "手つかずの自然に囲まれた沖縄県の星空スポット", "starry_sky", 5, 24.3239, 123.7969),
        NatureSpot("やんばるキャンプ場（沖縄）", "沖縄県北部の亜熱帯の森でのキャンプ", "camping", 4, 26.7439, 128.2439),
        NatureSpot("宮古島・伊良部大橋", "透明度抜群の青い海と絶景の橋", "sea", 5, 24.8059, 125.2819),
        NatureSpot("石垣島・川平湾", "エメラルドグリーンの美しい湾", "sea", 5, 24.4239, 124.1569),
        NatureSpot("青の洞窟・真栄田岬", "幻想的な青い光に包まれる洞窟", "sea", 4, 26.3939, 127.8169),
        NatureSpot("残波岬", "沖縄本島の絶景岬", "sea", 4, 26.4339, 127.7439),
        NatureSpot("万座毛", "沖縄本島の象の鼻の形をした絶景岬", "sea", 4, 26.4939, 127.8539),
        NatureSpot("辺戸岬", "沖縄本島最北端の岬", "sea", 4, 26.8639, 128.2539),
    ),
)
