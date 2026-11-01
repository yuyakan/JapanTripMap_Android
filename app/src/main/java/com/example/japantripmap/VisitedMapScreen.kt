package com.example.japantripmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val UnvisitedFill = Color(0xFFE5E5EA)

/**
 * 訪問済みマップ。マイプランの「訪問済み」タブ。iOS 版 VisitedMapView。
 * 訪問済みの県をブランドグラデで塗り、左上に踏破率リング、右下に地方別集計ボタン。
 * 県タップで訪問済みの切替＋その県を含むプラン一覧のシートを開く。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitedMapScreen(store: TravelPlanStore, onOpenPlan: (String) -> Unit) {
    val visited = VisitedPrefectureStore.visited
    val total = Prefecture.entries.size
    val ratio = visited.size.toFloat() / total
    var showRegions by remember { mutableStateOf(false) }
    var selectedPrefecture by remember { mutableStateOf<Prefecture?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            VisitedJapanMap(
                visited = visited,
                onTap = { selectedPrefecture = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).aspectRatio(1f),
            )
            if (visited.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(stringResource(R.string.visited_empty_title), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextPrimary)
                    Text(stringResource(R.string.visited_empty_message), fontSize = 12.sp, color = AppTheme.TextSecondary, textAlign = TextAlign.Center)
                }
            }
        }

        CompletionRing(
            ratio = ratio,
            visitedCount = visited.size,
            total = total,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 12.dp).size(100.dp),
        )

        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            PlanFab(Icons.Filled.BarChart, stringResource(R.string.visited_region_header), size = 52) { showRegions = true }
        }
    }

    if (showRegions) {
        ModalBottomSheet(
            onDismissRequest = { showRegions = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color.White,
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.visited_region_header), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showRegions = false }) {
                        Text(stringResource(R.string.common_done), color = PlanTheme.Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                JapanRegion.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                        row.forEach { region -> RegionChip(region, visited, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    selectedPrefecture?.let { pref ->
        PrefectureVisitedSheet(
            prefecture = pref,
            store = store,
            onDismiss = { selectedPrefecture = null },
            onOpenPlan = {
                selectedPrefecture = null
                onOpenPlan(it)
            },
        )
    }
}

/** 日本地図（500x500 座標系を正規化）。訪問済みをブランドグラデ、未訪問をグレーで塗り、タップで県を返す。 */
@Composable
private fun VisitedJapanMap(visited: Set<Prefecture>, onTap: (Prefecture) -> Unit, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { pos ->
                val x = pos.x / size.width * Prefecture.MAP_SIZE
                val y = pos.y / size.height * Prefecture.MAP_SIZE
                Prefecture.entries.firstOrNull { insidePolygon(x, y, it.points) }?.let(onTap)
            }
        },
    ) {
        Prefecture.entries.forEach { pref ->
            val path = Path().apply {
                pref.points.forEachIndexed { i, p ->
                    val o = Offset(p.x / Prefecture.MAP_SIZE * size.width, p.y / Prefecture.MAP_SIZE * size.height)
                    if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y)
                }
                close()
            }
            if (pref in visited) drawPath(path, PlanTheme.brandGradient) else drawPath(path, UnvisitedFill)
            drawPath(path, Color.White.copy(alpha = 0.7f), style = Stroke(width = 0.6.dp.toPx()))
        }
    }
}

/** レイキャスト法の点内判定。 */
private fun insidePolygon(px: Float, py: Float, poly: List<Offset>): Boolean {
    var inside = false
    var j = poly.size - 1
    for (i in poly.indices) {
        val xi = poly[i].x
        val yi = poly[i].y
        val xj = poly[j].x
        val yj = poly[j].y
        if ((yi > py) != (yj > py) && px < (xj - xi) * (py - yi) / (yj - yi) + xi) inside = !inside
        j = i
    }
    return inside
}

/** 踏破率のドーナツリング（中央に % と「訪問数/全県」）。 */
@Composable
private fun CompletionRing(ratio: Float, visitedCount: Int, total: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.shadow(6.dp, CircleShape, spotColor = PlanTheme.cardShadow).clip(CircleShape).background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val stroke = 9.dp.toPx()
            drawArc(UnvisitedFill, 0f, 360f, useCenter = false, style = Stroke(width = stroke))
            drawArc(
                PlanTheme.brandGradient,
                startAngle = -90f,
                sweepAngle = 360f * maxOf(0.0001f, ratio),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(ratio * 100).let { kotlin.math.round(it).toInt() }}%", fontSize = 22.sp, fontWeight = FontWeight.Black, color = PlanTheme.Primary)
            Text("$visitedCount/$total", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextSecondary)
        }
    }
}

/** 1 地方分のピル（地方名と M/N。全県達成はブランド色で塗る）。 */
@Composable
private fun RegionChip(region: JapanRegion, visited: Set<Prefecture>, modifier: Modifier = Modifier) {
    val prefs = region.prefectures
    val count = prefs.count { it in visited }
    val complete = count == prefs.size && prefs.isNotEmpty()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(if (complete) Modifier.background(PlanTheme.brandGradient) else Modifier.background(PlanTheme.groupedSurface2))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            region.localizedName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (complete) Color.White else AppTheme.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (complete) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            stringResource(R.string.visited_region_count_format, count, prefs.size),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                complete -> Color.White.copy(alpha = 0.95f)
                count > 0 -> PlanTheme.Primary
                else -> AppTheme.TextSecondary
            },
        )
    }
}

/** 県タップで開くシート（訪問済みトグル＋この県を含むプラン）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrefectureVisitedSheet(
    prefecture: Prefecture,
    store: TravelPlanStore,
    onDismiss: () -> Unit,
    onOpenPlan: (String) -> Unit,
) {
    val related = store.plansContaining(prefecture)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(prefecture.localizedName(), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_done), color = PlanTheme.Primary, fontWeight = FontWeight.SemiBold) }
            }
            Row(modifier = Modifier.fillMaxWidth().planCard(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Verified, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.visited_state_toggle), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(
                    checked = VisitedPrefectureStore.isVisited(prefecture),
                    onCheckedChange = { VisitedPrefectureStore.setVisited(prefecture, it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = PlanTheme.Primary, checkedThumbColor = Color.White, checkedBorderColor = PlanTheme.Primary),
                )
            }
            Column(modifier = Modifier.fillMaxWidth().planCard(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.visited_related_plans), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                if (related.isEmpty()) {
                    Text(stringResource(R.string.visited_related_none), fontSize = 14.sp, color = AppTheme.TextSecondary)
                } else {
                    related.forEach { plan ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(PlanTheme.groupedSurface2)
                                .clickable { onOpenPlan(plan.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(PlanTheme.brandGradient),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    if (plan.status == PlanStatus.COMPLETED) Icons.Filled.Verified else Icons.Filled.Luggage,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(plan.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(stringResource(R.string.plan_itemcount_format, plan.items.size), fontSize = 11.sp, color = AppTheme.TextSecondary)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AppTheme.TextSecondary.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}
