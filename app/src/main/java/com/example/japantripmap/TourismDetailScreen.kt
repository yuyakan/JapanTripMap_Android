package com.example.japantripmap

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// MARK: - セクションのアクセント色（iOS 版の各 Rich セクション accent に合わせる）
private val AttractionAccent = Color(0xFF4C8CFA) // 観光: ブルー系
private val GourmetAccent = Color(0xFFED7321) // グルメ: オレンジ (0.93,0.45,0.13)
private val OnsenAccent = Color(0xFF2194A8) // 温泉: ティール (0.13,0.58,0.66)
private val SouvenirAccent = Color(0xFFDB5994) // お土産: ピンク (0.86,0.35,0.58)

// ヘッダーのグラデーション（青→紫）。iOS の LinearGradient([.blue,.purple]) 相当。
private val HeaderGradient = Brush.horizontalGradient(listOf(Color(0xFF0A84FF), Color(0xFFAF52DE)))

/** グルメ／お土産カテゴリ -> 表示色（iOS 版の手調整カラーに合わせる）。 */
private val CATEGORY_COLORS = mapOf(
    // gourmet (FoodCategory)
    "ramen" to Color(0xFFED7321),
    "seafood" to Color(0xFF2980D9),
    "meat" to Color(0xFFD6454D),
    "sweets" to Color(0xFFDB5994),
    "local" to Color(0xFF3D9E66),
    "drinks" to Color(0xFF8C61C7),
    "vegetables" to Color(0xFFD1951F),
    // souvenir (SouvenirCategory)
    "food" to Color(0xFFED7321),
    "crafts" to Color(0xFF996B47),
    "textiles" to Color(0xFF2980D9),
    "ceramics" to Color(0xFF737380),
    "regional" to Color(0xFF3D9E66),
)

/** カテゴリの日本語ラベル（タグ表示用の短縮名）。 */
private val CATEGORY_LABELS = mapOf(
    "ramen" to "ラーメン", "seafood" to "海鮮", "meat" to "肉", "sweets" to "スイーツ",
    "local" to "郷土料理", "drinks" to "ドリンク", "vegetables" to "野菜・果物",
    "food" to "食品", "crafts" to "工芸品", "textiles" to "織物", "ceramics" to "陶磁器",
    "regional" to "地域特産",
)

/** 温泉タイプ -> 表示色（iOS 版 OnsenType.color）。 */
private val ONSEN_TYPE_COLORS = mapOf(
    "scenic" to Color(0xFF2980D9),
    "historical" to Color(0xFF996B47),
    "therapeutic" to Color(0xFF3D9E66),
    "resort" to Color(0xFF8C61C7),
    "mountain" to Color(0xFFD9732E),
    "seaside" to Color(0xFF2194A8),
    "ski" to Color(0xFF38A394),
)

private val ONSEN_TYPE_LABELS = mapOf(
    "scenic" to "絶景", "historical" to "歴史", "therapeutic" to "療養",
    "resort" to "リゾート", "mountain" to "山あい", "seaside" to "海辺", "ski" to "スキー",
)

// プラン詳細のルーター（PlanItemDetail.kt）からグルメ/お土産の色・ラベルを再構築するため internal。
// onsen 系は OnsenDetailScreen.kt にも同名 private があるため衝突を避けて private のまま残す。
internal fun categoryColor(cat: String) = CATEGORY_COLORS[cat] ?: Color.Gray
internal fun categoryLabel(cat: String) = CATEGORY_LABELS[cat] ?: cat
private fun onsenColor(t: String) = ONSEN_TYPE_COLORS[t] ?: Color.Gray
private fun onsenLabel(t: String) = ONSEN_TYPE_LABELS[t] ?: t

/** 最初に見せる件数（iOS 版の Array(items.prefix(4)) に合わせる）。 */
private const val INITIAL_GRID_COUNT = 4

/**
 * 観光詳細画面。iOS 版 TourismDetailView を移植し、見た目も iOS に寄せている。
 * グラデーション県名 → 観光地図 + 横スクロール観光カード → ご当地グルメ（2列リッチカード）
 * → 温泉 → お土産 → フォトギャラリー をリスト表示する。
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
    val onsens = prefecture.onsens

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
        containerColor = AppTheme.Background,
        topBar = {
            TopAppBar(
                modifier = Modifier.drawBottomHairline(),
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.TopBar,
                ),
                windowInsets = WindowInsets(0),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 県名のグラデーション見出し（中央寄せ・コンパクト）。
            item {
                Text(
                    text = prefecture.displayName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(brush = HeaderGradient),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 観光地図 + 横スクロールの観光カード。
            if (info != null && info.attractions.isNotEmpty()) {
                item {
                    RichSectionHeader(
                        icon = Icons.Filled.Map,
                        title = "観光マップ",
                        accent = AttractionAccent,
                    )
                }
                item {
                    PlacesMapSection(
                        showSelectedPlace = false,
                        places = info.attractions.map { a ->
                            MapPlace(
                                id = a.name,
                                title = a.name,
                                subtitle = a.description,
                                latitude = a.latitude,
                                longitude = a.longitude,
                            )
                        },
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(info.attractions.size) { i ->
                            val a = info.attractions[i]
                            AttractionMiniCard(
                                name = a.name,
                                description = a.description,
                                onClick = {
                                    onOpenSpot(
                                        SpotDetail(
                                            title = a.name,
                                            prefecture = prefecture,
                                            accent = AttractionAccent,
                                            icon = Icons.Filled.LocationOn,
                                            description = a.description,
                                            planCategory = PlanItemCategory.ATTRACTION,
                                            latitude = a.latitude,
                                            longitude = a.longitude,
                                            photoResName = ATTRACTION_PHOTOS[a.name],
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }

            // ご当地グルメ（2列リッチカード + もっと見る）。
            if (gourmets.isNotEmpty()) {
                richGridSection(
                    icon = Icons.Filled.Restaurant,
                    title = "ご当地グルメ",
                    subtitle = "${prefecture.displayName}の味覚",
                    accent = GourmetAccent,
                    count = gourmets.size,
                ) { index ->
                    val g = gourmets[index]
                    RichItemCard(
                        name = g.name,
                        description = g.description,
                        accent = categoryColor(g.category),
                        icon = Icons.Filled.Restaurant,
                        tagLabel = categoryLabel(g.category),
                        popularity = g.popularity,
                        price = g.price,
                        season = g.bestSeason,
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
                        onAdd = {
                            pendingItem = PlanItem(
                                category = PlanItemCategory.GOURMET,
                                prefectureName = prefecture.displayName,
                                name = g.name,
                                detail = g.description,
                            )
                        },
                    )
                }
            }

            // 温泉（2列リッチカード）。
            if (onsens.isNotEmpty()) {
                item {
                    RichSectionHeader(
                        icon = Icons.Filled.Spa,
                        title = "温泉情報",
                        accent = OnsenAccent,
                    )
                }
                gridItems(onsens.size) { index ->
                    val o = onsens[index]
                    RichItemCard(
                        name = o.name,
                        description = o.description,
                        accent = onsenColor(o.type),
                        icon = Icons.Filled.Hotel,
                        tagLabel = onsenLabel(o.type),
                        popularity = o.popularity,
                        price = null,
                        season = null,
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
                        onAdd = {
                            pendingItem = PlanItem(
                                category = PlanItemCategory.ONSEN,
                                prefectureName = prefecture.displayName,
                                name = o.name,
                                detail = o.description,
                            )
                        },
                    )
                }
            }

            // お土産（2列リッチカード + もっと見る）。
            if (souvenirs.isNotEmpty()) {
                richGridSection(
                    icon = Icons.Filled.CardGiftcard,
                    title = "お土産",
                    subtitle = "${prefecture.displayName}の思い出",
                    accent = SouvenirAccent,
                    count = souvenirs.size,
                ) { index ->
                    val s = souvenirs[index]
                    RichItemCard(
                        name = s.name,
                        description = s.description,
                        accent = categoryColor(s.category),
                        icon = Icons.Filled.CardGiftcard,
                        tagLabel = categoryLabel(s.category),
                        popularity = s.popularity,
                        price = null,
                        season = s.bestSeason,
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
                        onAdd = {
                            pendingItem = PlanItem(
                                category = PlanItemCategory.SOUVENIR,
                                prefectureName = prefecture.displayName,
                                name = s.name,
                                detail = s.description,
                            )
                        },
                    )
                }
            }

            // フォトギャラリー（写真を持つ観光スポットの横スクロール）。一番下に配置。
            val photographed = info?.attractions?.filter { ATTRACTION_PHOTOS[it.name] != null } ?: emptyList()
            if (photographed.isNotEmpty()) {
                item {
                    RichSectionHeader(
                        icon = Icons.Filled.PhotoLibrary,
                        title = "スポット写真",
                        accent = AttractionAccent,
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                            accent = AttractionAccent,
                                            icon = Icons.Filled.LocationOn,
                                            description = a.description,
                                            planCategory = PlanItemCategory.ATTRACTION,
                                            latitude = a.latitude,
                                            longitude = a.longitude,
                                            photoResName = ATTRACTION_PHOTOS[a.name],
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }

            // 最下部の余白。
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

/**
 * 「見出し + 2列グリッド + もっと見る」をまとめて LazyColumn に流し込むヘルパー。
 * iOS 版 RichGourmetSection / RichSouvenirSection の「4件まで表示→もっと見る」挙動を再現する。
 */
private fun androidx.compose.foundation.lazy.LazyListScope.richGridSection(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    count: Int,
    card: @Composable (index: Int) -> Unit,
) {
    // showAll をこのセクション内で保持したいので、状態は item スコープ内で持つ。
    item(key = "header_$title") {
        RichSectionHeader(icon = icon, title = title, subtitle = subtitle, accent = accent)
    }
    // 展開状態はヘッダー直下の 1 item にまとめて描画し、内部で行を組む。
    item(key = "grid_$title") {
        var showAll by remember { mutableStateOf(false) }
        val visible = if (showAll) count else minOf(count, INITIAL_GRID_COUNT)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            var row = 0
            while (row * 2 < visible) {
                val left = row * 2
                val right = left + 1
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) { card(left) }
                    Box(modifier = Modifier.weight(1f)) {
                        if (right < visible) card(right)
                    }
                }
                row++
            }

            if (!showAll && count > INITIAL_GRID_COUNT) {
                LoadMoreButton(title = "もっと見る", accent = accent) { showAll = true }
            }
        }
    }
}

/**
 * 2列グリッドの各セルを 1 行 2 セルで LazyColumn に流し込むヘルパー（もっと見る無し・全件表示用）。
 * 温泉セクションで使用する。
 */
private fun androidx.compose.foundation.lazy.LazyListScope.gridItems(
    count: Int,
    card: @Composable (index: Int) -> Unit,
) {
    val rows = (count + 1) / 2
    items(rows) { row ->
        val left = row * 2
        val right = left + 1
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) { card(left) }
            Box(modifier = Modifier.weight(1f)) {
                if (right < count) card(right)
            }
        }
    }
}

/**
 * セクション見出し（アクセントバー + アイコンチップ + タイトル + サブタイトル）。
 * iOS 版 RichSectionHeader を再現。
 */
@Composable
private fun RichSectionHeader(
    icon: ImageVector,
    title: String,
    accent: Color,
    subtitle: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // アクセントバー
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Spacer(modifier = Modifier.width(12.dp))
        // アイコンチップ
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

/** 「もっと見る」ボタン（アクセント色ソフト塗り）。iOS 版 LoadMoreButton を再現。 */
@Composable
private fun LoadMoreButton(title: String, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.width(6.dp))
        Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

/** 観光地リストの横スクロールミニカード。iOS 版の attraction カードを再現。 */
@Composable
private fun AttractionMiniCard(name: String, description: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, AttractionAccent.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xCC0A84FF), Color(0x99AF52DE)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            description,
            fontSize = 12.sp,
            color = Color.Gray,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * グルメ／温泉／お土産で共通の 2列リッチカード。
 * グラデーション円アイコン + 5つ星 + タイトル + 説明 + 価格/時期/カテゴリタグ + グラデーション枠。
 * iOS 版 GourmetItemCard / OnsenCard / SouvenirItemCard を再現。
 */
@Composable
private fun RichItemCard(
    name: String,
    description: String,
    accent: Color,
    icon: ImageVector,
    tagLabel: String,
    popularity: Int,
    price: String?,
    season: String?,
    onClick: () -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(
                1.dp,
                Brush.linearGradient(listOf(accent.copy(alpha = 0.3f), accent.copy(alpha = 0.1f))),
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // トップ: グラデーション円アイコン + 星 + 追加ボタン
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(accent.copy(alpha = 0.8f), accent)),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { i ->
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = if (i < popularity) Color(0xFFFFC107) else Color(0x4D9E9E9E),
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }

        // タイトル + 説明
        Text(
            name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            description,
            fontSize = 12.sp,
            color = Color.Gray,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.weight(1f))

        // ボトム: 価格 / 時期 + カテゴリタグ
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!price.isNullOrBlank()) {
                Text("💰 $price", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!season.isNullOrBlank()) {
                    Text(
                        "📅 $season",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // カテゴリ/タイプ タグ（カプセル）
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(tagLabel, fontSize = 11.sp, color = accent, maxLines = 1)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Filled.AddCircle,
                    contentDescription = "プランに追加",
                    tint = accent,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable(onClick = onAdd),
                )
            }
        }
    }
}

/** フォトギャラリーの 1 枚（写真 + スポット名）。 */
@Composable
private fun PhotoCard(spotName: String, resName: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val photoId = context.resources.getIdentifier(resName, "drawable", context.packageName)
    if (photoId == 0) return
    Column(
        modifier = Modifier
            .width(260.dp)
            .appCard()
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(id = photoId),
            contentDescription = spotName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
        Text(
            text = spotName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
