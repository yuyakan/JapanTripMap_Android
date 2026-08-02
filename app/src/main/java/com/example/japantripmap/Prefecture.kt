package com.example.japantripmap

import androidx.compose.ui.geometry.Offset

/**
 * 都道府県。
 *
 * iOS 版 MapRoulette の Prefecture.swift を移植したもの。
 * 座標は 500x500 の仮想座標系で定義されており、描画時に画面サイズへ正規化する。
 */
enum class Prefecture(
    val displayName: String,
    val regionKey: String,
    /** 500x500 座標系での輪郭ポリゴン。 */
    val points: List<Offset>,
) {
    // 座標データは PrefectureData に集約している。
    HOKKAIDO("北海道", "hokkaido", PrefectureData.HOKKAIDO),
    AOMORI("青森県", "tohoku", PrefectureData.AOMORI),
    IWATE("岩手県", "tohoku", PrefectureData.IWATE),
    AKITA("秋田県", "tohoku", PrefectureData.AKITA),
    MIYAGI("宮城県", "tohoku", PrefectureData.MIYAGI),
    YAMAGATA("山形県", "tohoku", PrefectureData.YAMAGATA),
    FUKUSHIMA("福島県", "tohoku", PrefectureData.FUKUSHIMA),
    IBARAKI("茨城県", "kanto", PrefectureData.IBARAKI),
    CHIBA("千葉県", "kanto", PrefectureData.CHIBA),
    TOCHIGI("栃木県", "kanto", PrefectureData.TOCHIGI),
    GUNMA("群馬県", "kanto", PrefectureData.GUNMA),
    SAITAMA("埼玉県", "kanto", PrefectureData.SAITAMA),
    TOKYO("東京都", "kanto", PrefectureData.TOKYO),
    KANAGAWA("神奈川県", "kanto", PrefectureData.KANAGAWA),
    NIIGATA("新潟県", "koshinetsu", PrefectureData.NIIGATA),
    NAGANO("長野県", "koshinetsu", PrefectureData.NAGANO),
    YAMANASHI("山梨県", "koshinetsu", PrefectureData.YAMANASHI),
    SHIZUOKA("静岡県", "tokai", PrefectureData.SHIZUOKA),
    AICHI("愛知県", "tokai", PrefectureData.AICHI),
    MIE("三重県", "tokai", PrefectureData.MIE),
    GIFU("岐阜県", "tokai", PrefectureData.GIFU),
    FUKUI("福井県", "hokuriku", PrefectureData.FUKUI),
    ISHIKAWA("石川県", "hokuriku", PrefectureData.ISHIKAWA),
    TOYAMA("富山県", "hokuriku", PrefectureData.TOYAMA),
    SHIGA("滋賀県", "kinki", PrefectureData.SHIGA),
    KYOTO("京都府", "kinki", PrefectureData.KYOTO),
    HYOGO("兵庫県", "kinki", PrefectureData.HYOGO),
    NARA("奈良県", "kinki", PrefectureData.NARA),
    WAKAYAMA("和歌山県", "kinki", PrefectureData.WAKAYAMA),
    OSAKA("大阪府", "kinki", PrefectureData.OSAKA),
    TOTTORI("鳥取県", "chugoku", PrefectureData.TOTTORI),
    OKAYAMA("岡山県", "chugoku", PrefectureData.OKAYAMA),
    HIROSHIMA("広島県", "chugoku", PrefectureData.HIROSHIMA),
    YAMAGUCHI("山口県", "chugoku", PrefectureData.YAMAGUCHI),
    SHIMANE("島根県", "chugoku", PrefectureData.SHIMANE),
    KAGAWA("香川県", "shikoku", PrefectureData.KAGAWA),
    TOKUSHIMA("徳島県", "shikoku", PrefectureData.TOKUSHIMA),
    KOCHI("高知県", "shikoku", PrefectureData.KOCHI),
    EHIME("愛媛県", "shikoku", PrefectureData.EHIME),
    FUKUOKA("福岡県", "kyushu", PrefectureData.FUKUOKA),
    OITA("大分県", "kyushu", PrefectureData.OITA),
    MIYAZAKI("宮崎県", "kyushu", PrefectureData.MIYAZAKI),
    KAGOSHIMA("鹿児島県", "kyushu", PrefectureData.KAGOSHIMA),
    KUMAMOTO("熊本県", "kyushu", PrefectureData.KUMAMOTO),
    SAGA("佐賀県", "kyushu", PrefectureData.SAGA),
    NAGASAKI("長崎県", "kyushu", PrefectureData.NAGASAKI),
    OKINAWA("沖縄県", "okinawa", PrefectureData.OKINAWA);

    val regionName: String get() = REGION_NAMES[regionKey] ?: regionKey

    /** 食べログのエリアスラッグ等に使う英語スラッグ（iOS 版 rawValue 相当。例: "mie"）。 */
    val slug: String get() = name.lowercase()

    /** 沖縄だけ、本島から離れていることを示す点線を引く。 */
    val okinawaLinePoints: List<Offset>
        get() = if (this == OKINAWA) {
            listOf(Offset(21f, 456f), Offset(62f, 456f), Offset(62f, 496f))
        } else {
            emptyList()
        }

    companion object {
        /** 500x500 の仮想座標系の一辺。描画時の正規化に使う。 */
        const val MAP_SIZE = 500f

        private val REGION_NAMES = mapOf(
            "hokkaido" to "北海道",
            "tohoku" to "東北",
            "kanto" to "関東",
            "koshinetsu" to "甲信越",
            "tokai" to "東海",
            "hokuriku" to "北陸",
            "kinki" to "近畿",
            "chugoku" to "中国",
            "shikoku" to "四国",
            "kyushu" to "九州",
            "okinawa" to "沖縄",
        )

        /** 設定画面などで使う地方の並び順。 */
        val REGION_ORDER = listOf(
            "hokkaido", "tohoku", "kanto", "koshinetsu", "tokai",
            "hokuriku", "kinki", "chugoku", "shikoku", "kyushu", "okinawa",
        )

        /** regionKey に対応する地方名を返す。 */
        fun regionNameFor(regionKey: String): String = REGION_NAMES[regionKey] ?: regionKey

        /**
         * 都道府県を地方ごとにグループ化して、REGION_ORDER の順で返す。
         * 設定画面などで「地方見出し + その地方の県」を並べるのに使う。
         * @param among このリストに含まれる県だけを対象にする（既定は全県）。
         */
        fun groupedByRegion(among: List<Prefecture> = entries): List<Pair<String, List<Prefecture>>> {
            val byRegion = among.groupBy { it.regionKey }
            return REGION_ORDER.mapNotNull { key ->
                val list = byRegion[key]
                if (list.isNullOrEmpty()) null else regionNameFor(key) to list
            }
        }
    }
}

/** 観光情報（観光スポット・グルメ・お土産）へのアクセサ。 */
val Prefecture.tourismInfo: TourismInfo?
    get() = TOURISM_INFO[this]

val Prefecture.gourmets: List<Gourmet>
    get() = GOURMET_INFO[this] ?: emptyList()

val Prefecture.souvenirs: List<Souvenir>
    get() = SOUVENIR_INFO[this] ?: emptyList()

val Prefecture.onsens: List<Onsen>
    get() = ONSEN_INFO[this] ?: emptyList()

val Prefecture.hasOnsen: Boolean
    get() = onsens.isNotEmpty()

/** 温泉がある都道府県のみ（設定・ルーレット対象に使う）。 */
val PREFECTURES_WITH_ONSEN: List<Prefecture>
    get() = Prefecture.entries.filter { it.hasOnsen }

/** この県が持つ温泉タイプ集合。 */
val Prefecture.onsenTypes: Set<String>
    get() = onsens.map { it.type }.toSet()

val Prefecture.natureSpots: List<NatureSpot>
    get() = NATURE_INFO[this] ?: emptyList()

/** この県が持つ自然スポットのタイプ集合。 */
val Prefecture.natureTypes: Set<String>
    get() = natureSpots.map { it.type }.toSet()

val Prefecture.hasNatureSpot: Boolean
    get() = natureSpots.isNotEmpty()

/** 自然スポットがある都道府県のみ。 */
val PREFECTURES_WITH_NATURE: List<Prefecture>
    get() = Prefecture.entries.filter { it.hasNatureSpot }

val Prefecture.festivals: List<Festival>
    get() = FESTIVAL_INFO[this] ?: emptyList()

/**
 * 都道府県の輪郭ポリゴン座標。
 *
 * iOS 版 Prefecture.swift の CGPoint 配列をそのまま移植している。
 */
private object PrefectureData {
    private fun o(x: Float, y: Float) = Offset(x, y)

    val HOKKAIDO = listOf(o(382f, 8f), o(390f, 15f), o(398f, 25f), o(406f, 35f), o(414f, 45f), o(425f, 52f), o(437f, 58f), o(450f, 64f), o(458f, 57f), o(466f, 51f), o(464f, 60f), o(461f, 69f), o(463f, 76f), o(466f, 83f), o(472f, 82f), o(479f, 81f), o(475f, 88f), o(465f, 92f), o(450f, 96f), o(442f, 96f), o(434f, 96f), o(426f, 104f), o(419f, 112f), o(416f, 121f), o(413f, 131f), o(394f, 118f), o(384f, 111f), o(374f, 105f), o(365f, 111f), o(356f, 117f), o(352f, 110f), o(348f, 104f), o(348f, 107f), o(343f, 113f), o(338f, 119f), o(341f, 120f), o(345f, 121f), o(352f, 127f), o(360f, 134f), o(358f, 136f), o(356f, 139f), o(353f, 137f), o(350f, 136f), o(348f, 138f), o(346f, 140f), o(340f, 144f), o(334f, 149f), o(335f, 138f), o(336f, 127f), o(331f, 123f), o(327f, 119f), o(329f, 112f), o(331f, 106f), o(338f, 100f), o(345f, 95f), o(343f, 89f), o(342f, 83f), o(344f, 82f), o(347f, 81f), o(358f, 83f), o(369f, 85f), o(367f, 76f), o(365f, 68f), o(368f, 61f), o(372f, 55f), o(375f, 45f), o(378f, 36f), o(375f, 24f), o(372f, 13f), o(375f, 13f), o(378f, 14f))
    val AOMORI = listOf(o(356f, 145f), o(362f, 151f), o(371f, 150f), o(369f, 172f), o(373f, 179f), o(357f, 184f), o(358f, 182f), o(352f, 178f), o(345f, 181f), o(332f, 180f), o(331f, 172f), o(339f, 168f), o(341f, 154f), o(344f, 155f), o(349f, 154f), o(351f, 168f), o(353f, 165f), o(355f, 162f), o(362f, 166f), o(363f, 153f), o(352f, 156f))
    val IWATE = listOf(o(357f, 184f), o(373f, 179f), o(379f, 184f), o(384f, 206f), o(378f, 225f), o(374f, 230f), o(370f, 232f), o(364f, 232f), o(360f, 231f), o(353f, 231f), o(349f, 214f), o(353f, 200f))
    val AKITA = listOf(o(332f, 180f), o(345f, 181f), o(352f, 178f), o(358f, 182f), o(357f, 184f), o(353f, 200f), o(349f, 214f), o(353f, 231f), o(346f, 231f), o(341f, 225f), o(328f, 225f), o(334f, 207f), o(332f, 198f), o(325f, 200f), o(323f, 196f), o(329f, 193f))
    val MIYAGI = listOf(o(374f, 232f), o(368f, 236f), o(369f, 251f), o(363f, 247f), o(359f, 249f), o(356f, 253f), o(356f, 263f), o(351f, 267f), o(340f, 261f), o(340f, 258f), o(346f, 249f), o(346f, 239f), o(348f, 235f), o(346f, 231f), o(360f, 231f), o(364f, 232f))
    val YAMAGATA = listOf(o(328f, 225f), o(341f, 225f), o(346f, 231f), o(348f, 235f), o(346f, 239f), o(346f, 249f), o(340f, 258f), o(340f, 261f), o(338f, 267f), o(329f, 266f), o(326f, 266f), o(325f, 261f), o(328f, 249f), o(322f, 242f), o(328f, 230f))
    val FUKUSHIMA = listOf(o(356f, 263f), o(358f, 273f), o(355f, 292f), o(347f, 296f), o(346f, 297f), o(340f, 296f), o(337f, 294f), o(337f, 290f), o(329f, 286f), o(321f, 292f), o(313f, 292f), o(312f, 277f), o(321f, 275f), o(326f, 266f), o(329f, 266f), o(338f, 267f), o(340f, 261f), o(351f, 267f))
    val IBARAKI = listOf(o(340f, 296f), o(346f, 297f), o(347f, 296f), o(355f, 292f), o(345f, 315f), o(353f, 331f), o(344f, 325f), o(338f, 327f), o(326f, 320f), o(323f, 317f), o(329f, 312f), o(338f, 308f))
    val CHIBA = listOf(o(326f, 320f), o(338f, 327f), o(344f, 325f), o(353f, 331f), o(343f, 335f), o(343f, 347f), o(327f, 355f), o(327f, 343f), o(334f, 338f), o(333f, 333f), o(329f, 334f), o(328f, 327f))
    val TOCHIGI = listOf(o(321f, 292f), o(329f, 286f), o(337f, 290f), o(337f, 294f), o(340f, 296f), o(338f, 308f), o(329f, 312f), o(323f, 317f), o(321f, 317f), o(317f, 311f), o(318f, 304f), o(314f, 302f))
    val GUNMA = listOf(o(300f, 321f), o(297f, 319f), o(298f, 309f), o(292f, 306f), o(303f, 297f), o(308f, 291f), o(313f, 292f), o(321f, 292f), o(314f, 302f), o(318f, 304f), o(317f, 311f), o(321f, 317f), o(311f, 313f), o(308f, 317f))
    val SAITAMA = listOf(o(300f, 321f), o(308f, 317f), o(311f, 313f), o(321f, 317f), o(323f, 317f), o(326f, 320f), o(328f, 327f), o(317f, 329f), o(310f, 326f), o(306f, 327f), o(297f, 323f))
    val TOKYO = listOf(o(306f, 327f), o(310f, 326f), o(317f, 329f), o(328f, 327f), o(329f, 334f), o(327f, 335f), o(320f, 334f), o(320f, 335f), o(312f, 332f))
    val KANAGAWA = listOf(o(312f, 332f), o(320f, 335f), o(320f, 334f), o(327f, 335f), o(324f, 342f), o(326f, 346f), o(322f, 349f), o(321f, 345f), o(312f, 345f), o(308f, 350f), o(308f, 343f), o(305f, 341f), o(310f, 334f))
    val NIIGATA = listOf(o(322f, 242f), o(328f, 249f), o(325f, 261f), o(326f, 266f), o(321f, 275f), o(312f, 277f), o(313f, 292f), o(308f, 291f), o(303f, 297f), o(292f, 306f), o(295f, 292f), o(285f, 296f), o(280f, 294f), o(276f, 297f), o(274f, 291f), o(292f, 282f), o(302f, 266f), o(313f, 259f))
    val NAGANO = listOf(o(273f, 309f), o(276f, 297f), o(280f, 294f), o(285f, 296f), o(295f, 292f), o(292f, 306f), o(298f, 309f), o(297f, 319f), o(300f, 321f), o(297f, 323f), o(292f, 324f), o(288f, 326f), o(288f, 336f), o(284f, 340f), o(276f, 347f), o(270f, 345f), o(269f, 333f), o(265f, 326f), o(271f, 323f), o(272f, 310f))
    val YAMANASHI = listOf(o(288f, 336f), o(288f, 326f), o(292f, 324f), o(297f, 323f), o(306f, 327f), o(312f, 332f), o(310f, 334f), o(297f, 342f), o(294f, 345f), o(292f, 347f), o(289f, 341f))
    val SHIZUOKA = listOf(o(310f, 334f), o(305f, 341f), o(308f, 343f), o(308f, 350f), o(311f, 355f), o(304f, 365f), o(300f, 362f), o(300f, 354f), o(305f, 351f), o(297f, 349f), o(284f, 365f), o(270f, 364f), o(268f, 358f), o(276f, 347f), o(284f, 340f), o(288f, 336f), o(289f, 341f), o(292f, 347f), o(294f, 345f), o(297f, 342f))
    val AICHI = listOf(o(270f, 345f), o(276f, 347f), o(268f, 358f), o(270f, 364f), o(258f, 366f), o(265f, 361f), o(259f, 360f), o(256f, 359f), o(257f, 363f), o(252f, 360f), o(253f, 353f), o(250f, 351f), o(249f, 347f), o(251f, 342f), o(255f, 341f), o(262f, 344f))
    val MIE = listOf(o(249f, 347f), o(250f, 351f), o(244f, 363f), o(254f, 369f), o(253f, 375f), o(240f, 378f), o(239f, 383f), o(231f, 391f), o(228f, 387f), o(234f, 381f), o(233f, 372f), o(237f, 370f), o(232f, 361f), o(234f, 360f), o(244f, 347f), o(242f, 346f))
    val GIFU = listOf(o(272f, 310f), o(271f, 323f), o(265f, 326f), o(269f, 333f), o(270f, 345f), o(262f, 344f), o(255f, 341f), o(251f, 342f), o(249f, 347f), o(242f, 346f), o(239f, 333f), o(242f, 330f), o(251f, 327f), o(249f, 321f), o(250f, 317f), o(252f, 310f), o(258f, 313f), o(261f, 309f))
    val FUKUI = listOf(o(238f, 313f), o(244f, 317f), o(250f, 317f), o(249f, 321f), o(251f, 327f), o(242f, 330f), o(239f, 333f), o(228f, 338f), o(225f, 343f), o(222f, 343f), o(215f, 339f), o(222f, 337f), o(233f, 330f), o(228f, 323f))
    val ISHIKAWA = listOf(o(238f, 313f), o(255f, 290f), o(249f, 282f), o(265f, 274f), o(264f, 281f), o(258f, 284f), o(258f, 291f), o(254f, 296f), o(252f, 310f), o(250f, 317f), o(244f, 317f))
    val TOYAMA = listOf(o(258f, 291f), o(258f, 297f), o(266f, 298f), o(266f, 293f), o(274f, 291f), o(276f, 297f), o(272f, 310f), o(261f, 309f), o(258f, 313f), o(252f, 310f), o(254f, 296f))
    val SHIGA = listOf(o(225f, 343f), o(228f, 338f), o(239f, 333f), o(242f, 346f), o(244f, 347f), o(234f, 360f), o(229f, 353f))
    val KYOTO = listOf(o(215f, 339f), o(222f, 343f), o(225f, 343f), o(229f, 353f), o(234f, 360f), o(232f, 361f), o(225f, 360f), o(225f, 355f), o(217f, 349f), o(204f, 342f), o(208f, 337f), o(203f, 334f), o(211f, 329f), o(215f, 332f), o(214f, 336f))
    val HYOGO = listOf(o(190f, 334f), o(203f, 334f), o(208f, 337f), o(204f, 342f), o(217f, 349f), o(225f, 355f), o(218f, 362f), o(212f, 362f), o(205f, 365f), o(198f, 360f), o(186f, 360f), o(192f, 345f), o(194f, 343f))
    val NARA = listOf(o(232f, 361f), o(237f, 370f), o(233f, 372f), o(234f, 381f), o(228f, 387f), o(225f, 387f), o(220f, 381f), o(225f, 376f), o(225f, 372f), o(225f, 360f))
    val WAKAYAMA = listOf(o(211f, 372f), o(225f, 372f), o(225f, 376f), o(220f, 381f), o(225f, 387f), o(228f, 387f), o(231f, 391f), o(225f, 401f), o(216f, 397f), o(217f, 390f), o(209f, 385f), o(209f, 374f))
    val OSAKA = listOf(o(225f, 355f), o(225f, 360f), o(225f, 372f), o(211f, 372f), o(217f, 366f), o(218f, 362f))
    val TOTTORI = listOf(o(165f, 340f), o(170f, 337f), o(182f, 338f), o(190f, 334f), o(194f, 343f), o(192f, 345f), o(185f, 346f), o(178f, 345f), o(170f, 343f), o(164f, 351f), o(159f, 349f), o(164f, 342f))
    val OKAYAMA = listOf(o(192f, 345f), o(186f, 360f), o(177f, 370f), o(169f, 368f), o(165f, 358f), o(164f, 351f), o(170f, 343f), o(178f, 345f), o(185f, 346f))
    val HIROSHIMA = listOf(o(159f, 349f), o(164f, 351f), o(165f, 358f), o(169f, 368f), o(166f, 369f), o(145f, 377f), o(142f, 371f), o(139f, 377f), o(136f, 374f), o(133f, 370f), o(138f, 360f), o(147f, 357f), o(152f, 350f))
    val YAMAGUCHI = listOf(o(133f, 370f), o(136f, 374f), o(139f, 377f), o(138f, 376f), o(137f, 383f), o(131f, 388f), o(124f, 381f), o(112f, 385f), o(104f, 384f), o(106f, 370f), o(114f, 372f), o(123f, 363f), o(123f, 369f), o(129f, 374f))
    val SHIMANE = listOf(o(123f, 363f), o(147f, 343f), o(163f, 335f), o(164f, 338f), o(165f, 340f), o(164f, 342f), o(159f, 349f), o(152f, 350f), o(147f, 357f), o(138f, 360f), o(133f, 370f), o(129f, 374f), o(123f, 369f))
    val KAGAWA = listOf(o(172f, 382f), o(171f, 375f), o(184f, 373f), o(187f, 375f), o(194f, 377f), o(182f, 380f), o(174f, 384f))
    val TOKUSHIMA = listOf(o(194f, 377f), o(197f, 377f), o(200f, 389f), o(189f, 397f), o(188f, 398f), o(181f, 390f), o(173f, 388f), o(174f, 384f), o(182f, 380f))
    val KOCHI = listOf(o(173f, 388f), o(181f, 390f), o(188f, 398f), o(186f, 406f), o(178f, 399f), o(167f, 400f), o(151f, 421f), o(146f, 415f), o(154f, 400f), o(162f, 390f))
    val EHIME = listOf(o(172f, 382f), o(174f, 384f), o(173f, 388f), o(162f, 390f), o(154f, 400f), o(146f, 415f), o(143f, 417f), o(143f, 404f), o(132f, 403f), o(148f, 393f), o(155f, 380f), o(158f, 385f))
    val FUKUOKA = listOf(o(111f, 397f), o(103f, 401f), o(104f, 410f), o(93f, 413f), o(91f, 411f), o(88f, 407f), o(95f, 401f), o(83f, 398f), o(103f, 385f), o(105f, 391f))
    val OITA = listOf(o(111f, 397f), o(122f, 393f), o(127f, 398f), o(118f, 405f), o(130f, 407f), o(133f, 416f), o(129f, 421f), o(114f, 418f), o(110f, 409f), o(108f, 408f), o(108f, 412f), o(104f, 410f), o(103f, 401f))
    val MIYAZAKI = listOf(o(114f, 418f), o(129f, 421f), o(124f, 425f), o(119f, 437f), o(114f, 462f), o(110f, 457f), o(111f, 455f), o(104f, 450f), o(100f, 442f), o(110f, 436f), o(107f, 428f))
    val KAGOSHIMA = listOf(o(100f, 442f), o(104f, 450f), o(111f, 455f), o(110f, 457f), o(98f, 472f), o(98f, 456f), o(101f, 456f), o(101f, 450f), o(97f, 450f), o(94f, 462f), o(98f, 467f), o(83f, 462f), o(90f, 458f), o(85f, 450f), o(84f, 436f), o(88f, 437f), o(95f, 437f))
    val KUMAMOTO = listOf(o(93f, 413f), o(104f, 410f), o(108f, 412f), o(108f, 408f), o(110f, 409f), o(114f, 418f), o(107f, 428f), o(110f, 436f), o(100f, 442f), o(95f, 437f), o(88f, 437f), o(89f, 428f), o(95f, 420f), o(96f, 414f))
    val SAGA = listOf(o(76f, 402f), o(78f, 397f), o(83f, 398f), o(95f, 401f), o(88f, 407f), o(85f, 415f))
    val NAGASAKI = listOf(o(76f, 402f), o(85f, 415f), o(83f, 418f), o(83f, 419f), o(85f, 420f), o(91f, 421f), o(86f, 426f), o(85f, 422f), o(76f, 425f), o(76f, 420f), o(72f, 410f), o(69f, 406f), o(72f, 402f))
    val OKINAWA = listOf(o(52f, 469f), o(52f, 473f), o(38f, 481f), o(36f, 490f), o(32f, 491f), o(31f, 489f), o(34f, 481f), o(42f, 477f), o(39f, 474f), o(39f, 473f), o(42f, 472f), o(43f, 475f), o(50f, 468f))
}
