package com.example.japantripmap

import androidx.compose.ui.geometry.Offset

/**
 * 緯度経度を、自作日本地図の 500x500 座標系へ変換するための投影ロジック。
 *
 * iOS 版 OnsenMapView は温泉名ごとの手打ち座標でマーカーを置いていたが、
 * Android 側はスポットが緯度経度を持つため、県重心でフィットした線形式で変換する。
 * 本州は平均 1.6% 程度の誤差で収まる。緯度が極端に低い沖縄だけは線形式だと
 * 地図外へ飛ぶため、沖縄本島まわりの表示領域へマッピングする専用式を使う。
 */
object GeoProjection {
    // 県重心 × 県庁所在地の緯度経度で最小二乗フィットした線形係数。
    // x = A*lng + B, y = C*lat + D （500x500 座標系）
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

    /**
     * 緯度経度を 500x500 座標へ変換する。
     * @param prefecture スポットが属する県。沖縄は専用式、その他は線形式＋県ポリゴンへのクランプ。
     */
    fun project(latitude: Double, longitude: Double, prefecture: Prefecture): Offset {
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
}
