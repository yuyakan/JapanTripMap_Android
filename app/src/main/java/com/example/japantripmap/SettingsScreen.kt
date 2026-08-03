package com.example.japantripmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.filled.Scale
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AccentGreen = Color(0xFF33A06B)
private val AccentRed = Color(0xFFE53935)

/**
 * 都道府県ルーレット設定画面。iOS 版 SettingsView + WeightSettingsView を移植し、
 * 温泉・自然の SpotSettingsScreen と同じ構成に統一。
 *
 * ヘッダー: 左「全選択」「全解除(赤)」、右「完了」。
 * リスト順: ①表示設定（タイプフィルタがある場合のみ）②重み付け設定への導線 ③地方別の県 ON/OFF 行。
 * 重み付け調整は別画面（WeightAdjustScreen）に分離し、そこでスライダー・±・確率を扱う。
 */
@Composable
fun SettingsScreen(
    viewModel: RouletteViewModel,
    onDone: () -> Unit,
) {
    // 重み付け調整サブ画面の表示状態。
    var showWeights by remember { mutableStateOf(false) }
    val accent = AccentGreen // 都道府県は緑基調（温泉・自然はオレンジ）。

    if (showWeights) {
        WeightAdjustScreen(viewModel = viewModel, accent = accent, onBack = { showWeights = false })
        return
    }

    val enabled = viewModel.enabledPrefectures
    val grouped = remember(viewModel.mode) { Prefecture.groupedByRegion(viewModel.availablePrefectures) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ヘッダー（左=全選択/全解除, 右=完了）。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { viewModel.selectAllPrefectures() }) {
                Text("全選択", fontSize = 16.sp, color = accent)
            }
            TextButton(onClick = { viewModel.deselectAllPrefectures() }) {
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
            // ① 表示設定（タイプフィルタ）。TOURISM モードでは availableTypes が空なので出ない。
            if (viewModel.availableTypes.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().appCard(corner = 16.dp).padding(14.dp),
                    ) {
                        Text("表示設定", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            viewModel.availableTypes.forEach { type ->
                                val selected = type in viewModel.selectedTypes
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selected) accent else accent.copy(alpha = 0.12f))
                                        .clickable { viewModel.toggleType(type) }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                ) {
                                    Text(
                                        text = viewModel.typeLabels[type] ?: type,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selected) Color.White else accent,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ② 重み付け設定への導線。
            item {
                WeightSettingsRow(accent = accent) { showWeights = true }
            }

            // ③ 地方別の県 ON/OFF セクション。
            grouped.forEach { (regionName, prefectures) ->
                item(key = "region_$regionName") {
                    SectionHeader(
                        title = regionName,
                        countText = "${prefectures.size}県",
                        accent = accent,
                    )
                }
                items(prefectures, key = { it.name }) { prefecture ->
                    PrefectureSettingRow(
                        prefecture = prefecture,
                        isEnabled = prefecture in enabled,
                        weight = viewModel.weights[prefecture] ?: WeightManager.DEFAULT_WEIGHT,
                        accent = accent,
                        onToggle = { viewModel.togglePrefecture(prefecture) },
                    )
                }
            }
        }
    }
}

/** 重み付け設定サブ画面へ進む行。SpotSettingsScreen の WeightSettingsRow と同一デザイン。 */
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
            Text("都道府県ごとの選択確率を調整", fontSize = 12.sp, color = AppTheme.TextSecondary)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AppTheme.TextSecondary, modifier = Modifier.size(18.dp))
    }
}

/** 地方見出し行（左に地方名、右に「N県」）。 */
@Composable
private fun SectionHeader(title: String, countText: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextPrimary)
        Spacer(modifier = Modifier.weight(1f))
        Text(countText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent)
    }
}

/** 県 1 件の ON/OFF 行（チェック＋重みバッジ）。SpotSettingRow と同一デザイン。スライダーは別画面。 */
@Composable
private fun PrefectureSettingRow(
    prefecture: Prefecture,
    isEnabled: Boolean,
    weight: Double,
    accent: Color,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appCard(corner = 14.dp)
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isEnabled) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = if (isEnabled) "対象" else "対象外",
            tint = if (isEnabled) accent else Color(0xFFC7C7CC),
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = prefecture.displayName,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isEnabled) AppTheme.TextPrimary else AppTheme.TextSecondary,
            modifier = Modifier.weight(1f),
        )
        if (isEnabled) {
            Badge(text = "×%.1f".format(weight), color = accent)
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
 * 重み付け調整画面（別画面）。iOS 版 WeightSettingsView を移植。
 * ヘッダー: 左「リセット(赤)」＋「重み付け調整」＋右「完了(=戻る)」。
 * 有効な県のみを地方別にグループ化し、スライダー・±ボタン・確率を表示する。
 */
@Composable
private fun WeightAdjustScreen(
    viewModel: RouletteViewModel,
    accent: Color,
    onBack: () -> Unit,
) {
    // 有効な県のみ地方別グルーピング。
    val grouped = remember(viewModel.enabledPrefectures) {
        Prefecture.groupedByRegion(viewModel.availablePrefectures.filter { it in viewModel.enabledPrefectures })
    }

    Column(modifier = Modifier.fillMaxWidth()) {
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
                Text("完了", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = accent)
            }
        }
        // 設定へ戻る導線。
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "設定へ戻る", tint = accent, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("設定", fontSize = 14.sp, color = accent)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth().appCard(corner = 16.dp).padding(14.dp)) {
                    Text("重み付け設定", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "各都道府県の選択される確率を調整できます。数値が大きいほど選ばれやすくなります。",
                        fontSize = 12.sp,
                        color = AppTheme.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            if (grouped.isEmpty()) {
                item {
                    Text(
                        "対象の都道府県がありません。設定で対象を追加してください。",
                        fontSize = 13.sp,
                        color = AppTheme.TextSecondary,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            grouped.forEach { (regionName, prefectures) ->
                item(key = "wregion_$regionName") {
                    SectionHeader(title = regionName, countText = "${prefectures.size}県", accent = accent)
                }
                items(prefectures, key = { it.name }) { prefecture ->
                    WeightSliderRow(
                        prefecture = prefecture,
                        weight = viewModel.weights[prefecture] ?: WeightManager.DEFAULT_WEIGHT,
                        probability = viewModel.probabilityOf(prefecture),
                        accent = accent,
                        onDecrease = {
                            val cur = viewModel.weights[prefecture] ?: WeightManager.DEFAULT_WEIGHT
                            viewModel.setWeight(prefecture, (cur - 0.5).coerceIn(WeightManager.MIN_WEIGHT, WeightManager.MAX_WEIGHT))
                        },
                        onIncrease = {
                            val cur = viewModel.weights[prefecture] ?: WeightManager.DEFAULT_WEIGHT
                            viewModel.setWeight(prefecture, (cur + 0.5).coerceIn(WeightManager.MIN_WEIGHT, WeightManager.MAX_WEIGHT))
                        },
                        onWeightChange = { viewModel.setWeight(prefecture, it) },
                    )
                }
            }
        }
    }
}

/** 重みスライダー行。SpotSettingsScreen の WeightSliderRow と同一デザイン。 */
@Composable
private fun WeightSliderRow(
    prefecture: Prefecture,
    weight: Double,
    probability: Double,
    accent: Color,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onWeightChange: (Double) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appCard(corner = 14.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(prefecture.displayName, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Badge(text = "%.1f%%".format(probability), color = accent)
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Text("重み: %.1f".format(weight), fontSize = 12.sp, color = AppTheme.TextSecondary)
            Spacer(modifier = Modifier.weight(1f))
            QuickButton("−", AccentRed, onDecrease)
            Spacer(modifier = Modifier.width(6.dp))
            QuickButton("＋", AccentGreen, onIncrease)
        }

        Slider(
            value = weight.toFloat(),
            onValueChange = { onWeightChange(it.toDouble()) },
            valueRange = WeightManager.MIN_WEIGHT.toFloat()..WeightManager.MAX_WEIGHT.toFloat(),
            steps = 98,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
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
