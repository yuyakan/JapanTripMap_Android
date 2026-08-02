package com.example.japantripmap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 「プランに追加」ダイアログ。既存プランを選ぶか、新規作成して追加する。
 * iOS 版 AddToPlanSheet を移植（中核）。
 */
@Composable
fun AddToPlanDialog(
    store: TravelPlanStore,
    item: PlanItem,
    onDismiss: () -> Unit,
    onAdded: (planTitle: String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("プランに追加") },
        text = {
            Column {
                Text(
                    text = "「${item.name}」を追加するプランを選択",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (store.plans.isEmpty()) {
                    Text("プランがありません。下の「新規プランに追加」を押してください。", fontSize = 13.sp)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(store.plans.size) { i ->
                            val plan = store.plans[i]
                            Text(
                                text = plan.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        store.addItem(plan.id, item)
                                        onAdded(plan.title)
                                    }
                                    .padding(vertical = 12.dp),
                            )
                            HorizontalDivider(color = Color(0x11000000))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val plan = store.createPlan("${item.prefectureName}の旅")
                store.addItem(plan.id, item)
                onAdded(plan.title)
            }) {
                Text("新規プランに追加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}
