package com.example.japantripmap

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val OnsenMarkColor = Color(0xFFFF9500)
private val CheckGreen = Color(0xFF34C759)

/**
 * シート相当の全画面のヘッダー（左=キャンセル/閉じる、中央=タイトル、右=確定）。
 * iOS の NavigationStack + toolbar のシートに対応する。
 */
@Composable
fun SheetTopBar(
    title: String,
    leading: String?,
    onLeading: () -> Unit,
    trailing: String? = null,
    trailingEnabled: Boolean = true,
    onTrailing: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(52.dp)
            .padding(horizontal = 8.dp),
    ) {
        if (leading != null) {
            TextButton(onClick = onLeading, modifier = Modifier.align(Alignment.CenterStart)) {
                Text(leading, color = PlanTheme.Primary, fontSize = 16.sp)
            }
        }
        Text(
            title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 96.dp),
        )
        if (trailing != null) {
            TextButton(onClick = onTrailing, enabled = trailingEnabled, modifier = Modifier.align(Alignment.CenterEnd)) {
                Text(
                    trailing,
                    color = if (trailingEnabled) PlanTheme.Primary else AppTheme.TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// MARK: - 県をまとめて追加

/**
 * 県を複数選択してプランへ一括追加する画面。iOS 版 PrefecturePickerSheet。
 * 既にプランにある県は除外し、検索（漢字/かな/ローマ字）で絞り込み・チェックで複数選択する。
 */
@Composable
fun PrefecturePickerScreen(excluded: Set<Prefecture>, onAdd: (List<Prefecture>) -> Unit, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    var query by remember { mutableStateOf("") }
    var selection by remember { mutableStateOf(listOf<Prefecture>()) }
    val regions = prefecturesByRegion(query, excluded)

    Column(modifier = Modifier.fillMaxSize().background(PlanTheme.pageBackground)) {
        SheetTopBar(
            title = stringResource(R.string.plan_prefectures_add),
            leading = stringResource(R.string.common_cancel),
            onLeading = onDismiss,
            trailing = if (selection.isEmpty()) stringResource(R.string.common_add)
            else stringResource(R.string.plan_prefectures_add_count, selection.size),
            trailingEnabled = selection.isNotEmpty(),
            onTrailing = { onAdd(selection) },
        )
        SearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.prefecture_search_placeholder),
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        )
        if (regions.isEmpty()) {
            SearchEmptyState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(regions, key = { it.first.name }) { (region, prefs) ->
                    RegionBlock(region.localizedName) {
                        prefs.forEach { pref ->
                            val selected = pref in selection
                            PrefectureRow(
                                prefecture = pref,
                                leadingIcon = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                leadingTint = if (selected) PlanTheme.Primary else Color(0xFFC7C7CC),
                                highlighted = selected,
                                showChevron = false,
                            ) {
                                selection = if (selected) selection - pref else selection + pref
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionBlock(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextSecondary, modifier = Modifier.padding(horizontal = 4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun PrefectureRow(
    prefecture: Prefecture,
    leadingIcon: ImageVector?,
    leadingTint: Color,
    highlighted: Boolean,
    showChevron: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PlanTheme.groupedSurface)
            .border(1.5.dp, if (highlighted) PlanTheme.Primary else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = leadingTint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(prefecture.localizedName(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextPrimary, modifier = Modifier.weight(1f))
        if (prefecture.hasOnsen) {
            Icon(Icons.Filled.Thermostat, contentDescription = null, tint = OnsenMarkColor, modifier = Modifier.size(14.dp))
        }
        if (showChevron) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFFC7C7CC), modifier = Modifier.size(18.dp))
        }
    }
}

// MARK: - スポットのクイック追加

/** クイック追加で選べるカテゴリ（観光・グルメ・温泉・お土産）。iOS 版 QuickAddCategory。 */
private enum class QuickAddCategory(val planCategory: PlanItemCategory) {
    ATTRACTION(PlanItemCategory.ATTRACTION),
    GOURMET(PlanItemCategory.GOURMET),
    ONSEN(PlanItemCategory.ONSEN),
    SOUVENIR(PlanItemCategory.SOUVENIR);

    /** 指定県のこのカテゴリのスポット（名前, 説明）。 */
    fun spots(prefecture: Prefecture): List<Pair<String, String>> = when (this) {
        ATTRACTION -> prefecture.tourismInfo?.attractions?.map { it.name to it.description } ?: emptyList()
        GOURMET -> prefecture.gourmets.map { it.name to it.description }
        ONSEN -> prefecture.onsens.map { it.name to it.description }
        SOUVENIR -> prefecture.souvenirs.map { it.name to it.description }
    }
}

/**
 * 県を選ぶ → その県のスポットを一覧で見て、詳細を開かずに「＋」で直接プランへ追加する画面。
 * iOS 版 SpotQuickAddSheet。プランに登録済みの県を「候補」として先頭に出す。
 * Day 起点で開いたとき（initialDay）は追加した項目をその日に割り当てる。
 */
@Composable
fun SpotQuickAddScreen(
    store: TravelPlanStore,
    planId: String,
    suggestedPrefectures: List<Prefecture>,
    initialDay: Int?,
    onDismiss: () -> Unit,
) {
    var prefecture by remember { mutableStateOf<Prefecture?>(null) }
    var category by remember { mutableStateOf(QuickAddCategory.ATTRACTION) }
    var query by remember { mutableStateOf("") }
    BackHandler { if (prefecture != null) prefecture = null else onDismiss() }

    Column(modifier = Modifier.fillMaxSize().background(PlanTheme.pageBackground)) {
        Box {
            SheetTopBar(
                title = prefecture?.localizedName() ?: stringResource(R.string.plan_spot_add),
                leading = null,
                onLeading = {},
                trailing = stringResource(R.string.common_close),
                onTrailing = onDismiss,
            )
            if (prefecture != null) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 8.dp, top = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { prefecture = null }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.plan_spot_change_prefecture), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                }
            }
        }

        val pref = prefecture
        if (pref == null) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.prefecture_search_placeholder),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            )
            val regions = prefecturesByRegion(query)
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (query.isEmpty() && suggestedPrefectures.isNotEmpty()) {
                    item(key = "suggested") {
                        RegionBlock(stringResource(R.string.plan_spot_suggested)) {
                            suggestedPrefectures.forEach { p ->
                                PrefectureRow(p, null, Color.Transparent, highlighted = false, showChevron = true) { prefecture = p }
                            }
                        }
                    }
                }
                items(regions, key = { it.first.name }) { (region, prefs) ->
                    RegionBlock(region.localizedName) {
                        prefs.forEach { p ->
                            PrefectureRow(p, null, Color.Transparent, highlighted = false, showChevron = true) { prefecture = p }
                        }
                    }
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                items(QuickAddCategory.entries) { c ->
                    val selected = c == category
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .then(if (selected) Modifier.background(PlanTheme.brandGradient) else Modifier.background(PlanTheme.Primary.copy(alpha = 0.12f)))
                            .clickable { category = c }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(planCategoryIcon(c.planCategory), contentDescription = null, tint = if (selected) Color.White else PlanTheme.Primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(c.planCategory.labelRes), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else PlanTheme.Primary)
                    }
                }
            }
            val spots = category.spots(pref)
            if (spots.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(planCategoryIcon(category.planCategory), contentDescription = null, tint = AppTheme.TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(stringResource(R.string.plan_spot_empty_category), fontSize = 14.sp, color = AppTheme.TextSecondary)
                }
            } else {
                val plan = store.plan(planId)
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(spots, key = { i, s -> "$i|${s.first}" }) { _, (name, subtitle) ->
                        val added = plan?.items?.any { it.name == name && it.category == category.planCategory } == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (added) 0.7f else 1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PlanTheme.groupedSurface)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CategoryAvatar(category.planCategory, size = 40, iconSize = 18)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(localizeData(name), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (subtitle.isNotBlank()) {
                                    Text(localizeData(subtitle), fontSize = 11.sp, color = AppTheme.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                if (added) Icons.Filled.CheckCircle else Icons.Filled.AddCircle,
                                contentDescription = stringResource(R.string.common_add_to_plan),
                                tint = if (added) CheckGreen else PlanTheme.Primary,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = !added) {
                                        store.addItem(
                                            planId,
                                            PlanItem(
                                                category = category.planCategory,
                                                prefectureName = pref.displayName,
                                                name = name,
                                                detail = subtitle,
                                                dayNumber = initialDay,
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

// MARK: - タイトル・メモ編集

/** タイトル編集（1 行なのでダイアログで完結）。空タイトルは保存しない。iOS の alert 入力に対応。 */
@Composable
fun PlanTitleEditDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var title by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.plan_title_label)) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(stringResource(R.string.plan_title_placeholder)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PlanTheme.Primary, cursorColor = PlanTheme.Primary),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(title) }) { Text(stringResource(R.string.common_save), color = PlanTheme.Primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = PlanTheme.Primary) }
        },
        containerColor = Color.White,
    )
}

/** メモ編集画面（iOS memoEditor シート）。 */
@Composable
fun PlanMemoEditorScreen(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    title: String = stringResource(R.string.plan_item_editmemo),
) {
    BackHandler(onBack = onDismiss)
    var memo by remember { mutableStateOf(initial) }
    Column(modifier = Modifier.fillMaxSize().background(PlanTheme.pageBackground)) {
        SheetTopBar(
            title = title,
            leading = stringResource(R.string.common_cancel),
            onLeading = onDismiss,
            trailing = stringResource(R.string.common_save),
            onTrailing = { onSave(memo) },
        )
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth().planCard()) {
            Text(stringResource(R.string.plan_memo_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = PlanTheme.Primary,
                ),
            )
        }
    }
}
