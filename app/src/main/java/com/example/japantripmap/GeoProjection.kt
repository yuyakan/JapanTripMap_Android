package com.example.japantripmap

import androidx.compose.ui.geometry.Offset

/**
 * 自作日本地図（500x500 座標系）へのスポット配置ロジック。
 *
 * iOS 版 OnsenMapView / NatureSpotMapView はスポット名ごとに 500x500 座標を手打ちして
 * マーカーを置いていた。Android の県ポリゴンは iOS と同じ座標系を移植しているため、
 * この手打ち座標をそのまま使えば iOS と同じ「正しい位置」にアイコンが載る。
 *
 * よって温泉・自然スポットは名前で SPOT_MAP_POSITION を引く。手打ち座標が無い名前だけ、
 * 緯度経度からの線形式＋県ポリゴンへのクランプにフォールバックする。
 * 沖縄だけは本島から離れて描かれるため専用式を使う。
 */
object GeoProjection {
    // 緯度経度→500x500 座標の線形フィット係数（手打ち座標が無いスポット用のフォールバック）。
    // x = A*lng + B, y = C*lat + D
    private const val A = 24.71355
    private const val B = -3127.70861
    private const val C = -31.07672
    private const val D = 1442.12781

    // 沖縄スポットの緯度経度レンジ（実データの bbox に少し余裕を持たせた値）。
    private const val OKI_LAT_MIN = 24.0
    private const val OKI_LAT_MAX = 27.0
    private const val OKI_LNG_MIN = 123.5
    private const val OKI_LNG_MAX = 128.5
    // 沖縄本島まわりの表示領域（点線で示される位置の近く）。
    private const val OKI_X_MIN = 22f
    private const val OKI_X_MAX = 64f
    private const val OKI_Y_MIN = 458f
    private const val OKI_Y_MAX = 492f

    // マーカーの縦位置の微調整（500x500 座標系, プラスで下へ）。
    // Android では地図全体が iOS よりやや上寄りに見えるため、手打ち座標を一律で下げる。
    private const val MAP_Y_SHIFT = 15f
    // 沖縄は本島から離れた専用枠に描かれ、アイコンが島の形より上に浮いて見えるため
    // 島の上へ少しだけ下げる。下端(y=500)で見切れないよう本州より小さめの値にする。
    private const val OKINAWA_Y_SHIFT = 8f

    /**
     * スポットを 500x500 座標へ変換する。
     * @param name スポット名。SPOT_MAP_POSITION にあれば iOS と同じ手打ち座標を返す。
     * @param prefecture スポットが属する県。手打ち座標が無い場合の沖縄専用式／クランプに使う。
     */
    fun project(name: String, latitude: Double, longitude: Double, prefecture: Prefecture): Offset {
        // iOS と同じ手打ち座標があれば最優先で使う（温泉・自然タブ）。
        // 本州側と沖縄で下げ幅を変え、どちらも島／県の形にアイコンが乗るようにする。
        SPOT_MAP_POSITION[name]?.let { pos ->
            val shift = if (prefecture == Prefecture.OKINAWA) OKINAWA_Y_SHIFT else MAP_Y_SHIFT
            return pos.copy(y = pos.y + shift)
        }

        if (prefecture == Prefecture.OKINAWA) {
            return projectOkinawa(latitude, longitude)
        }
        val raw = Offset(
            x = (A * longitude + B).toFloat(),
            y = (C * latitude + D).toFloat(),
        )
        // 線形式で県ポリゴンの外に出る岬・湖畔などは、県内へ引き戻して自県の近くに収める。
        return clampToPrefecture(raw, prefecture)
    }

    private fun projectOkinawa(latitude: Double, longitude: Double): Offset {
        val tx = ((longitude - OKI_LNG_MIN) / (OKI_LNG_MAX - OKI_LNG_MIN))
            .coerceIn(0.0, 1.0)
        // 緯度が高い（北）ほど y は小さい。
        val ty = ((OKI_LAT_MAX - latitude) / (OKI_LAT_MAX - OKI_LAT_MIN))
            .coerceIn(0.0, 1.0)
        return Offset(
            x = OKI_X_MIN + (OKI_X_MAX - OKI_X_MIN) * tx.toFloat(),
            y = OKI_Y_MIN + (OKI_Y_MAX - OKI_Y_MIN) * ty.toFloat(),
        )
    }

    /** 点が県ポリゴン外なら、重心方向へ寄せてポリゴン内に収める。 */
    private fun clampToPrefecture(point: Offset, prefecture: Prefecture): Offset {
        val poly = prefecture.points
        if (poly.size < 3) return point
        if (pointInPolygon(point.x, point.y, poly)) return point

        val centroid = centroidOf(poly)
        // 重心→点 の線分を二分探索して、ポリゴン内に入る最も外側の点を採用する。
        var inside = centroid
        var outside = point
        repeat(12) {
            val mid = Offset((inside.x + outside.x) / 2f, (inside.y + outside.y) / 2f)
            if (pointInPolygon(mid.x, mid.y, poly)) inside = mid else outside = mid
        }
        return inside
    }

    private fun centroidOf(poly: List<Offset>): Offset {
        var sx = 0f
        var sy = 0f
        for (p in poly) {
            sx += p.x
            sy += p.y
        }
        return Offset(sx / poly.size, sy / poly.size)
    }

    /** レイキャスティング法。座標は 500x500 系。 */
    private fun pointInPolygon(px: Float, py: Float, poly: List<Offset>): Boolean {
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val xi = poly[i].x
            val yi = poly[i].y
            val xj = poly[j].x
            val yj = poly[j].y
            if (((yi > py) != (yj > py)) &&
                (px < (xj - xi) * (py - yi) / (yj - yi) + xi)
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    /**
     * スポット名 → 500x500 座標。iOS 版 OnsenMapView.getOnsenPosition /
     * NatureSpotMapView.getSpotPosition の手打ち座標をそのまま移植。
     * ここに載っているスポットは iOS と同じ位置にマーカーが置かれる。
     */
    private val SPOT_MAP_POSITION: Map<String, Offset> = mapOf(
        // ===== 温泉（iOS OnsenMapView） =====
        // 北海道
        "登別温泉" to Offset(365f, 102f),
        "洞爺湖温泉" to Offset(355f, 105f),
        "定山渓温泉" to Offset(360f, 90f),
        "函館湯の川" to Offset(350f, 130f),
        // 東北
        "乳頭温泉郷" to Offset(340f, 200f),
        "銀山温泉" to Offset(335f, 240f),
        "蔵王温泉" to Offset(330f, 250f),
        "花巻温泉" to Offset(365f, 210f),
        "鳴子温泉" to Offset(355f, 245f),
        "飯坂温泉" to Offset(345f, 280f),
        // 関東・甲信越・東海
        "箱根温泉" to Offset(315f, 340f),
        "草津温泉" to Offset(300f, 310f),
        "伊香保温泉" to Offset(305f, 305f),
        "熱海温泉" to Offset(305f, 350f),
        "修善寺温泉" to Offset(290f, 355f),
        "熱川温泉" to Offset(295f, 360f),
        "石和温泉" to Offset(290f, 335f),
        "野沢温泉" to Offset(280f, 320f),
        "上諏訪温泉" to Offset(275f, 330f),
        "下呂温泉" to Offset(255f, 335f),
        "宇奈月温泉" to Offset(265f, 290f),
        // 北陸
        "山中温泉" to Offset(245f, 305f),
        "和倉温泉" to Offset(250f, 285f),
        "山代温泉" to Offset(248f, 300f),
        "片山津温泉" to Offset(252f, 295f),
        // 関西
        "有馬温泉" to Offset(200f, 350f),
        "城崎温泉" to Offset(185f, 340f),
        "湯村温泉" to Offset(180f, 335f),
        "白浜温泉" to Offset(220f, 390f),
        "勝浦温泉" to Offset(235f, 385f),
        // 中国
        "三朝温泉" to Offset(175f, 345f),
        "玉造温泉" to Offset(155f, 350f),
        // 四国
        "道後温泉" to Offset(160f, 390f),
        // 九州
        "別府温泉" to Offset(120f, 405f),
        "湯布院温泉" to Offset(115f, 400f),
        "指宿温泉" to Offset(95f, 450f),
        "黒川温泉" to Offset(100f, 420f),
        "雲仙温泉" to Offset(80f, 415f),
        "嬉野温泉" to Offset(82f, 408f),
        "武雄温泉" to Offset(85f, 405f),

        // ===== 自然スポット（iOS NatureSpotMapView） =====
        // 北海道
        "函館山" to Offset(350f, 130f),
        "藻岩山" to Offset(360f, 90f),
        "大雪山" to Offset(400f, 73f),
        "津別峠" to Offset(425f, 65f),
        "襟裳岬" to Offset(420f, 120f),
        "洞爺湖畔キャンプ場" to Offset(355f, 105f),
        "支笏湖畔キャンプ場" to Offset(360f, 100f),
        "摩周湖畔キャンプ場" to Offset(430f, 70f),
        "富良野・美瑛エリアキャンプ場" to Offset(395f, 85f),
        "ニセコ周辺キャンプ場" to Offset(350f, 95f),
        "積丹半島・神威岬" to Offset(345f, 85f),
        "知床半島" to Offset(460f, 62f),
        // 東北
        "十和田湖畔キャンプ場" to Offset(345f, 190f),
        "種差海岸" to Offset(365f, 175f),
        "三陸海岸" to Offset(380f, 210f),
        "松島" to Offset(360f, 245f),
        "蔵王" to Offset(330f, 250f),
        "蔵王坊平キャンプ場" to Offset(330f, 250f),
        "磐梯高原キャンプ場" to Offset(335f, 285f),
        "裏磐梯五色沼キャンプ場" to Offset(335f, 285f),
        // 関東
        "九十九里浜" to Offset(340f, 340f),
        "那須高原キャンプ場" to Offset(320f, 300f),
        "奥日光キャンプ場" to Offset(320f, 305f),
        "赤城山" to Offset(305f, 310f),
        "東京スカイツリー・東京タワー" to Offset(315f, 330f),
        "奥多摩キャンプ場群" to Offset(310f, 330f),
        "湘南平" to Offset(315f, 340f),
        "みなとみらい21" to Offset(315f, 340f),
        "丹沢湖キャンプ場" to Offset(315f, 340f),
        "箱根キャンプ場" to Offset(310f, 345f),
        "湘南海岸" to Offset(315f, 340f),
        "城ヶ島" to Offset(315f, 350f),
        "江の島" to Offset(315f, 342f),
        // 甲信越・東海
        "阿智村" to Offset(275f, 330f),
        "野辺山高原" to Offset(280f, 335f),
        "美ヶ原高原" to Offset(275f, 330f),
        "八ヶ岳" to Offset(285f, 335f),
        "霧ヶ峰高原" to Offset(280f, 330f),
        "上高地キャンプ場" to Offset(270f, 330f),
        "白馬岩岳キャンプ場" to Offset(270f, 325f),
        "軽井沢キャンプ場" to Offset(285f, 325f),
        "志賀高原キャンプ場" to Offset(230f, 350f),
        "富士五湖キャンプ場群" to Offset(290f, 340f),
        "清里高原キャンプ場" to Offset(285f, 335f),
        "伊豆・白浜海岸" to Offset(295f, 355f),
        "熱海海岸" to Offset(305f, 350f),
        "伊勢志摩" to Offset(240f, 370f),
        // 北陸
        "能登半島・千里浜なぎさドライブウェイ" to Offset(250f, 285f),
        // 関西
        "琵琶湖畔キャンプ場" to Offset(230f, 350f),
        "天橋立" to Offset(210f, 340f),
        "摩耶山掬星台" to Offset(200f, 350f),
        "六甲山" to Offset(200f, 350f),
        "竹野海岸" to Offset(190f, 340f),
        "若草山" to Offset(225f, 375f),
        "大台ヶ原" to Offset(225f, 380f),
        "大台ヶ原キャンプ場" to Offset(225f, 380f),
        "白良浜" to Offset(215f, 390f),
        "天保山" to Offset(220f, 365f),
        // 中国
        "鳥取砂丘" to Offset(170f, 340f),
        "蒜山高原キャンプ場" to Offset(165f, 345f),
        "角島大橋" to Offset(125f, 370f),
        // 四国
        "剣山キャンプ場" to Offset(185f, 385f),
        "鳴門海峡" to Offset(190f, 385f),
        "四万十川キャンプ場" to Offset(165f, 405f),
        "桂浜" to Offset(165f, 405f),
        "室戸岬" to Offset(185f, 410f),
        "足摺岬" to Offset(155f, 415f),
        // 九州
        "皿倉山" to Offset(105f, 400f),
        "宗像・沖ノ島" to Offset(105f, 395f),
        "久住高原" to Offset(125f, 410f),
        "くじゅう高原キャンプ場" to Offset(125f, 410f),
        "日南海岸" to Offset(110f, 440f),
        "城山公園" to Offset(95f, 450f),
        "霧島高原キャンプ場" to Offset(105f, 440f),
        "屋久島キャンプ場" to Offset(95f, 465f),
        "阿蘇草千里キャンプ場" to Offset(105f, 420f),
        "天草" to Offset(95f, 425f),
        "稲佐山" to Offset(80f, 415f),
        "壱岐・対馬" to Offset(85f, 380f),
        // 沖縄（本島から離れた表示領域。iOS の手打ち座標をそのまま使う）
        "石垣島" to Offset(40f, 485f),
        "西表島" to Offset(35f, 485f),
        "やんばるキャンプ場（沖縄）" to Offset(45f, 475f),
        "宮古島・伊良部大橋" to Offset(50f, 480f),
        "石垣島・川平湾" to Offset(40f, 485f),
        "青の洞窟・真栄田岬" to Offset(45f, 480f),
        "残波岬" to Offset(43f, 478f),
        "万座毛" to Offset(44f, 477f),
        "辺戸岬" to Offset(47f, 473f),
    )
}
