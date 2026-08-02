package com.example.japantripmap

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

private fun festColor(c: String) = FESTIVAL_CATEGORY_COLORS[c] ?: Color.Gray
private fun festLabel(c: String) = FESTIVAL_CATEGORY_LABELS[c] ?: c

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
 * 全国の祭りをカテゴリで絞り込みつつ一覧表示する。
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

    Column(modifier = modifier.fillMaxSize().background(Color(0xFFF7F5FA))) {
        // ヘッダー。
        Text(
            text = "全国の祭り・イベント",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
        Text(
            text = "${filtered.size} 件",
            fontSize = 13.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
        )

        // キーワード検索。
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text("祭り名・県名で検索", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
        )

        // カテゴリフィルタ（横スクロール）。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip("すべて", selectedCategory == null, Color(0xFF666666)) { selectedCategory = null }
            categories.forEach { cat ->
                FilterChip(festLabel(cat), selectedCategory == cat, festColor(cat)) {
                    selectedCategory = if (selectedCategory == cat) null else cat
                }
            }
        }

        // 月フィルタ（横スクロール）。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip("全期間", selectedMonth == null, Color(0xFF666666)) { selectedMonth = null }
            (1..12).forEach { m ->
                val key = m.toString()
                FilterChip("${m}月", selectedMonth == key, Color(0xFF4285F4)) {
                    selectedMonth = if (selectedMonth == key) null else key
                }
            }
        }

        // 規模フィルタ。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip("規模すべて", minScale == null, Color(0xFF666666)) { minScale = null }
            listOf(3, 4, 5).forEach { s ->
                FilterChip("★$s 以上", minScale == s, Color(0xFFFFA000)) {
                    minScale = if (minScale == s) null else s
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(filtered.size) { i ->
                val fw = filtered[i]
                FestivalCard(fw, onClick = {
                    onOpenSpot(
                        SpotDetail(
                            title = fw.festival.name,
                            prefecture = fw.prefecture,
                            accent = festColor(fw.festival.category),
                            icon = Icons.Filled.Celebration,
                            description = fw.festival.description,
                            planCategory = PlanItemCategory.FESTIVAL,
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

@Composable
private fun FilterChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) color else color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else color,
        )
    }
}

@Composable
private fun FestivalCard(item: FestivalWithPref, onClick: () -> Unit) {
    val f = item.festival
    val accent = festColor(f.category)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(f.name, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(festLabel(f.category), fontSize = 11.sp, color = accent)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(f.scale) {
                    Icon(Icons.Filled.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(11.dp))
                }
            }
        }
        // 県・場所・時期。
        Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Place, null, tint = accent, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${item.prefecture.displayName}・${f.location}",
                fontSize = 12.sp,
                color = Color(0xFF666666),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text("${f.month}・${f.duration}", fontSize = 12.sp, color = Color.Gray)
        }
        Text(
            text = f.description,
            fontSize = 13.sp,
            color = Color(0xFF555555),
            modifier = Modifier.padding(top = 6.dp),
        )
        // 特徴タグ。
        if (f.features.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                f.features.forEach { feat ->
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(feat, fontSize = 11.sp, color = Color(0xFF777777))
                    }
                }
            }
        }
    }
}
