package com.example.japantripmap

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 自然スポットのタイプ -> 表示色（iOS 版 NatureSpotType.color に対応）。 */
private val NATURE_TYPE_COLORS = mapOf(
    "night_view" to Color(0xFFD9A521), // 夜景=イエロー系（白文字用に調整）
    "starry_sky" to Color(0xFF5C50A6), // 星空=インディゴ
    "sea" to Color(0xFF2194A8),        // 海=ティール
    "camping" to Color(0xFF3D9E66),    // キャンプ=グリーン
)

private val NATURE_TYPE_LABELS = mapOf(
    "night_view" to "夜景", "starry_sky" to "星空", "sea" to "海", "camping" to "キャンプ",
)

private fun natureColor(t: String) = NATURE_TYPE_COLORS[t] ?: Color.Gray
// ラベルは表示専用なのでロケールに応じて英訳して返す。
private fun natureLabel(t: String) = localizeTypeLabel(NATURE_TYPE_LABELS[t] ?: t)

/**
 * 自然スポット詳細画面。選ばれた県の自然スポット一覧をカード表示する。
 * iOS 版 NatureSpotMapView の結果表示に相当（地図は後日）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NatureDetailScreen(
    prefecture: Prefecture,
    store: TravelPlanStore,
    onOpenSpot: (SpotDetail) -> Unit,
    onBack: () -> Unit,
) {
    val spots = prefecture.natureSpots
    val context = LocalContext.current
    var pendingItem by remember { mutableStateOf<PlanItem?>(null) }
    // 地図ドラッグ中はリストスクロールを止め、地図のパンを親に横取りされないようにする。
    var mapTouched by remember { mutableStateOf(false) }

    pendingItem?.let { item ->
        AddToPlanDialog(
            store = store,
            item = item,
            onDismiss = { pendingItem = null },
            onAdded = { planTitle ->
                pendingItem = null
                Toast.makeText(context, context.getString(R.string.added_to_plan, planTitle), Toast.LENGTH_SHORT).show()
            },
        )
    }

    Scaffold(
        containerColor = AppTheme.Background,
        topBar = {
            TopAppBar(
                modifier = Modifier.drawBottomHairline(),
                title = { Text(stringResource(R.string.nature_prefecture_title, prefecture.localizedName()), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.TopBar),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            userScrollEnabled = !mapTouched,
        ) {
            item {
                Text(
                    text = "${prefecture.localizedName()}（${prefecture.localizedRegionName()}）",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3D9E66),
                )
                Text(
                    text = stringResource(R.string.nature_count, spots.size),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            item {
                PlacesMapSection(
                    title = stringResource(R.string.nature_prefecture_map, prefecture.localizedName()),
                    onMapTouch = { mapTouched = it },
                    places = spots.map { s ->
                        MapPlace(
                            id = s.name,
                            title = s.name,
                            subtitle = s.description,
                            latitude = s.latitude,
                            longitude = s.longitude,
                        )
                    },
                )
            }
            items(spots.size) { i ->
                val s = spots[i]
                NatureCard(
                    s,
                    onAdd = {
                        pendingItem = PlanItem(
                            category = PlanItemCategory.NATURE,
                            prefectureName = prefecture.displayName,
                            name = s.name,
                            detail = s.description,
                        )
                    },
                    onClick = {
                        onOpenSpot(
                            SpotDetail(
                                title = s.name,
                                prefecture = prefecture,
                                accent = natureColor(s.type),
                                icon = Icons.Filled.Park,
                                description = s.description,
                                planCategory = PlanItemCategory.NATURE,
                                latitude = s.latitude,
                                longitude = s.longitude,
                                badge = natureLabel(s.type),
                                popularity = s.popularity,
                                infoRows = listOf("種別" to natureLabel(s.type)),
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun NatureCard(spot: NatureSpot, onAdd: () -> Unit, onClick: () -> Unit) {
    val accent = natureColor(spot.type)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appCard()
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localizeData(spot.name), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(natureLabel(spot.type), fontSize = 11.sp, color = accent)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(spot.popularity) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.AddCircle,
                contentDescription = stringResource(R.string.common_add_to_plan),
                tint = accent,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onAdd),
            )
        }
        Text(
            text = localizeData(spot.description),
            fontSize = 13.sp,
            color = Color(0xFF555555),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
