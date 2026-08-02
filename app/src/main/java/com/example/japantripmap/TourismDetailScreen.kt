package com.example.japantripmap

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CardGiftcard
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HeaderBlue = Color(0xFF4285F4)
private val HeaderPurple = Color(0xFF9C27B0)

/** グルメ／お土産カテゴリ -> 表示色（iOS 版の手調整カラーに合わせる）。 */
private val CATEGORY_COLORS = mapOf(
    // gourmet
    "ramen" to Color(0xFFED7321),
    "seafood" to Color(0xFF2980D9),
    "meat" to Color(0xFFD6454D),
    "sweets" to Color(0xFFDB5994),
    "local" to Color(0xFF3D9E66),
    "drinks" to Color(0xFF8C61C7),
    "vegetables" to Color(0xFFD29421),
    // souvenir
    "food" to Color(0xFFED7321),
    "crafts" to Color(0xFF996B47),
    "textiles" to Color(0xFF2980D9),
    "ceramics" to Color(0xFF737380),
    "regional" to Color(0xFF3D9E66),
)

/** カテゴリの日本語ラベル。 */
private val CATEGORY_LABELS = mapOf(
    "ramen" to "ラーメン", "seafood" to "海鮮", "meat" to "肉", "sweets" to "スイーツ",
    "local" to "郷土料理", "drinks" to "ドリンク", "vegetables" to "野菜・果物",
    "food" to "食品", "crafts" to "工芸品", "textiles" to "織物", "ceramics" to "陶磁器",
    "regional" to "地域特産",
)

private fun categoryColor(cat: String) = CATEGORY_COLORS[cat] ?: Color.Gray
private fun categoryLabel(cat: String) = CATEGORY_LABELS[cat] ?: cat

/**
 * 観光詳細画面。iOS 版 TourismDetailView を移植（地図は後日）。
 * 県名ヘッダー → 観光スポット → ご当地グルメ → お土産 → フォトギャラリー をリスト表示する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourismDetailScreen(
    prefecture: Prefecture,
    store: TravelPlanStore,
    onOpenSpot: (SpotDetail) -> Unit,
    onBack: () -> Unit,
) {
    val info = prefecture.tourismInfo
    val gourmets = prefecture.gourmets
    val souvenirs = prefecture.souvenirs

    val context = LocalContext.current
    // 「プランに追加」対象の項目。null なら未選択。
    var pendingItem by remember { mutableStateOf<PlanItem?>(null) }

    pendingItem?.let { item ->
        AddToPlanDialog(
            store = store,
            item = item,
            onDismiss = { pendingItem = null },
            onAdded = { planTitle ->
                pendingItem = null
                Toast.makeText(context, "「$planTitle」に追加しました", Toast.LENGTH_SHORT).show()
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(prefecture.displayName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF7F7FA),
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 県名の大きな見出し。
            item {
                Text(
                    text = prefecture.displayName,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = HeaderBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
                Text(
                    text = prefecture.regionName,
                    fontSize = 14.sp,
                    color = Color.Gray,
                )
            }

            // 観光スポット。
            if (info != null && info.attractions.isNotEmpty()) {
                item { SectionHeader(Icons.Filled.LocationOn, "観光スポット", HeaderBlue) }
                items(info.attractions.size) { i ->
                    val a = info.attractions[i]
                    InfoCard(
                        title = a.name,
                        description = a.description,
                        accent = HeaderPurple,
                        photoResName = ATTRACTION_PHOTOS[a.name],
                        onAdd = {
                            pendingItem = PlanItem(
                                category = PlanItemCategory.ATTRACTION,
                                prefectureName = prefecture.displayName,
                                name = a.name,
                                detail = a.description,
                            )
                        },
                        onClick = {
                            onOpenSpot(
                                SpotDetail(
                                    title = a.name,
                                    prefecture = prefecture,
                                    accent = HeaderBlue,
                                    icon = Icons.Filled.LocationOn,
                                    description = a.description,
                                    planCategory = PlanItemCategory.ATTRACTION,
                                    photoResName = ATTRACTION_PHOTOS[a.name],
                                ),
                            )
                        },
                    )
                }
            }

            // ご当地グルメ。
            if (gourmets.isNotEmpty()) {
                item { SectionHeader(Icons.Filled.Restaurant, "ご当地グルメ", Color(0xFFED7321)) }
                items(gourmets.size) { i ->
                    val g = gourmets[i]
                    InfoCard(
                        title = g.name,
                        description = g.description,
                        accent = categoryColor(g.category),
                        categoryLabel = categoryLabel(g.category),
                        price = g.price,
                        season = g.bestSeason,
                        popularity = g.popularity,
                        onAdd = {
                            pendingItem = PlanItem(
                                category = PlanItemCategory.GOURMET,
                                prefectureName = prefecture.displayName,
                                name = g.name,
                                detail = g.description,
                            )
                        },
                        onClick = {
                            onOpenSpot(
                                SpotDetail(
                                    title = g.name,
                                    prefecture = prefecture,
                                    accent = categoryColor(g.category),
                                    icon = Icons.Filled.Restaurant,
                                    description = g.description,
                                    planCategory = PlanItemCategory.GOURMET,
                                    badge = categoryLabel(g.category),
                                    popularity = g.popularity,
                                    infoRows = listOf(
                                        "価格帯" to g.price,
                                        "おすすめ時期" to g.bestSeason,
                                        "カテゴリ" to categoryLabel(g.category),
                                    ),
                                    tabelogKeyword = g.name,
                                ),
                            )
                        },
                    )
                }
            }

            // お土産。
            if (souvenirs.isNotEmpty()) {
                item { SectionHeader(Icons.Filled.CardGiftcard, "お土産", Color(0xFFDB5994)) }
                items(souvenirs.size) { i ->
                    val s = souvenirs[i]
                    InfoCard(
                        title = s.name,
                        description = s.description,
                        accent = categoryColor(s.category),
                        categoryLabel = categoryLabel(s.category),
                        price = s.price,
                        season = s.bestSeason,
                        popularity = s.popularity,
                        onAdd = {
                            pendingItem = PlanItem(
                                category = PlanItemCategory.SOUVENIR,
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
                                    accent = categoryColor(s.category),
                                    icon = Icons.Filled.CardGiftcard,
                                    description = s.description,
                                    planCategory = PlanItemCategory.SOUVENIR,
                                    badge = categoryLabel(s.category),
                                    popularity = s.popularity,
                                    infoRows = listOf(
                                        "価格帯" to s.price,
                                        "おすすめ時期" to s.bestSeason,
                                        "カテゴリ" to categoryLabel(s.category),
                                    ),
                                ),
                            )
                        },
                    )
                }
            }

            // フォトギャラリー（写真を持つ観光スポットの横スクロール）。一番下に配置。
            val photographed = info?.attractions?.filter { ATTRACTION_PHOTOS[it.name] != null } ?: emptyList()
            if (photographed.isNotEmpty()) {
                item { SectionHeader(Icons.Filled.PhotoLibrary, "フォトギャラリー", HeaderBlue) }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(photographed.size) { i ->
                            val a = photographed[i]
                            PhotoCard(
                                spotName = a.name,
                                resName = ATTRACTION_PHOTOS[a.name]!!,
                                onClick = {
                                    onOpenSpot(
                                        SpotDetail(
                                            title = a.name,
                                            prefecture = prefecture,
                                            accent = HeaderBlue,
                                            icon = Icons.Filled.LocationOn,
                                            description = a.description,
                                            planCategory = PlanItemCategory.ATTRACTION,
                                            photoResName = ATTRACTION_PHOTOS[a.name],
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/** フォトギャラリーの 1 枚（写真＋スポット名）。 */
@Composable
private fun PhotoCard(spotName: String, resName: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val photoId = context.resources.getIdentifier(resName, "drawable", context.packageName)
    if (photoId == 0) return
    Column(
        modifier = Modifier
            .width(240.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(id = photoId),
            contentDescription = spotName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
        )
        Text(
            text = spotName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    description: String,
    accent: Color,
    categoryLabel: String? = null,
    price: String? = null,
    season: String? = null,
    popularity: Int? = null,
    onAdd: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    photoResName: String? = null,
) {
    val context = LocalContext.current
    val photoId = photoResName?.let {
        context.resources.getIdentifier(it, "drawable", context.packageName)
    } ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        // 写真があればカード上部に表示。
        if (photoId != 0) {
            Image(
                painter = painterResource(id = photoId),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            )
        }
        Column(modifier = Modifier.padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(accent, RoundedCornerShape(5.dp)),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (categoryLabel != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(accent.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(categoryLabel, fontSize = 11.sp, color = accent)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (popularity != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(popularity) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
            if (onAdd != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = "プランに追加",
                    tint = accent,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onAdd),
                )
            }
        }
        Text(
            text = description,
            fontSize = 13.sp,
            color = Color(0xFF555555),
            modifier = Modifier.padding(top = 6.dp),
        )
        if (price != null || season != null) {
            Row(modifier = Modifier.padding(top = 6.dp)) {
                if (!price.isNullOrBlank()) {
                    Text("💰 $price", fontSize = 12.sp, color = Color.Gray)
                }
                if (!season.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("📅 $season", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        } // 内側 Column（padding 14dp）を閉じる
    }
}
