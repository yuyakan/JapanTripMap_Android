package com.example.japantripmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 祭りカテゴリ -> 色（iOS 版 IntegratedFestivalCategory.color に対応）。 */
private val FESTIVAL_CATEGORY_COLORS = mapOf(
    "summer" to Color(0xFFED7321),
    "fireworks" to Color(0xFF8C61C7),
    "traditional" to Color(0xFF996B47),
    "dance" to Color(0xFFDB5994),
    "food" to Color(0xFFD6454D),
    "seasonal" to Color(0xFF3D9E66),
    "religious" to Color(0xFF5C50A6),
    "spring" to Color(0xFF66B366),
    "autumn" to Color(0xFFCC7A29),
    "winter" to Color(0xFF2980D9),
    "sakura" to Color(0xFFE87DA8),
    "illumination" to Color(0xFFD9A521),
    "snow" to Color(0xFF56A0C7),
)

private val FESTIVAL_CATEGORY_LABELS = mapOf(
    "summer" to "夏祭り", "fireworks" to "花火", "traditional" to "伝統",
    "dance" to "踊り", "food" to "グルメ", "seasonal" to "季節", "religious" to "宗教",
    "spring" to "春", "autumn" to "秋", "winter" to "冬",
    "sakura" to "桜", "illumination" to "イルミ", "snow" to "雪",
)

/** カテゴリ -> アイコン（iOS 版 IntegratedFestivalCategory.icon の SF Symbol 相当）。 */
private val FESTIVAL_CATEGORY_ICONS: Map<String, ImageVector> = mapOf(
    "summer" to Icons.Filled.WbSunny,
    "fireworks" to Icons.Filled.Celebration,
    "traditional" to Icons.Filled.AccountBalance,
    "dance" to Icons.Filled.MusicNote,
    "food" to Icons.Filled.Restaurant,
    "seasonal" to Icons.Filled.Park,
    "religious" to Icons.Filled.AccountBalance,
    "spring" to Icons.Filled.Park,
    "autumn" to Icons.Filled.Park,
    "winter" to Icons.Filled.AcUnit,
    "sakura" to Icons.Filled.LocalFlorist,
    "illumination" to Icons.Filled.AutoAwesome,
    "snow" to Icons.Filled.AcUnit,
)

// プラン詳細のルーター（PlanItemDetail.kt）からも祭りの色/ラベル/アイコンを再構築するため internal。
internal fun festColor(c: String) = FESTIVAL_CATEGORY_COLORS[c] ?: Color.Gray
internal fun festLabel(c: String) = FESTIVAL_CATEGORY_LABELS[c] ?: c
internal fun festIcon(c: String) = FESTIVAL_CATEGORY_ICONS[c] ?: Icons.Filled.Celebration

/** ヘッダー土台のブランドグラデーション（iOS の brandGradient オレンジ→コーラル相当）。 */
private val BrandGradient = Brush.horizontalGradient(listOf(Color(0xFFFF9500), Color(0xFFFF3B30)))

/**
 * 祭りの月表記（例: "8月", "7-8月", "4-5月"）に、指定月（"1"〜"12"）が含まれるか。
 * 範囲表記は開始〜終了の各月にマッチさせる。
 */
private fun monthMatches(monthText: String, target: String): Boolean {
    val t = target.toIntOrNull() ?: return false
    // 表記から数字だけを取り出す（"7-8月" -> [7, 8]）。
    val nums = Regex("\\d+").findAll(monthText).mapNotNull { it.value.toIntOrNull() }.toList()
    return when {
        nums.isEmpty() -> false
        nums.size == 1 -> nums[0] == t
        // 2 つ以上あるときは最初と最後を範囲とみなす。
        else -> {
            val lo = nums.first()
            val hi = nums.last()
            if (lo <= hi) t in lo..hi else (t >= lo || t <= hi) // 年跨ぎ（例: 12-2月）
        }
    }
}

/** 県情報付きの祭り（一覧表示用）。 */
private data class FestivalWithPref(val prefecture: Prefecture, val festival: Festival)

/** 全祭りを県情報付きでフラット化（規模の大きい順）。 */
private val ALL_FESTIVALS: List<FestivalWithPref> by lazy {
    Prefecture.entries.flatMap { pref ->
        pref.festivals.map { FestivalWithPref(pref, it) }
    }.sortedByDescending { it.festival.scale }
}

/**
 * 祭り・イベントタブ。iOS 版 AllFestivalsComparisonView を移植。
 * 全国の祭りをカテゴリ・月・規模で絞り込みつつ 2 列グリッドで表示する。
 */
@Composable
fun FestivalScreen(
    modifier: Modifier = Modifier,
    onOpenSpot: (SpotDetail) -> Unit = {},
) {
    // 選択中カテゴリ（null なら全部）。
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    // 選択中の月（"1"〜"12"。null なら全部）。
    var selectedMonth by remember { mutableStateOf<String?>(null) }
    // 最低規模（★以上。null なら全部）。
    var minScale by remember { mutableStateOf<Int?>(null) }
    // キーワード検索。
    var searchText by remember { mutableStateOf("") }

    // 実際に存在するカテゴリだけをフィルタ候補にする。
    val categories = remember { ALL_FESTIVALS.map { it.festival.category }.distinct() }

    val filtered = remember(selectedCategory, selectedMonth, minScale, searchText) {
        ALL_FESTIVALS.filter { fw ->
            val f = fw.festival
            (selectedCategory == null || f.category == selectedCategory) &&
                (selectedMonth == null || monthMatches(f.month, selectedMonth!!)) &&
                (minScale == null || f.scale >= minScale!!) &&
                (searchText.isBlank() ||
                    f.name.contains(searchText, true) ||
                    f.description.contains(searchText, true) ||
                    fw.prefecture.displayName.contains(searchText, true))
        }
    }

    Column(modifier = modifier.fillMaxSize().background(AppTheme.Background)) {
        // ── ブランドグラデーション土台のヘッダー（白カプセル検索バー） ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandGradient)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
        ) {
            SearchBar(
                value = searchText,
                onValueChange = { searchText = it },
            )
        }

        // ── カテゴリフィルタ（横スクロール・アイコン付き丸ボタン） ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterButton("すべて", selectedCategory == null, Color(0xFF8A8A8E)) { selectedCategory = null }
            categories.forEach { cat ->
                FilterButton(festLabel(cat), selectedCategory == cat, festColor(cat), icon = festIcon(cat)) {
                    selectedCategory = if (selectedCategory == cat) null else cat
                }
            }
        }

        // ── 規模フィルタ＋月フィルタ（横スクロール） ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(3, 4, 5).forEach { s ->
                FilterButton("★$s 以上", minScale == s, Color(0xFFFFA000), icon = Icons.Filled.Star) {
                    minScale = if (minScale == s) null else s
                }
            }
            // 区切り。
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .height(20.dp)
                    .width(1.dp)
                    .background(AppTheme.Hairline),
            )
            FilterButton("全期間", selectedMonth == null, Color(0xFF4285F4)) { selectedMonth = null }
            (1..12).forEach { m ->
                val key = m.toString()
                FilterButton("${m}月", selectedMonth == key, Color(0xFF4285F4)) {
                    selectedMonth = if (selectedMonth == key) null else key
                }
            }
        }

        // ── 2 列グリッドのリッチカード ──
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(filtered) { fw ->
                FestivalCard(fw, onClick = {
                    onOpenSpot(
                        SpotDetail(
                            title = fw.festival.name,
                            prefecture = fw.prefecture,
                            accent = festColor(fw.festival.category),
                            icon = festIcon(fw.festival.category),
                            description = fw.festival.description,
                            planCategory = PlanItemCategory.FESTIVAL,
                            latitude = fw.festival.latitude,
                            longitude = fw.festival.longitude,
                            badge = festLabel(fw.festival.category),
                            popularity = fw.festival.scale,
                            infoRows = listOf(
                                "開催地" to "${fw.prefecture.displayName}・${fw.festival.location}",
                                "時期" to fw.festival.month,
                                "期間" to fw.festival.duration,
                            ),
                            features = fw.festival.features,
                        ),
                    )
                })
            }
        }
    }
}

/** iOS 版の白カプセル型検索バー（虫眼鏡＋クリアボタン）。 */
@Composable
private fun SearchBar(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = Color(0xFFFF6B2C),
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("祭り名・県名で検索", fontSize = 15.sp, color = Color(0xFFB0B0B5))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = AppTheme.TextPrimary),
                cursorBrush = SolidColor(Color(0xFFFF6B2C)),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Filled.Cancel,
                contentDescription = "クリア",
                tint = Color(0xFFC7C7CC),
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onValueChange("") },
            )
        }
    }
}

/** アイコン付き丸フィルタボタン（iOS 版 FilterButton 相当）。 */
@Composable
private fun FilterButton(
    label: String,
    selected: Boolean,
    color: Color,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) color else Color(0xFFF2F2F5))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else color,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else AppTheme.TextPrimary,
        )
    }
}

/** iOS 版 IntegratedFestivalCard 相当の 2 列グリッド用リッチカード。 */
@Composable
private fun FestivalCard(item: FestivalWithPref, onClick: () -> Unit) {
    val f = item.festival
    val accent = festColor(f.category)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.CardSurface)
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        // ── ヘッダー: 丸アイコン + カテゴリ名/★ + 県バッジ ──
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.85f), accent))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(festIcon(f.category), null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(festLabel(f.category), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = accent)
                Row {
                    repeat(5) { i ->
                        Icon(
                            Icons.Filled.Star,
                            null,
                            tint = if (i < f.scale) Color(0xFFFFA000) else Color(0xFFE0E0E0),
                            modifier = Modifier.size(9.dp),
                        )
                    }
                }
            }
            // 県バッジ。
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color(0xFFEFEFF2))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    item.prefecture.displayName,
                    fontSize = 10.sp,
                    color = AppTheme.TextSecondary,
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── 祭り名 ──
        Text(
            f.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppTheme.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ── 説明 ──
        Text(
            f.description,
            fontSize = 11.sp,
            color = AppTheme.TextSecondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.weight(1f))

        // ── 詳細情報: 時期 / 期間 / 場所 ──
        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Celebration, null, tint = Color(0xFF4285F4), modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(f.month, fontSize = 10.sp, color = Color(0xFF4285F4), maxLines = 1)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Schedule, null, tint = Color(0xFF3D9E66), modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(f.duration, fontSize = 10.sp, color = Color(0xFF3D9E66), maxLines = 1)
        }
        Row(modifier = Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Place, null, tint = Color(0xFFD6454D), modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                f.location,
                fontSize = 10.sp,
                color = Color(0xFFD6454D),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // ── 特徴タグ（横スクロール、最大 3 件） ──
        if (f.features.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                f.features.take(3).forEach { feat ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(accent.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(feat, fontSize = 9.sp, color = accent, maxLines = 1)
                    }
                }
            }
        }
    }
}
