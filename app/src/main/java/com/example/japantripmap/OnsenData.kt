package com.example.japantripmap

/** 温泉地。iOS 版 OnsenItem.swift を移植（日本語解決済み）。 */
data class Onsen(
    val name: String,
    val description: String,
    /** 温泉タイプ: scenic/historical/therapeutic/resort/mountain/seaside/ski */
    val type: String,
    val popularity: Int,
    val latitude: Double,
    val longitude: Double,
)

/** 都道府県 -> 温泉地リスト（温泉がある県のみ）。 */
val ONSEN_INFO: Map<Prefecture, List<Onsen>> = mapOf(
    Prefecture.HOKKAIDO to listOf(
        Onsen("登別温泉", "地獄谷で有名な硫黄泉の名湯", "therapeutic", 5, 42.4919, 141.1539),
        Onsen("洞爺湖温泉", "美しい湖畔の温泉リゾート", "scenic", 4, 42.5968, 140.7524),
        Onsen("定山渓温泉", "札幌の奥座敷として親しまれる", "mountain", 4, 42.9621, 141.1621),
        Onsen("函館湯の川", "函館観光の拠点にもなる温泉地", "resort", 3, 41.7774, 140.7886),
    ),
    Prefecture.IWATE to listOf(
        Onsen("花巻温泉", "宮沢賢治ゆかりの地", "historical", 3, 39.3778, 141.1048),
    ),
    Prefecture.AKITA to listOf(
        Onsen("乳頭温泉郷", "秘湯ムード満点の山間の温泉郷", "mountain", 5, 39.7372, 140.7372),
    ),
    Prefecture.MIYAGI to listOf(
        Onsen("鳴子温泉", "紅葉の名所としても知られる", "scenic", 4, 38.7299, 140.7299),
    ),
    Prefecture.YAMAGATA to listOf(
        Onsen("銀山温泉", "大正ロマンあふれるノスタルジックな温泉街", "historical", 5, 38.5398, 140.5098),
        Onsen("蔵王温泉", "スキーでも有名な高原温泉", "ski", 4, 38.1495, 140.4495),
    ),
    Prefecture.FUKUSHIMA to listOf(
        Onsen("飯坂温泉", "松尾芭蕉も訪れた歴史ある温泉", "historical", 3, 37.8267, 140.4267),
    ),
    Prefecture.GUNMA to listOf(
        Onsen("草津温泉", "「恋の病以外何でも治る」と言われる強酸性の湯で有名", "therapeutic", 5, 36.6228, 138.5989),
        Onsen("伊香保温泉", "石段街で知られる情緒ある温泉街", "historical", 4, 36.4895, 138.9095),
    ),
    Prefecture.KANAGAWA to listOf(
        Onsen("箱根温泉", "都心からのアクセスが良く、芦ノ湖や富士山の景色も楽しめる", "scenic", 5, 35.2043, 139.0235),
    ),
    Prefecture.NAGANO to listOf(
        Onsen("野沢温泉", "スキーと外湯めぐりで有名", "ski", 4, 36.9144, 138.4444),
        Onsen("上諏訪温泉", "諏訪湖畔の温泉地", "scenic", 3, 36.0461, 138.1161),
    ),
    Prefecture.YAMANASHI to listOf(
        Onsen("石和温泉", "ぶどう狩りと温泉が楽しめる", "resort", 3, 35.6536, 138.6336),
    ),
    Prefecture.SHIZUOKA to listOf(
        Onsen("熱海温泉", "首都圏から近い海辺のリゾート温泉", "seaside", 4, 35.1042, 139.0731),
        Onsen("修善寺温泉", "伊豆半島の山間にある文学の里", "historical", 3, 34.9679, 138.9279),
        Onsen("熱川温泉", "伊豆東海岸の海を望む温泉", "seaside", 3, 34.8164, 139.0764),
    ),
    Prefecture.GIFU to listOf(
        Onsen("下呂温泉", "日本三名泉の一つとして知られる美肌の湯", "therapeutic", 5, 35.8080, 137.2480),
    ),
    Prefecture.ISHIKAWA to listOf(
        Onsen("山中温泉", "加賀温泉郷の一つで、松尾芭蕉も愛した温泉地", "historical", 4, 36.2889, 136.3689),
        Onsen("和倉温泉", "能登半島の海辺の温泉", "seaside", 4, 37.1169, 136.9269),
        Onsen("山代温泉", "加賀温泉郷の中心的存在", "resort", 3, 36.2981, 136.3681),
        Onsen("片山津温泉", "柴山潟を望む湖畔の温泉", "scenic", 3, 36.3200, 136.3500),
    ),
    Prefecture.TOYAMA to listOf(
        Onsen("宇奈月温泉", "黒部峡谷の玄関口", "scenic", 4, 36.8033, 137.5833),
    ),
    Prefecture.HYOGO to listOf(
        Onsen("有馬温泉", "日本最古の温泉の一つで、金泉・銀泉で有名", "historical", 5, 34.7974, 135.2574),
        Onsen("城崎温泉", "7つの外湯めぐりで有名", "historical", 4, 35.6076, 134.8076),
        Onsen("湯村温泉", "山陰の小京都と呼ばれる", "historical", 3, 35.4883, 134.6183),
    ),
    Prefecture.WAKAYAMA to listOf(
        Onsen("白浜温泉", "海沿いの温泉地で、パンダで有名なアドベンチャーワールドも近い", "seaside", 4, 33.6886, 135.3386),
        Onsen("勝浦温泉", "洞窟風呂で知られる海辺の温泉", "seaside", 3, 33.6767, 135.8867),
    ),
    Prefecture.TOTTORI to listOf(
        Onsen("三朝温泉", "ラドン温泉で有名", "therapeutic", 4, 35.4036, 133.8936),
    ),
    Prefecture.SHIMANE to listOf(
        Onsen("玉造温泉", "美肌の湯として女性に人気", "therapeutic", 4, 35.4230, 132.8730),
    ),
    Prefecture.EHIME to listOf(
        Onsen("道後温泉", "日本最古級の温泉で夏目漱石の『坊っちゃん』の舞台", "historical", 5, 33.8518, 132.7818),
    ),
    Prefecture.OITA to listOf(
        Onsen("別府温泉", "源泉数日本一を誇る温泉王国", "therapeutic", 5, 33.2695, 131.4895),
        Onsen("湯布院温泉", "由布岳を望む落ち着いた雰囲気の高原リゾート", "resort", 5, 33.2667, 131.3567),
    ),
    Prefecture.KAGOSHIMA to listOf(
        Onsen("指宿温泉", "砂蒸し風呂で有名な南国の温泉地", "resort", 4, 31.2513, 130.6413),
    ),
    Prefecture.KUMAMOTO to listOf(
        Onsen("黒川温泉", "自然に溶け込んだ露天風呂が人気", "mountain", 5, 33.0600, 131.1000),
    ),
    Prefecture.SAGA to listOf(
        Onsen("嬉野温泉", "美肌効果の高いぬめりのある湯", "therapeutic", 3, 33.1056, 129.9956),
        Onsen("武雄温泉", "竜宮城を思わせる楼門で有名", "historical", 3, 33.1936, 129.9936),
    ),
    Prefecture.NAGASAKI to listOf(
        Onsen("雲仙温泉", "雲仙岳の中腹にある高原温泉", "mountain", 4, 32.7600, 130.2900),
    ),
)
