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
import androidx.compose.material.icons.filled.Hotel
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

/** 温泉タイプ -> 表示色（iOS 版 OnsenType.color に合わせる）。 */
private val ONSEN_TYPE_COLORS = mapOf(
    "scenic" to Color(0xFF2980D9),
    "historical" to Color(0xFF996B47),
    "therapeutic" to Color(0xFF3D9E66),
    "resort" to Color(0xFF8C61C7),
    "mountain" to Color(0xFFD9732E),
    "seaside" to Color(0xFF2194A8),
    "ski" to Color(0xFF38A394),
)

/** 温泉タイプ -> 日本語ラベル。 */
private val ONSEN_TYPE_LABELS = mapOf(
    "scenic" to "絶景", "historical" to "歴史", "therapeutic" to "療養",
    "resort" to "リゾート", "mountain" to "山あい", "seaside" to "海辺", "ski" to "スキー",
)

private fun onsenColor(t: String) = ONSEN_TYPE_COLORS[t] ?: Color.Gray
// ラベルは表示専用なのでロケールに応じて英訳して返す。
private fun onsenLabel(t: String) = localizeTypeLabel(ONSEN_TYPE_LABELS[t] ?: t)

/**
 * 温泉詳細画面。選ばれた県の温泉地一覧をカード表示する。
 * iOS 版 OnsenMapView の結果表示に相当（地図は後日）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnsenDetailScreen(
    prefecture: Prefecture,
    store: TravelPlanStore,
    onOpenSpot: (SpotDetail) -> Unit,
    onBack: () -> Unit,
) {
    val onsens = prefecture.onsens
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
                title = { Text(stringResource(R.string.onsen_prefecture_title, prefecture.localizedName()), fontWeight = FontWeight.Bold) },
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
                    color = Color(0xFFED7321),
                )
                Text(
                    text = stringResource(R.string.onsen_count, onsens.size),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            item {
                PlacesMapSection(
                    title = stringResource(R.string.onsen_prefecture_map, prefecture.localizedName()),
                    onMapTouch = { mapTouched = it },
                    places = onsens.map { o ->
                        MapPlace(
                            id = o.name,
                            title = o.name,
                            subtitle = o.description,
                            latitude = o.latitude,
                            longitude = o.longitude,
                        )
                    },
                )
            }
            items(onsens.size) { i ->
                val o = onsens[i]
                OnsenCard(
                    o,
                    onAdd = {
                        pendingItem = PlanItem(
                            category = PlanItemCategory.ONSEN,
                            prefectureName = prefecture.displayName,
                            name = o.name,
                            detail = o.description,
                        )
                    },
                    onClick = {
                        onOpenSpot(
                            SpotDetail(
                                title = o.name,
                                prefecture = prefecture,
                                accent = onsenColor(o.type),
                                icon = Icons.Filled.Hotel,
                                description = o.description,
                                planCategory = PlanItemCategory.ONSEN,
                                latitude = o.latitude,
                                longitude = o.longitude,
                                badge = onsenLabel(o.type),
                                popularity = o.popularity,
                                infoRows = listOf("泉質タイプ" to onsenLabel(o.type)),
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun OnsenCard(onsen: Onsen, onAdd: () -> Unit, onClick: () -> Unit) {
    val accent = onsenColor(onsen.type)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appCard()
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localizeData(onsen.name), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(onsenLabel(onsen.type), fontSize = 11.sp, color = accent)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(onsen.popularity) {
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
            text = localizeData(onsen.description),
            fontSize = 13.sp,
            color = Color(0xFF555555),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
