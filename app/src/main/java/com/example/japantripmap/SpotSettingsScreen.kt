package com.example.japantripmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AccentOrange = Color(0xFFFF9500)
private val AccentRed = Color(0xFFE53935)
private val AccentGray = Color(0xFF8A8A8E)

/** 温泉設定のグルーピング方法。iOS 版 GroupingMode に対応（"地方別"/"タイプ別"）。 */
private enum class SpotGrouping(val label: String, val icon: ImageVector) {
    REGION("地方別", Icons.Filled.Map),
    TYPE("タイプ別", Icons.Filled.Tag),
}

/**
 * 温泉・自然の「スポット単位」設定画面。ボトムシート内に表示する。
 * iOS 版 OnsenSettingsView / NatureSpotSettingsView を忠実に移植。
 *
 * ヘッダー: 左に「全選択」「全解除(赤)」、右に「完了」（iOS のツールバー配置に合わせる）。
 * リスト順: ①表示設定（温泉=地方別/タイプ別 切替 / 自然=表示アイコングリッド）
 *          ②重み設定への導線行 ③スポット選択行（地方別/タイプ別）。
 * 重みリセットは iOS 同様、重み設定サブ画面の左上に置く（メイン画面には置かない）。
 */
@Composable
fun SpotSettingsScreen(
    viewModel: SpotRouletteViewModel,
    onDone: () -> Unit,
) {
    // 重み設定サブ画面の表示状態。
    var showWeights by remember { mutableStateOf(false) }
    val accent = AccentOrange // iOS の温泉/自然設定はオレンジ基調。

    if (showWeights) {
        SpotWeightSettingsScreen(viewModel = viewModel, onBack = { showWeights = false })
        return
    }

    val enabled = viewModel.enabledSpots
    var grouping by remember { mutableStateOf(SpotGrouping.REGION) }

    val sections: List<SpotSection> = remember(viewModel.kind, grouping, viewModel.selectedDisplayTypes) {
        buildSections(viewModel, grouping)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ヘッダー（iOS: 左=全選択/全解除, 右=完了）。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { viewModel.selectAll() }) {
                Text("全選択", fontSize = 16.sp, color = accent)
            }
            TextButton(onClick = { viewModel.deselectAll() }) {
                Text("全解除", fontSize = 16.sp, color = AccentRed)
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onDone) {
                Text("完了", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = accent)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ① 表示設定。iOS: display_settings（温泉=グルーピング / 自然=表示アイコン）。
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().appCard(corner = 16.dp).padding(14.dp),
                ) {
                    Text("表示設定", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    if (viewModel.kind == SpotKind.ONSEN) {
                        // 地方別 / タイプ別 の切替。
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            SpotGrouping.entries.forEach { mode ->
                                val selected = grouping == mode
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(if (selected) accent else accent.copy(alpha = 0.10f))
                                        .clickable { grouping = mode }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(mode.icon, contentDescription = null, tint = if (selected) Color.White else accent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(mode.label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (selected) Color.White else accent)
                                }
                            }
                        }
                    } else {
                        // 自然: 表示アイコン（タイプ）選択グリッド。
                        DisplayTypeGrid(viewModel = viewModel)
                    }
                }
            }

            // ② 重み設定への導線。iOS: OnsenWeightSettingsButtonView。
            item {
                WeightSettingsRow(accent = accent) { showWeights = true }
            }

            // ③ スポット選択セクション（見出し「N箇所」＋行）。
            sections.forEach { section ->
                item(key = "sec_${section.title}") {
                    SectionHeader(
                        icon = section.icon,
                        iconTint = section.iconTint,
                        title = section.title,
                        countText = "${section.spots.size}箇所",
                    )
                }
                items(section.spots, key = { it.id }) { spot ->
                    SpotSettingRow(
                        spot = spot,
                        isEnabled = spot in enabled,
                        weight = viewModel.weightOf(spot),
                        onToggle = { viewModel.toggleSpot(spot) },
                    )
                }
            }
        }
    }
}

/** グルーピング後の 1 セクション。 */
private data class SpotSection(
    val title: String,
    val icon: ImageVector?,
    val iconTint: Color?,
    val spots: List<RouletteSpot>,
)

/** 温泉のグルーピング設定 or 自然の表示タイプに応じてセクション一覧を組む。 */
private fun buildSections(viewModel: SpotRouletteViewModel, grouping: SpotGrouping): List<SpotSection> {
    return when (viewModel.kind) {
        SpotKind.ONSEN -> when (grouping) {
            SpotGrouping.REGION ->
                SpotRepository.groupedByRegion(viewModel.allSpots).map { (region, spots) ->
                    SpotSection(region, null, null, spots)
                }
            SpotGrouping.TYPE ->
                SpotRepository.groupedByType(viewModel.kind, viewModel.allSpots).map { (meta, spots) ->
                    SpotSection(meta.name, meta.icon, meta.color, spots)
                }
        }
        // 自然は iOS と同じくタイプ別固定。表示タイプで絞り込む。
        SpotKind.NATURE -> {
            val filtered = viewModel.allSpots.filter { it.typeKey in viewModel.selectedDisplayTypes }
            SpotRepository.groupedByType(viewModel.kind, filtered).map { (meta, spots) ->
                SpotSection(meta.name, meta.icon, meta.color, spots)
            }
        }
    }
}

/** 重み設定サブ画面へ進む行。iOS の OnsenWeightSettingsButtonView に対応。 */
@Composable
private fun WeightSettingsRow(accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appCard(corner = 16.dp)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Scale, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("重み付け設定", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = AppTheme.TextPrimary)
            Text("スポットごとの選択確率を調整", fontSize = 12.sp, color = AppTheme.TextSecondary)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AppTheme.TextSecondary, modifier = Modifier.size(18.dp))
    }
}

/** 自然タブの表示タイプ（アイコン）選択グリッド。iOS の DisplayIconSelectionView に対応。 */
@Composable
private fun DisplayTypeGrid(viewModel: SpotRouletteViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val types = viewModel.allTypes
        types.chunked(2).forEach { rowTypes ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowTypes.forEach { meta ->
                    val selected = meta.key in viewModel.selectedDisplayTypes
                    val count = viewModel.allSpots.count { it.typeKey == meta.key }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) meta.color.copy(alpha = 0.12f) else AppTheme.Hairline.copy(alpha = 0.4f))
                            .clickable { viewModel.toggleDisplayType(meta.key) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(if (selected) meta.color else AccentGray.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(meta.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meta.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (selected) AppTheme.TextPrimary else AppTheme.TextSecondary)
                            Text("${count}箇所", fontSize = 11.sp, color = AppTheme.TextSecondary)
                        }
                        Icon(
                            imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = null,
                            tint = if (selected) meta.color else AccentGray.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                if (rowTypes.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** 地方／タイプ見出し行。iOS: 左に地方/タイプ名、右に「N箇所」（オレンジ）。 */
@Composable
private fun SectionHeader(
    icon: ImageVector?,
    iconTint: Color?,
    title: String,
    countText: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = iconTint ?: AccentOrange, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextPrimary)
        Spacer(modifier = Modifier.weight(1f))
        Text(countText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AccentOrange)
    }
}

/** スポット 1 件の ON/OFF 行（タイプタグ・人気度・重みバッジ付き）。iOS の OnsenRow / NatureSpotRow に対応。 */
@Composable
private fun SpotSettingRow(
    spot: RouletteSpot,
    isEnabled: Boolean,
    weight: Double,
    onToggle: () -> Unit,
) {
    val meta = spot.typeMeta
    // iOS: 温泉行のチェックはオレンジ、自然行のチェックはスポットのタイプ色。
    val checkColor = if (spot.kind == SpotKind.ONSEN) AccentOrange else meta.color
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appCard(corner = 14.dp)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // iOS: 有効=checkmark.circle.fill, 無効=circle(グレー)。
        Icon(
            imageVector = if (isEnabled) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (isEnabled) "対象" else "対象外",
            tint = if (isEnabled) checkColor else Color(0xFFC7C7CC),
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                spot.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isEnabled) AppTheme.TextPrimary else AppTheme.TextSecondary,
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                Icon(meta.icon, contentDescription = null, tint = meta.color, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(meta.name, fontSize = 11.sp, color = meta.color)
                Spacer(modifier = Modifier.width(8.dp))
                repeat(spot.popularity) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(10.dp))
                }
            }
            Text(
                spot.description,
                fontSize = 12.sp,
                color = AppTheme.TextSecondary,
                maxLines = 2,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        // iOS: 有効なときだけ「×N.N」（オレンジ）。
        if (isEnabled) {
            Spacer(modifier = Modifier.width(8.dp))
            Badge(text = "×%.1f".format(weight), color = AccentOrange)
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

/**
 * 重み設定サブ画面。iOS の OnsenWeightSettingsView / NatureSpotWeightSettingsView を移植。
 * ヘッダー: 左「リセット(赤)」＋タイトル「重み付け調整」＋右「完了(=戻る)」。
 * 有効スポットのみをタイプ別にグループ化し、スライダー・±ボタン・確率を表示する。
 */
@Composable
private fun SpotWeightSettingsScreen(
    viewModel: SpotRouletteViewModel,
    onBack: () -> Unit,
) {
    val sections = remember(viewModel.enabledSpots) {
        SpotRepository.groupedByType(viewModel.kind, viewModel.enabledSpots.toList())
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ヘッダー（iOS: 左=リセット(赤), 右=完了）。戻る矢印も付けて設定へ帰れるようにする。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { viewModel.resetWeights() }) {
                Text("リセット", fontSize = 16.sp, color = AccentRed)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("重み付け調整", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) {
                Text("完了", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AccentOrange)
            }
        }
        // 戻る導線（サブ画面のためボトムシートを閉じずに設定へ戻す）。
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "設定へ戻る", tint = AccentOrange, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("設定", fontSize = 14.sp, color = AccentOrange)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // iOS: 冒頭の説明セクション。
            item {
                Column(modifier = Modifier.fillMaxWidth().appCard(corner = 16.dp).padding(14.dp)) {
                    Text("重み付け設定", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "各スポットの選択される確率を調整できます。数値が大きいほど選ばれやすくなります。",
                        fontSize = 12.sp,
                        color = AppTheme.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            if (sections.isEmpty()) {
                item {
                    Text(
                        "有効なスポットがありません。設定で対象を追加してください。",
                        fontSize = 13.sp,
                        color = AppTheme.TextSecondary,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            sections.forEach { (meta, spots) ->
                item(key = "wsec_${meta.key}") {
                    SectionHeader(icon = meta.icon, iconTint = meta.color, title = meta.name, countText = "${spots.size}箇所")
                }
                items(spots, key = { it.id }) { spot ->
                    WeightSliderRow(
                        spot = spot,
                        weight = viewModel.weightOf(spot),
                        probability = viewModel.probabilityOf(spot),
                        onDecrease = { viewModel.adjustWeight(spot, -0.5) },
                        onIncrease = { viewModel.adjustWeight(spot, 0.5) },
                        onWeightChange = { viewModel.setWeight(spot, it) },
                    )
                }
            }
        }
    }
}

/** 重みスライダー行。iOS の OnsenWeightSliderRow / NatureSpotWeightSliderRow に対応。 */
@Composable
private fun WeightSliderRow(
    spot: RouletteSpot,
    weight: Double,
    probability: Double,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onWeightChange: (Double) -> Unit,
) {
    val meta = spot.typeMeta
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appCard(corner = 14.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        // 上段: 名前＋タイプ＋星（左）と 確率%（右）。iOS 同様。
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(spot.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                    Icon(meta.icon, contentDescription = null, tint = meta.color, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(meta.name, fontSize = 11.sp, color = meta.color)
                    Spacer(modifier = Modifier.width(8.dp))
                    repeat(spot.popularity) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(10.dp))
                    }
                }
            }
            Badge(text = "%.1f%%".format(probability), color = AccentOrange)
        }

        // 中段: 「重み: N.N」（左）と ± ボタン（右）。iOS 同様。
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Text("重み: %.1f".format(weight), fontSize = 12.sp, color = AppTheme.TextSecondary)
            Spacer(modifier = Modifier.weight(1f))
            QuickButton("−", AccentRed, onDecrease)
            Spacer(modifier = Modifier.width(6.dp))
            QuickButton("＋", Color(0xFF33A06B), onIncrease)
        }

        // スライダー（0.1〜10.0）。iOS はタイプ色をアクセントにする。
        Slider(
            value = weight.toFloat(),
            onValueChange = { onWeightChange(it.toDouble()) },
            valueRange = WeightManager.MIN_WEIGHT.toFloat()..WeightManager.MAX_WEIGHT.toFloat(),
            steps = 98,
            colors = SliderDefaults.colors(thumbColor = meta.color, activeTrackColor = meta.color),
        )
    }
}

@Composable
private fun QuickButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
