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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.RemoveDone
import androidx.compose.material.icons.outlined.Circle
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AccentBlue = Color(0xFF4285F4)
private val AccentGreen = Color(0xFF33A06B)
private val AccentRed = Color(0xFFE53935)

/**
 * ルーレット設定画面。iOS 版 SettingsView + WeightSettingsView を統合し、
 * カードベースのリッチな見た目に再構成。
 * - サマリーカード（対象県数・重み付け状態）
 * - アクション（全選択 / 全解除 / 重みリセット / 完了）
 * - タイプフィルタ（温泉・自然モードのみ）
 * - 地方ごとの都道府県カード（ON/OFF・重みスライダー・確率表示）
 */
@Composable
fun SettingsScreen(
    viewModel: RouletteViewModel,
    onDone: () -> Unit,
) {
    val enabled = viewModel.enabledPrefectures
    // このモードで選べる県だけを地方ごとにグルーピング（温泉モードなら温泉県のみ）。
    val grouped = remember(viewModel.mode) { Prefecture.groupedByRegion(viewModel.availablePrefectures) }
    val accent = if (viewModel.mode == RouletteMode.TOURISM) AccentGreen else PlanTheme.Primary

    Column(modifier = Modifier.fillMaxWidth()) {
        // 上部固定のヘッダー（タイトル＋完了）。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ルーレット設定", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onDone) {
                Text("完了", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = accent)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // サマリーカード。
            item {
                SummaryCard(
                    targetCount = viewModel.effectiveEnabled.size,
                    totalCount = viewModel.availablePrefectures.size,
                    hasCustomWeights = viewModel.hasCustomWeights,
                    accent = accent,
                )
            }

            // アクションチップ（全選択 / 全解除 / 重みリセット）。
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionChip("全選択", Icons.Filled.DoneAll, AccentBlue, Modifier.weight(1f)) {
                        viewModel.selectAllPrefectures()
                    }
                    ActionChip("全解除", Icons.Filled.RemoveDone, AccentRed, Modifier.weight(1f)) {
                        viewModel.deselectAllPrefectures()
                    }
                    ActionChip("重みリセット", Icons.Filled.RestartAlt, Color(0xFF8A8A8E), Modifier.weight(1f)) {
                        viewModel.resetWeights()
                    }
                }
            }

            // タイプフィルタ（温泉/自然モードのみ）。
            if (viewModel.availableTypes.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appCard(corner = 16.dp)
                            .padding(14.dp),
                    ) {
                        Text("タイプで絞り込み", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 10.dp),
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

            // 地方ごとのセクション（ヘッダー＋県カード群）。
            grouped.forEach { (regionName, prefectures) ->
                val enabledInRegion = prefectures.count { it in enabled }
                item(key = "region_$regionName") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 8.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(regionName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "$enabledInRegion / ${prefectures.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accent,
                        )
                    }
                }
                items(prefectures, key = { it.name }) { prefecture ->
                    PrefectureSettingCard(
                        prefecture = prefecture,
                        isEnabled = prefecture in enabled,
                        weight = viewModel.weights[prefecture] ?: WeightManager.DEFAULT_WEIGHT,
                        probability = viewModel.probabilityOf(prefecture),
                        accent = accent,
                        onToggle = { viewModel.togglePrefecture(prefecture) },
                        onWeightChange = { viewModel.setWeight(prefecture, it) },
                    )
                }
            }
        }
    }
}

/** 対象県数・重み付け状態を示すサマリーカード。 */
@Composable
private fun SummaryCard(
    targetCount: Int,
    totalCount: Int,
    hasCustomWeights: Boolean,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .appCard(corner = 16.dp)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$targetCount", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = accent)
                Text(" / $totalCount 都道府県", fontSize = 14.sp, color = AppTheme.TextSecondary, modifier = Modifier.padding(bottom = 3.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(
                    Icons.Filled.Scale,
                    contentDescription = null,
                    tint = if (hasCustomWeights) AccentBlue else AppTheme.TextSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (hasCustomWeights) "重み付け: カスタム" else "重み付け: 均等",
                    fontSize = 12.sp,
                    color = if (hasCustomWeights) AccentBlue else AppTheme.TextSecondary,
                )
            }
        }
    }
}

/** アイコン付きのアクションチップ（全選択など）。 */
@Composable
private fun ActionChip(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun PrefectureSettingCard(
    prefecture: Prefecture,
    isEnabled: Boolean,
    weight: Double,
    probability: Double,
    accent: Color,
    onToggle: () -> Unit,
    onWeightChange: (Double) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appCard(corner = 14.dp)
            .clickable { onToggle() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isEnabled) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = if (isEnabled) "対象" else "対象外",
                tint = if (isEnabled) AccentGreen else Color(0xFFC7C7CC),
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = prefecture.displayName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isEnabled) AppTheme.TextPrimary else AppTheme.TextSecondary,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isEnabled) {
                Badge(text = "%.1f%%".format(probability), color = AccentBlue)
            } else {
                Badge(text = "対象外", color = Color(0xFFC7C7CC))
            }
        }

        // 重みスライダー（有効時のみ操作可）。
        if (isEnabled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Text(
                    text = "重み ×%.1f".format(weight),
                    fontSize = 12.sp,
                    color = AppTheme.TextSecondary,
                    modifier = Modifier.width(72.dp),
                )
                Slider(
                    value = weight.toFloat(),
                    onValueChange = { onWeightChange(it.toDouble()) },
                    valueRange = WeightManager.MIN_WEIGHT.toFloat()..WeightManager.MAX_WEIGHT.toFloat(),
                    steps = 98, // 0.1 刻み（0.1〜10.0）
                    colors = SliderDefaults.colors(
                        thumbColor = AccentBlue,
                        activeTrackColor = AccentBlue,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
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
