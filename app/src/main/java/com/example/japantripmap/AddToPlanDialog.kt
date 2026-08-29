package com.example.japantripmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 「プランに追加」シート。既存プランを選ぶか、新規作成して追加する。
 * iOS 版 AddToPlanSheet を移植（ブランド統一のカード UI）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlanDialog(
    store: TravelPlanStore,
    item: PlanItem,
    onDismiss: () -> Unit,
    onAdded: (planTitle: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showNewPlanField by remember { mutableStateOf(store.plans.isEmpty()) }
    var newPlanTitle by remember { mutableStateOf("") }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.plan_add_title), fontSize = 17.sp, fontWeight = FontWeight.Bold)

            // 追加対象のプレビュー。
            ItemPreviewCard(item)

            // 新規プラン作成。
            NewPlanCard(
                showField = showNewPlanField,
                title = newPlanTitle,
                onTitleChange = { newPlanTitle = it },
                onExpand = { showNewPlanField = true },
                onCreate = {
                    val fallback = if (item.prefectureName.isNotBlank())
                        context.getString(R.string.plan_default_title, item.prefectureName)
                    else context.getString(R.string.plan_untitled)
                    val name = newPlanTitle.trim().ifBlank { fallback }
                    val plan = store.createPlan(name)
                    store.addItem(plan.id, item)
                    onAdded(plan.title)
                },
            )

            // 既存プラン一覧。
            if (store.plans.isNotEmpty()) {
                ExistingPlansCard(
                    store = store,
                    item = item,
                    onSelect = { plan ->
                        store.addItem(plan.id, item)
                        onAdded(plan.title)
                    },
                )
            }
        }
    }
}

/** 追加しようとしている項目のプレビュー（円形アイコン＋名前＋カテゴリ・県）。 */
@Composable
private fun ItemPreviewCard(item: PlanItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PlanTheme.Surface2)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(item.category, size = 44, iconSize = 22)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(localizeData(item.name), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            val categoryLabel = stringResource(item.category.labelRes)
            val sub = buildString {
                append(categoryLabel)
                if (item.prefectureName.isNotBlank()) append(" · ${localizeData(item.prefectureName)}")
            }
            Text(sub, fontSize = 12.sp, color = Color(0xFF8A8A8E), modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** 新規プランを作って追加するカード（初期はボタン、押すと入力欄が展開）。 */
@Composable
private fun NewPlanCard(
    showField: Boolean,
    title: String,
    onTitleChange: (String) -> Unit,
    onExpand: () -> Unit,
    onCreate: () -> Unit,
) {
    if (showField) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = { Text(stringResource(R.string.plan_title_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            PlanPrimaryButton(
                text = stringResource(R.string.plan_add_new),
                icon = Icons.Filled.AddCircleOutline,
                modifier = Modifier.fillMaxWidth(),
                onClick = onCreate,
            )
        }
    } else {
        PlanPrimaryButton(
            text = stringResource(R.string.plan_add_new),
            icon = Icons.Filled.AddCircleOutline,
            modifier = Modifier.fillMaxWidth(),
            onClick = onExpand,
        )
    }
}

/** 既存プランの一覧。追加済みのものはチェック表示で無効化。 */
@Composable
private fun ExistingPlansCard(
    store: TravelPlanStore,
    item: PlanItem,
    onSelect: (TravelPlan) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.plan_add_existing), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
        store.plans.forEach { plan ->
            val contained = plan.items.any { it.name == item.name && it.category == item.category }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PlanTheme.Surface2)
                    .then(if (contained) Modifier.alpha(0.6f) else Modifier)
                    .clickable(enabled = !contained) { onSelect(plan) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(PlanTheme.brandGradient, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Luggage, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(plan.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(stringResource(R.string.plan_item_count, plan.items.size), fontSize = 12.sp, color = Color(0xFF8A8A8E), modifier = Modifier.padding(top = 1.dp))
                }
                Icon(
                    if (contained) Icons.Filled.CheckCircle else Icons.Filled.AddCircleOutline,
                    contentDescription = stringResource(if (contained) R.string.plan_already_added else R.string.plan_add),
                    tint = if (contained) Color(0xFF34C759) else PlanTheme.Primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
