package com.example.japantripmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AccentBlue = Color(0xFF4285F4)
private val AccentGreen = Color(0xFF33A06B)

/**
 * ルーレット設定画面。iOS 版 SettingsView + WeightSettingsView を統合したもの。
 * - 全選択 / 全解除
 * - 地方ごとの都道府県一覧（ON/OFF・重みスライダー・確率表示）
 * - 重みリセット
 */
@Composable
fun SettingsScreen(
    viewModel: RouletteViewModel,
    onDone: () -> Unit,
) {
    val enabled = viewModel.enabledPrefectures
    // このモードで選べる県だけを地方ごとにグルーピング（温泉モードなら温泉県のみ）。
    val grouped = remember(viewModel.mode) { Prefecture.groupedByRegion(viewModel.availablePrefectures) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ヘッダー（全選択 / 全解除 / 完了）。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { viewModel.selectAllPrefectures() }) {
                Text("全選択")
            }
            TextButton(onClick = { viewModel.deselectAllPrefectures() }) {
                Text("全解除", color = Color(0xFFE53935))
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { viewModel.resetWeights() }) {
                Text("重みリセット", color = Color(0xFFE53935))
            }
            TextButton(onClick = onDone) {
                Text("完了", fontWeight = FontWeight.SemiBold)
            }
        }

        HorizontalDivider()

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                Text(
                    text = "対象: ${viewModel.effectiveEnabled.size} 都道府県",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // タイプフィルタ（温泉/自然モードのみ）。
            if (viewModel.availableTypes.isNotEmpty()) {
                item {
                    Text(
                        text = "タイプで絞り込み",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        viewModel.availableTypes.forEach { type ->
                            val selected = type in viewModel.selectedTypes
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selected) AccentBlue else AccentBlue.copy(alpha = 0.12f))
                                    .clickable { viewModel.toggleType(type) }
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = viewModel.typeLabels[type] ?: type,
                                    fontSize = 13.sp,
                                    color = if (selected) Color.White else AccentBlue,
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }

            grouped.forEach { (regionName, prefectures) ->
                item(key = "region_$regionName") {
                    Text(
                        text = regionName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF7F7F9))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
                items(prefectures, key = { it.name }) { prefecture ->
                    PrefectureSettingRow(
                        prefecture = prefecture,
                        isEnabled = prefecture in enabled,
                        weight = viewModel.weights[prefecture] ?: WeightManager.DEFAULT_WEIGHT,
                        probability = viewModel.probabilityOf(prefecture),
                        onToggle = { viewModel.togglePrefecture(prefecture) },
                        onWeightChange = { viewModel.setWeight(prefecture, it) },
                    )
                    HorizontalDivider(color = Color(0x11000000))
                }
            }
        }
    }
}

@Composable
private fun PrefectureSettingRow(
    prefecture: Prefecture,
    isEnabled: Boolean,
    weight: Double,
    probability: Double,
    onToggle: () -> Unit,
    onWeightChange: (Double) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isEnabled) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = if (isEnabled) "対象" else "対象外",
                tint = if (isEnabled) AccentGreen else Color.Gray,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onToggle() },
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = prefecture.displayName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isEnabled) Color.Unspecified else Color.Gray,
            )
            Spacer(modifier = Modifier.weight(1f))
            // 確率 or 無効バッジ。
            if (isEnabled) {
                Badge(text = "%.1f%%".format(probability), color = AccentBlue)
            } else {
                Badge(text = "対象外", color = Color.Gray)
            }
        }

        // 重みスライダー（有効時のみ操作可）。
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(
                text = "重み ×%.1f".format(weight),
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.width(72.dp),
            )
            Slider(
                value = weight.toFloat(),
                onValueChange = { onWeightChange(it.toDouble()) },
                valueRange = WeightManager.MIN_WEIGHT.toFloat()..WeightManager.MAX_WEIGHT.toFloat(),
                steps = 98, // 0.1 刻み（0.1〜10.0）
                enabled = isEnabled,
                colors = SliderDefaults.colors(
                    thumbColor = AccentBlue,
                    activeTrackColor = AccentBlue,
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = text, fontSize = 11.sp, color = color)
    }
}
