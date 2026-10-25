package com.example.japantripmap

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Thermostat
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 温泉チップの色（iOS .orange）。 */
private val OnsenChipColor = Color(0xFFFF9500)

/**
 * 都道府県の一覧・検索画面。iOS 版 PrefectureSearchView。
 * 都道府県マップ右上の🔍から全画面で開き、県を選ぶと県詳細を開く（地図をタップしなくても辿り着ける導線）。
 * 漢字・ひらがな・カタカナ・ローマ字のいずれでも絞り込める。
 */
@Composable
fun PrefectureSearchScreen(onSelect: (Prefecture) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val regions = prefecturesByRegion(query)

    Column(modifier = Modifier.fillMaxSize().background(PlanTheme.backgroundGradient)) {
        // ブランドグラデーションの帯（検索窓＋閉じる）。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PlanTheme.brandGradient)
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.prefecture_search_placeholder),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        if (regions.isEmpty()) {
            SearchEmptyState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                items(regions, key = { it.first.name }) { (region, prefs) ->
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(PlanTheme.brandGradient),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(region.localizedName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextPrimary)
                        }
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            prefs.forEach { pref ->
                                PrefectureSearchRow(pref) {
                                    focusManager.clearFocus()
                                    onSelect(pref)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 角丸カード風の検索窓（OS 既定のバーは使わない）。県まとめ追加シート等でも共用する。 */
@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(14.dp), spotColor = PlanTheme.cardShadow)
            .clip(RoundedCornerShape(14.dp))
            .background(PlanTheme.groupedSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = AppTheme.TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, fontSize = 16.sp, color = AppTheme.TextSecondary)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = AppTheme.TextPrimary),
                cursorBrush = SolidColor(PlanTheme.Primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Filled.Cancel,
                contentDescription = stringResource(R.string.common_clear),
                tint = AppTheme.TextSecondary,
                modifier = Modifier.size(18.dp).clickable { onValueChange("") },
            )
        }
    }
}

@Composable
fun SearchEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = AppTheme.TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(44.dp))
        Spacer(modifier = Modifier.height(14.dp))
        Text(stringResource(R.string.prefecture_search_empty), fontSize = 15.sp, color = AppTheme.TextSecondary)
    }
}

/** 一覧の 1 行（県名＋代表スポット 2 件＋温泉チップ）。 */
@Composable
private fun PrefectureSearchRow(prefecture: Prefecture, onClick: () -> Unit) {
    val subtitle = prefecture.tourismInfo?.attractions?.take(2)?.joinToString("・") { localizeData(it.name) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = PlanTheme.cardShadow)
            .clip(RoundedCornerShape(16.dp))
            .background(PlanTheme.groupedSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(prefecture.localizedName(), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AppTheme.TextPrimary)
            if (!subtitle.isNullOrEmpty()) {
                Text(subtitle, fontSize = 12.sp, color = AppTheme.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (prefecture.hasOnsen) {
            Spacer(modifier = Modifier.width(8.dp))
            OnsenChip()
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(18.dp))
    }
}

@Composable
fun OnsenChip() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(OnsenChipColor.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Thermostat, contentDescription = null, tint = OnsenChipColor, modifier = Modifier.size(11.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(stringResource(R.string.prefecture_search_onsen_chip), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = OnsenChipColor)
    }
}
