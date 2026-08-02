package com.example.japantripmap

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 単体詳細に渡す汎用データ。観光/グルメ/温泉/自然/祭りの各項目を共通表現にする。
 */
data class SpotDetail(
    val title: String,
    val prefecture: Prefecture,
    val accent: Color,
    val icon: ImageVector,
    val description: String,
    val planCategory: PlanItemCategory,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** ヒーローに出す写真の drawable リソース名（拡張子なし）。null なら写真なし。 */
    val photoResName: String? = null,
    /** バッジ（カテゴリ/タイプ）ラベル。null なら非表示。 */
    val badge: String? = null,
    /** 人気度（1-5）。null なら星非表示。 */
    val popularity: Int? = null,
    /** 「基本情報」の行（ラベル→値）。空なら情報カード非表示。 */
    val infoRows: List<Pair<String, String>> = emptyList(),
    /** 特徴タグ（祭り用）。 */
    val features: List<String> = emptyList(),
    /** 食べログ検索を出すか（グルメ用）。 */
    val tabelogKeyword: String? = null,
)

/**
 * 汎用の単体詳細画面。iOS 版 AttractionDetailView / GourmetDetailView / OnsenDetailView 等を統合移植。
 * ヒーローヘッダー → アクション（プランに追加＋SNS検索） → 地図 → 説明 → 基本情報。
 */
@Composable
fun SpotDetailScreen(
    detail: SpotDetail,
    store: TravelPlanStore,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddToPlanDialog(
            store = store,
            item = PlanItem(
                category = detail.planCategory,
                prefectureName = detail.prefecture.displayName,
                name = detail.title,
                detail = detail.description,
            ),
            onDismiss = { showAddDialog = false },
            onAdded = { planTitle ->
                showAddDialog = false
                Toast.makeText(context, "「$planTitle」に追加しました", Toast.LENGTH_SHORT).show()
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(detail.accent)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // ヒーローヘッダー。
            HeroHeader(detail)

            // 本文（薄い背景の上にカードを重ねる）。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.Background)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // アクションカード（プランに追加 + SNS検索）。
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appCard()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AddToPlanButton(detail.accent) { showAddDialog = true }
                    Text("もっと調べる", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    SocialSearchButtons(
                        query = detail.title,
                        tabelogArea = if (detail.tabelogKeyword != null) detail.prefecture.slug else null,
                        tabelogKeyword = detail.tabelogKeyword ?: detail.title,
                    )
                }

                detail.mapPlace()?.let { place ->
                    PlacesMapSection(
                        title = "地図",
                        places = listOf(place),
                    )
                }

                // 説明カード。
                if (detail.description.isNotBlank()) {
                    DetailCard {
                        SectionHeader(Icons.Filled.LocationOn, "詳しい情報", detail.accent)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(detail.description, fontSize = 14.sp, color = Color(0xFF444444), lineHeight = 22.sp)
                    }
                }

                // 基本情報カード。
                if (detail.infoRows.isNotEmpty()) {
                    DetailCard {
                        SectionHeader(Icons.Filled.Star, "基本情報", detail.accent)
                        Spacer(modifier = Modifier.height(6.dp))
                        detail.infoRows.forEachIndexed { i, (label, value) ->
                            if (i > 0) HorizontalDivider(color = Color(0x11000000))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(label, fontSize = 14.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // 特徴タグ（祭り）。
                if (detail.features.isNotEmpty()) {
                    DetailCard {
                        SectionHeader(Icons.Filled.Star, "特徴", detail.accent)
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            detail.features.forEach { feat ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(detail.accent, CircleShape),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(feat, fontSize = 14.sp, color = Color(0xFF444444))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // 閉じるボタン（右上フローティング）。
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 44.dp, end = 18.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "閉じる", tint = detail.accent, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun HeroHeader(detail: SpotDetail) {
    val context = LocalContext.current
    val photoId = detail.photoResName?.let {
        context.resources.getIdentifier(it, "drawable", context.packageName)
    } ?: 0

    Box(modifier = Modifier.fillMaxWidth()) {
        // 写真があれば背景に敷く。無ければアクセント色のグラデーション。
        if (photoId != 0) {
            Image(
                painter = painterResource(id = photoId),
                contentDescription = detail.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize(),
            )
            // 下部を暗くして文字を読みやすくするオーバーレイ。
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.65f)),
                        ),
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(listOf(detail.accent, detail.accent.copy(alpha = 0.85f))),
                    ),
            )
        }

        HeroContent(detail, showIconCircle = photoId == 0)
    }
}

@Composable
private fun HeroContent(detail: SpotDetail, showIconCircle: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 90.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 写真がないときだけアイコン丸を出す（写真時は写真が主役）。
        if (showIconCircle) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(detail.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }
        Text(detail.title, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${detail.prefecture.displayName}（${detail.prefecture.regionName}）",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
        // バッジ＋星。
        if (detail.badge != null || detail.popularity != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (detail.badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(detail.badge, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                detail.popularity?.let { pop ->
                    Row {
                        repeat(5) { i ->
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (i < pop) Color.White else Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appCard()
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .background(accent, RoundedCornerShape(2.dp)),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AddToPlanButton(accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("プランに追加", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
