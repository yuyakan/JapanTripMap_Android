package com.example.japantripmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** アイコンマップに載せる 1 スポット。 */
data class IconSpot(
    val id: String,
    val name: String,
    val description: String,
    val prefecture: Prefecture,
    val latitude: Double,
    val longitude: Double,
    val accent: Color,
    val icon: ImageVector,
    val planCategory: PlanItemCategory,
    val typeLabel: String,
    val typeRowLabel: String,
    val popularity: Int,
)

/** マーカー/一覧タップで開く詳細画面用の SpotDetail に変換する。 */
fun IconSpot.toSpotDetail(): SpotDetail = SpotDetail(
    title = name,
    prefecture = prefecture,
    accent = accent,
    icon = icon,
    description = description,
    planCategory = planCategory,
    latitude = latitude,
    longitude = longitude,
    badge = typeLabel,
    popularity = popularity,
    infoRows = listOf(typeRowLabel to typeLabel),
)

private val MapFillStart = Color(0x6633C481)
private val MapFillEnd = Color(0x8014B8A6)
private val MapStroke = Color(0x66FFFFFF)
private val HighlightFill = Color(0x40FF9500) // スポットのある県を淡く強調

/**
 * 県ポリゴン地図の上に、スポットごとのアイコンマーカーを重ねて表示する画面。
 * iOS 版 OnsenMapView / NatureSpotMapView のマップ表示に相当する。
 */
@Composable
fun SpotIconMapScreen(
    title: String,
    spots: List<IconSpot>,
    onOpenSpot: (IconSpot) -> Unit,
    modifier: Modifier = Modifier,
) {
    // スポットのある県の集合（背景で淡く強調する）。
    val spotPrefectures = spots.map { it.prefecture }.toSet()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.Background),
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 2.dp),
        )
        Text(
            text = "${spots.size} 件のスポット",
            fontSize = 13.sp,
            color = AppTheme.TextSecondary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )

        // 地図（正方形）。県ポリゴン + マーカー。
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(horizontal = 8.dp)
                .clipToBounds(),
        ) {
            val side = maxWidth
            // 500x500 -> 実 dp の係数。
            fun mapX(v: Float) = side * (v / Prefecture.MAP_SIZE)
            fun mapY(v: Float) = side * (v / Prefecture.MAP_SIZE)

            // 県ポリゴンを Canvas で描画。
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (prefecture in Prefecture.entries) {
                    drawPrefecturePolygon(
                        prefecture = prefecture,
                        highlighted = prefecture in spotPrefectures,
                    )
                }
            }

            // 各スポットのアイコンマーカー。
            spots.forEach { spot ->
                val pos = GeoProjection.project(spot.latitude, spot.longitude, spot.prefecture)
                SpotMarker(
                    accent = spot.accent,
                    icon = spot.icon,
                    onClick = { onOpenSpot(spot) },
                    modifier = Modifier.offset(
                        x = mapX(pos.x) - 16.dp, // マーカー中心を座標に合わせる（32dp の半分）
                        y = mapY(pos.y) - 16.dp,
                    ),
                )
            }
        }

        // 地図の下に一覧（アイコン + 名前 + 県）。
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(spots) { spot ->
                SpotListRow(spot = spot, onClick = { onOpenSpot(spot) })
            }
        }
    }
}

/**
 * アイコンマーカー（グラデ円 + 白アイコン + 影）。
 * タップ領域は 32dp（指で押しやすいよう表示 24dp より大きめ）、その中央に丸を描く。
 */
@Composable
private fun SpotMarker(
    accent: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .shadow(3.dp, CircleShape, spotColor = Color(0x33000000))
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.85f), accent))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
        }
    }
}

/** 地図下の一覧 1 行。 */
@Composable
private fun SpotListRow(spot: IconSpot, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appCard(corner = 12.dp)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(spot.accent.copy(alpha = 0.85f), spot.accent))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(spot.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(spot.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(spot.prefecture.displayName, fontSize = 12.sp, color = AppTheme.TextSecondary)
        }
    }
}

/** Canvas 用: 県ポリゴンを描く（背景として薄く、スポットのある県は強調）。 */
private fun DrawScope.drawPrefecturePolygon(prefecture: Prefecture, highlighted: Boolean) {
    fun norm(p: Offset) = Offset(
        x = p.x / Prefecture.MAP_SIZE * size.width,
        y = p.y / Prefecture.MAP_SIZE * size.height,
    )

    val pts = prefecture.points
    if (pts.size >= 3) {
        val path = Path()
        val first = norm(pts.first())
        path.moveTo(first.x, first.y)
        for (p in pts.drop(1)) {
            val n = norm(p)
            path.lineTo(n.x, n.y)
        }
        path.close()

        drawPath(
            path = path,
            brush = Brush.linearGradient(listOf(MapFillStart, MapFillEnd)),
            alpha = if (highlighted) 1f else 0.25f,
        )
        if (highlighted) {
            drawPath(path = path, color = HighlightFill)
        }
        drawPath(
            path = path,
            color = MapStroke,
            alpha = if (highlighted) 1f else 0.4f,
            style = Stroke(width = 0.6.dp.toPx()),
        )
    }

    // 沖縄の点線。
    if (prefecture == Prefecture.OKINAWA) {
        val line = prefecture.okinawaLinePoints
        if (line.size >= 2) {
            val path = Path()
            val first = norm(line.first())
            path.moveTo(first.x, first.y)
            for (p in line.drop(1)) {
                val n = norm(p)
                path.lineTo(n.x, n.y)
            }
            drawPath(
                path = path,
                color = Color.Gray,
                alpha = 0.5f,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f)),
                ),
            )
        }
    }
}

// MARK: - 温泉 / 自然 のアイコン・色マッピング

/** 温泉タイプ -> 表示色（iOS 版 OnsenType.color）。 */
private val ONSEN_MAP_COLORS = mapOf(
    "scenic" to Color(0xFF2980D9),
    "historical" to Color(0xFF996B47),
    "therapeutic" to Color(0xFF3D9E66),
    "resort" to Color(0xFF8C61C7),
    "mountain" to Color(0xFFD9732E),
    "seaside" to Color(0xFF2194A8),
    "ski" to Color(0xFF38A394),
)

private val ONSEN_MAP_LABELS = mapOf(
    "scenic" to "絶景", "historical" to "歴史", "therapeutic" to "療養",
    "resort" to "リゾート", "mountain" to "山あい", "seaside" to "海辺", "ski" to "スキー",
)

/** 自然タイプ -> 表示色 / アイコン（iOS 版 NatureSpotType）。 */
private val NATURE_MAP_COLORS = mapOf(
    "night_view" to Color(0xFFD9A521),
    "starry_sky" to Color(0xFF5C50A6),
    "sea" to Color(0xFF2194A8),
    "camping" to Color(0xFF3D9E66),
)

private val NATURE_MAP_LABELS = mapOf(
    "night_view" to "夜景", "starry_sky" to "星空", "sea" to "海", "camping" to "キャンプ",
)

private fun natureIcon(type: String): ImageVector = when (type) {
    "night_view" -> Icons.Filled.AutoAwesome
    "starry_sky" -> Icons.Filled.NightsStay
    "sea" -> Icons.Filled.Waves
    "camping" -> Icons.Filled.Forest
    else -> Icons.Filled.Star
}

/** 県の温泉を IconSpot に変換する。 */
fun onsenIconSpots(prefectures: List<Prefecture> = Prefecture.entries): List<IconSpot> =
    prefectures.flatMap { pref ->
        pref.onsens.map { o ->
            IconSpot(
                id = "${pref.name}-${o.name}",
                name = o.name,
                description = o.description,
                prefecture = pref,
                latitude = o.latitude,
                longitude = o.longitude,
                accent = ONSEN_MAP_COLORS[o.type] ?: Color(0xFF2194A8),
                icon = Icons.Filled.Spa,
                planCategory = PlanItemCategory.ONSEN,
                typeLabel = ONSEN_MAP_LABELS[o.type] ?: o.type,
                typeRowLabel = "泉質タイプ",
                popularity = o.popularity,
            )
        }
    }

/** 県の自然スポットを IconSpot に変換する。 */
fun natureIconSpots(prefectures: List<Prefecture> = Prefecture.entries): List<IconSpot> =
    prefectures.flatMap { pref ->
        pref.natureSpots.map { s ->
            IconSpot(
                id = "${pref.name}-${s.name}",
                name = s.name,
                description = s.description,
                prefecture = pref,
                latitude = s.latitude,
                longitude = s.longitude,
                accent = NATURE_MAP_COLORS[s.type] ?: Color(0xFF3D9E66),
                icon = natureIcon(s.type),
                planCategory = PlanItemCategory.NATURE,
                typeLabel = NATURE_MAP_LABELS[s.type] ?: s.type,
                typeRowLabel = "種別",
                popularity = s.popularity,
            )
        }
    }
