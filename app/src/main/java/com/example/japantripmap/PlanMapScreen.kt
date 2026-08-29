package com.example.japantripmap

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

/**
 * プラン全体を地図で見る画面。iOS 版 PlanMapView を移植。
 * - 日程モードでは日ごとにピンを色分けし、同じ日の項目を訪問順にルート線でつなぐ。
 * - 日フィルタ（全て / Day1 / Day2 …）。フラットモードではカテゴリ色ピンのみ。
 * - ピンタップで詳細（onOpenItem）、特定の日を選ぶと「経路案内」で Google Maps を開く。
 */

/** 地図に載せる 1 項目（座標解決済み）。 */
private data class MappedItem(
    val item: PlanItem,
    val position: LatLng,
    val day: Int?,
)

/** 日ごとの色（iOS 版 PlanMapView.dayPalette 相当）。 */
private val DayPalette = listOf(
    Color(0xFFE53935), Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00),
    Color(0xFF8E24AA), Color(0xFFEC407A), Color(0xFF00897B), Color(0xFF3949AB),
    Color(0xFF6D4C41), Color(0xFF00ACC1),
)

private fun colorForDay(day: Int?): Color {
    if (day == null || day < 1) return Color.Gray
    return DayPalette[(day - 1) % DayPalette.size]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanMapScreen(
    plan: TravelPlan,
    onOpenItem: (PlanItem) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val dayMode = plan.groupingMode == PlanGroupingMode.DAY
    var selectedDay by remember { mutableStateOf<Int?>(null) } // null = 全日

    // 座標を解決できた全項目。
    val allMapped = remember(plan) {
        plan.items.mapNotNull { item ->
            coordinateForPlanItem(item)?.let { MappedItem(item, it, item.dayNumber) }
        }
    }

    // 日フィルタ適用後の表示対象。
    val visible = remember(allMapped, selectedDay, dayMode) {
        if (dayMode && selectedDay != null) allMapped.filter { it.day == selectedDay } else allMapped
    }

    // 日ごとのルート（訪問順＝時間ブロック順に並べた座標列。2 点以上のみ）。
    val routes = remember(plan, selectedDay, dayMode) {
        if (!dayMode) emptyList() else buildList {
            for (day in 1..maxOf(1, plan.dayCount)) {
                if (selectedDay != null && selectedDay != day) continue
                val coords = plan.orderedItems(day).mapNotNull { coordinateForPlanItem(it) }
                if (coords.size >= 2) add(day to coords)
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            centerOfPositions(allMapped.map { it.position }),
            zoomForPositions(allMapped.map { it.position }),
        )
    }
    val properties = remember { MapProperties() }
    val uiSettings = remember {
        MapUiSettings(zoomControlsEnabled = true, mapToolbarEnabled = false, myLocationButtonEnabled = false)
    }

    Scaffold(
        containerColor = AppTheme.Background,
        topBar = {
            TopAppBar(
                title = { Text(plan.title, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.TopBar),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (allMapped.isEmpty()) {
                Text(
                    stringResource(R.string.plan_map_empty),
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                )
                return@Box
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings,
            ) {
                // ルート線（日ごとに色分け・破線風の細線）。
                routes.forEach { (day, coords) ->
                    Polyline(
                        points = coords,
                        color = colorForDay(day),
                        width = 8f,
                    )
                }
                // ピン（色は日程モード=日色、フラット=カテゴリ色）。
                visible.forEach { mapped ->
                    val tint = if (dayMode) colorForDay(mapped.day) else planCategoryColor(mapped.item.category)
                    val order = if (dayMode) routeOrder(plan, mapped.item) else null
                    key(mapped.item.id, tint, order) {
                        val markerState = rememberUpdatedMarkerState(position = mapped.position)
                        MarkerComposable(
                            state = markerState,
                            onClick = {
                                onOpenItem(mapped.item)
                                true
                            },
                        ) {
                            PlanMapPin(item = mapped.item, tint = tint, order = order)
                        }
                    }
                }
            }

            // 日フィルタバー（日程モードのみ・上部）。
            if (dayMode) {
                DayFilterBar(
                    dayCount = plan.dayCount,
                    selectedDay = selectedDay,
                    onSelect = { selectedDay = it },
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }

            // 経路案内ボタン（特定の日を選択中でその日に 2 地点以上あるとき）。
            val day = selectedDay
            if (day != null) {
                val dayItems = plan.orderedItems(day).filter { coordinateForPlanItem(it) != null }
                if (dayItems.size >= 2) {
                    PlanPrimaryButton(
                        text = stringResource(R.string.plan_map_view_route, day),
                        icon = Icons.Filled.Directions,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        onClick = { openDayRoute(context, dayItems) },
                    )
                }
            }
        }
    }
}

/** ピンの見た目（カテゴリアイコン丸＋訪問順バッジ）。 */
@Composable
private fun PlanMapPin(item: PlanItem, tint: Color, order: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(tint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                planCategoryIcon(item.category),
                contentDescription = localizeData(item.name),
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
        if (order != null) {
            Box(
                modifier = Modifier
                    .padding(top = 1.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            ) {
                Text("$order", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = tint)
            }
        }
    }
}

@Composable
private fun DayFilterBar(
    dayCount: Int,
    selectedDay: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.92f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DayChip(title = stringResource(R.string.plan_map_all_days), tint = PlanTheme.Primary, selected = selectedDay == null) { onSelect(null) }
        for (day in 1..maxOf(1, dayCount)) {
            DayChip(title = stringResource(R.string.plan_day_format, day), tint = colorForDay(day), selected = selectedDay == day) { onSelect(day) }
        }
    }
}

@Composable
private fun DayChip(title: String, tint: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) tint else tint.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else tint,
        )
    }
}

// MARK: - 経路案内（Google Maps）

/** その日の項目を経由地として Google Maps の経路を開く（アプリ優先・無ければ Web）。 */
private fun openDayRoute(context: Context, items: List<PlanItem>) {
    if (items.size < 2) return
    val queries = items.map { placeQuery(it) }
    val origin = queries.first()
    val destination = queries.last()
    val waypoints = queries.drop(1).dropLast(1)

    // Google Maps アプリ用（google.navigation は単一目的地なので、複数経由地は dir URL を使う）。
    val webUri = buildString {
        append("https://www.google.com/maps/dir/?api=1")
        append("&origin=").append(Uri.encode(origin))
        append("&destination=").append(Uri.encode(destination))
        if (waypoints.isNotEmpty()) {
            append("&waypoints=").append(Uri.encode(waypoints.joinToString("|")))
        }
        append("&travelmode=driving")
    }
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webUri)).apply {
        // Google Maps アプリがあればアプリで開く。
        setPackage("com.google.android.apps.maps")
    }
    runCatching { context.startActivity(intent) }.onFailure {
        // アプリが無ければ通常のブラウザ/選択で開く。
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri))) }
    }
}

/**
 * Google Maps に渡す地点表現（iOS 版 placeQuery 相当）。
 * - カスタム項目: 施設名があれば名前、無ければ座標。
 * - アプリ項目: 「名前 県名」で同名地の誤解決を減らす。
 */
private fun placeQuery(item: PlanItem): String {
    if (item.category in CUSTOM_PLAN_CATEGORIES) {
        if (!item.placeName.isNullOrBlank()) return item.placeName!!
        val c = coordinateForPlanItem(item)
        return if (c != null) "${c.latitude},${c.longitude}" else item.name
    }
    return if (item.prefectureName.isNotBlank()) "${item.name} ${item.prefectureName}" else item.name
}

// MARK: - 訪問順・カメラ

/** その項目が同じ日の中で何番目に訪れるか（1 始まり・訪問順）。未割当は null。 */
private fun routeOrder(plan: TravelPlan, item: PlanItem): Int? {
    val day = item.dayNumber ?: return null
    val ordered = plan.orderedItems(day).filter { coordinateForPlanItem(it) != null }
    val index = ordered.indexOfFirst { it.id == item.id }
    return if (index >= 0) index + 1 else null
}

private fun centerOfPositions(positions: List<LatLng>): LatLng {
    if (positions.isEmpty()) return LatLng(36.2048, 138.2529) // 日本の中心あたり
    return LatLng(positions.map { it.latitude }.average(), positions.map { it.longitude }.average())
}

private fun zoomForPositions(positions: List<LatLng>): Float {
    if (positions.size <= 1) return 12f
    val latSpan = positions.maxOf { it.latitude } - positions.minOf { it.latitude }
    val lngSpan = positions.maxOf { it.longitude } - positions.minOf { it.longitude }
    val span = maxOf(latSpan, lngSpan)
    return when {
        span < 0.05 -> 13.5f
        span < 0.15 -> 11.5f
        span < 0.4 -> 10f
        span < 0.9 -> 8.3f
        span < 2.0 -> 7.2f
        span < 5.0 -> 6f
        else -> 4.5f
    }
}
