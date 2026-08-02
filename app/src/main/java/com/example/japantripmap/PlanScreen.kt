package com.example.japantripmap

import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PlanAccent = Color(0xFFFF9500)

/** カテゴリ -> 色。 */
private fun planCategoryColor(c: PlanItemCategory): Color = when (c) {
    PlanItemCategory.ATTRACTION -> Color(0xFF9C27B0)
    PlanItemCategory.GOURMET -> Color(0xFFED7321)
    PlanItemCategory.ONSEN -> Color(0xFFED7321)
    PlanItemCategory.FESTIVAL -> Color(0xFF8C61C7)
    PlanItemCategory.NATURE -> Color(0xFF3D9E66)
    PlanItemCategory.SOUVENIR -> Color(0xFFDB5994)
    PlanItemCategory.HOTEL -> Color(0xFF2980D9)
    PlanItemCategory.TRANSPORT -> Color(0xFF737380)
    PlanItemCategory.OTHER -> Color(0xFF888888)
}

/**
 * マイプランタブ。プラン一覧 ⇄ プラン詳細を切り替える。
 * iOS 版 MyPlansView / PlanDetailView を移植（中核）。
 */
@Composable
fun PlanScreen(store: TravelPlanStore) {
    var openPlanId by remember { mutableStateOf<String?>(null) }

    val current = openPlanId?.let { store.plan(it) }
    if (current != null) {
        PlanDetailScreen(
            plan = current,
            store = store,
            onBack = { openPlanId = null },
        )
    } else {
        PlanListScreen(
            store = store,
            onOpenPlan = { openPlanId = it },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanListScreen(
    store: TravelPlanStore,
    onOpenPlan: (String) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("マイプラン", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFF3E9)),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = PlanAccent,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "プラン作成", tint = Color.White)
            }
        },
    ) { padding ->
        if (store.plans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("プランがありません。\n＋ボタンで作成できます。", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(store.plans.size) { i ->
                    val plan = store.plans[i]
                    PlanRow(
                        plan = plan,
                        onClick = { onOpenPlan(plan.id) },
                        onDelete = { store.deletePlan(plan.id) },
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreatePlanDialog(
            onDismiss = { showCreate = false },
            onCreate = { title ->
                val plan = store.createPlan(title)
                showCreate = false
                onOpenPlan(plan.id)
            },
        )
    }
}

@Composable
private fun PlanRow(plan: TravelPlan, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(plan.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("${plan.items.size} 件の項目", fontSize = 13.sp, color = Color.Gray)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "削除", tint = Color(0xFFE53935))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanDetailScreen(
    plan: TravelPlan,
    store: TravelPlanStore,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var showEdit by remember { mutableStateOf(false) }
    var showAddCustom by remember { mutableStateOf(false) }
    // 時刻・見出し編集中の時間ブロック。null なら非表示。
    var editingBlock by remember { mutableStateOf<TimeBlock?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plan.title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    // 共有。
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, PlanShareFormatter.text(plan))
                        }
                        context.startActivity(Intent.createChooser(send, "プランを共有"))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "共有", tint = PlanAccent)
                    }
                    // 編集（タイトル・メモ）。
                    IconButton(onClick = { showEdit = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "編集", tint = PlanAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFF3E9)),
            )
        },
        floatingActionButton = {
            // カスタム項目（宿泊/交通/メモ）を手動追加。
            FloatingActionButton(onClick = { showAddCustom = true }, containerColor = PlanAccent) {
                Icon(Icons.Filled.Add, contentDescription = "項目を追加", tint = Color.White)
            }
        },
    ) { padding ->
        val dayMode = plan.groupingMode == PlanGroupingMode.DAY
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // コントロールカード（日程トグル・日数）。
            item {
                DayControlCard(
                    dayMode = dayMode,
                    dayCount = plan.dayCount,
                    onToggle = {
                        store.setGroupingMode(plan.id, if (it) PlanGroupingMode.DAY else PlanGroupingMode.FLAT)
                    },
                    onDayCountChange = { store.setDayCount(plan.id, it) },
                )
            }

            // メモ表示（あれば）。
            if (plan.memo.isNotBlank()) {
                item {
                    Text(
                        plan.memo,
                        fontSize = 14.sp,
                        color = Color(0xFF555555),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF7EF), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                    )
                }
            }

            if (dayMode) {
                // 日程モード：Day ごとに、実日は時間ブロックで細分化して表示。
                plan.itemsByDay.forEach { (day, dayItems) ->
                    item(key = "dayheader_${day ?: "none"}") {
                        Text(
                            text = if (day != null) "Day $day" else "未割り当て",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlanAccent,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                        )
                    }

                    if (day != null) {
                        // 実日：時間ブロックごと（時刻順）＋ブロック未割当。
                        plan.blockSections(day).forEach { (block, blockItems) ->
                            item(key = "block_${block?.id ?: "noblock"}_$day") {
                                TimeBlockHeader(
                                    block = block,
                                    onEdit = { editingBlock = it },
                                    onDelete = { block?.let { store.removeTimeBlock(plan.id, it.id) } },
                                )
                            }
                            items(blockItems, key = { it.id }) { item ->
                                PlanItemRow(
                                    item = item,
                                    onRemove = { store.removeItem(plan.id, item.id) },
                                    dayMode = true,
                                    dayCount = plan.dayCount,
                                    currentDay = item.dayNumber,
                                    onAssignDay = { store.assignItemToDay(plan.id, item.id, it) },
                                    timeBlocks = plan.timeBlocks.filter { it.dayNumber == day },
                                    currentBlockId = item.timeBlockId,
                                    onAssignBlock = { store.assignItemToBlock(plan.id, item.id, it) },
                                )
                            }
                        }
                        // この日に時間ブロックを追加。
                        item(key = "addblock_$day") {
                            AddTimeBlockButton(onClick = {
                                store.addTimeBlock(plan.id, day, startHour = 9, startMinute = 0)
                            })
                        }
                    } else {
                        // 未割り当て日：時間ブロックなしでフラット表示。
                        if (dayItems.isEmpty()) {
                            item(key = "dayempty_none") {
                                Text(
                                    "（項目なし）",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                                )
                            }
                        } else {
                            items(dayItems, key = { it.id }) { item ->
                                PlanItemRow(
                                    item = item,
                                    onRemove = { store.removeItem(plan.id, item.id) },
                                    dayMode = true,
                                    dayCount = plan.dayCount,
                                    currentDay = item.dayNumber,
                                    onAssignDay = { store.assignItemToDay(plan.id, item.id, it) },
                                )
                            }
                        }
                    }
                }
            } else if (plan.items.isEmpty()) {
                item {
                    Text(
                        "まだ項目がありません。\n観光・温泉・自然の詳細画面から「プランに追加」、\nまたは右下の＋で宿泊・交通・メモを追加できます。",
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 40.dp),
                    )
                }
            } else {
                items(plan.items.size) { i ->
                    val it = plan.items[i]
                    PlanItemRow(it, onRemove = { store.removeItem(plan.id, it.id) })
                }
            }
        }
    }

    if (showEdit) {
        EditPlanDialog(
            initialTitle = plan.title,
            initialMemo = plan.memo,
            onDismiss = { showEdit = false },
            onSave = { title, memo ->
                store.updatePlan(plan.id, title, memo)
                showEdit = false
            },
        )
    }

    if (showAddCustom) {
        AddCustomItemDialog(
            onDismiss = { showAddCustom = false },
            onAdd = { category, name, detail ->
                store.addItem(
                    plan.id,
                    PlanItem(category = category, prefectureName = "", name = name, detail = detail),
                )
                showAddCustom = false
            },
        )
    }

    editingBlock?.let { block ->
        TimeBlockEditDialog(
            block = block,
            onDismiss = { editingBlock = null },
            onSave = { updated ->
                store.updateTimeBlock(plan.id, updated)
                editingBlock = null
            },
            onDelete = {
                store.removeTimeBlock(plan.id, block.id)
                editingBlock = null
            },
        )
    }
}

@Composable
private fun PlanItemRow(
    item: PlanItem,
    onRemove: () -> Unit,
    dayMode: Boolean = false,
    dayCount: Int = 1,
    currentDay: Int? = null,
    onAssignDay: (Int?) -> Unit = {},
    timeBlocks: List<TimeBlock> = emptyList(),
    currentBlockId: String? = null,
    onAssignBlock: (String?) -> Unit = {},
) {
    val accent = planCategoryColor(item.category)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(accent, RoundedCornerShape(5.dp)),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(accent.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(item.category.label, fontSize = 11.sp, color = accent)
                }
            }
            if (item.prefectureName.isNotBlank()) {
                Text(item.prefectureName, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
            }
            if (item.detail.isNotBlank()) {
                Text(item.detail, fontSize = 12.sp, color = Color(0xFF666666), modifier = Modifier.padding(top = 2.dp))
            }
            // 実日で時間ブロックがあるとき、所属ブロックを選べる。
            if (dayMode && currentDay != null && timeBlocks.isNotEmpty()) {
                BlockAssignButton(
                    blocks = timeBlocks,
                    currentBlockId = currentBlockId,
                    onAssignBlock = onAssignBlock,
                )
            }
        }
        // 日程モードでは「日を選ぶ」ドロップダウン。
        if (dayMode) {
            DayAssignButton(dayCount = dayCount, currentDay = currentDay, onAssignDay = onAssignDay)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "削除", tint = Color(0xFFBBBBBB))
        }
    }
}

/** 時間ブロックの見出し行（時刻＋タイトル）。タップで時刻編集、ゴミ箱で削除。 */
@Composable
private fun TimeBlockHeader(block: TimeBlock?, onEdit: (TimeBlock) -> Unit, onDelete: () -> Unit) {
    if (block == null) {
        // ブロック未割当セクションの見出し。
        Text(
            text = "時間未設定",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onEdit(block) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PlanAccent.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = block.timeLabel ?: "時刻未設定",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PlanAccent,
            )
        }
        if (block.title.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(block.title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Filled.Edit, contentDescription = "時刻を編集", tint = Color(0xFFBBBBBB), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "ブロックを削除", tint = Color(0xFFDDDDDD), modifier = Modifier.size(18.dp))
        }
    }
}

/** 「この日に時間ブロックを追加」ボタン。 */
@Composable
private fun AddTimeBlockButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = PlanAccent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("時間ブロックを追加", fontSize = 13.sp, color = PlanAccent, fontWeight = FontWeight.Medium)
    }
}

/** 項目を時間ブロックに割り当てるボタン＋ドロップダウン。 */
@Composable
private fun BlockAssignButton(blocks: List<TimeBlock>, currentBlockId: String?, onAssignBlock: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val sorted = blocks.sortedBy { it.sortKey }
    val current = sorted.firstOrNull { it.id == currentBlockId }
    Box(modifier = Modifier.padding(top = 4.dp)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(PlanAccent.copy(alpha = 0.10f))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = current?.let { it.timeLabel ?: it.title.ifBlank { "ブロック" } } ?: "時間帯を選択",
                fontSize = 11.sp,
                color = PlanAccent,
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = PlanAccent, modifier = Modifier.size(14.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sorted.forEach { b ->
                val label = (b.timeLabel ?: "時刻未設定") + (if (b.title.isNotBlank()) " ${b.title}" else "")
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onAssignBlock(b.id); expanded = false },
                )
            }
            DropdownMenuItem(
                text = { Text("時間帯なし") },
                onClick = { onAssignBlock(null); expanded = false },
            )
        }
    }
}

/** 時間ブロックの時刻・見出しを編集するダイアログ。 */
@Composable
private fun TimeBlockEditDialog(
    block: TimeBlock,
    onDismiss: () -> Unit,
    onSave: (TimeBlock) -> Unit,
    onDelete: () -> Unit,
) {
    var hour by remember { mutableStateOf(block.startHour ?: 9) }
    var minute by remember { mutableStateOf(block.startMinute ?: 0) }
    var title by remember { mutableStateOf(block.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("時間ブロック") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 時刻（時・分のステッパー）。
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("開始時刻", fontSize = 14.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    StepperField(value = hour, range = 0..23, onChange = { hour = it }, suffix = "時")
                    Spacer(modifier = Modifier.width(8.dp))
                    StepperField(value = minute, range = 0..59, step = 5, onChange = { minute = it }, suffix = "分")
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("見出し（任意・例: 午前）") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(block.copy(startHour = hour, startMinute = minute, title = title))
            }) { Text("保存") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("削除", color = Color(0xFFE53935)) }
                TextButton(onClick = onDismiss) { Text("キャンセル") }
            }
        },
    )
}

/** 数値の＋/−ステッパー。 */
@Composable
private fun StepperField(value: Int, range: IntRange, step: Int = 1, onChange: (Int) -> Unit, suffix: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange((value - step).coerceIn(range)) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "減らす", tint = PlanAccent, modifier = Modifier.size(20.dp))
        }
        Text("%02d%s".format(value, suffix), fontSize = 15.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onChange((value + step).coerceIn(range)) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.AddCircleOutline, contentDescription = "増やす", tint = PlanAccent, modifier = Modifier.size(20.dp))
        }
    }
}

/** 項目をどの日に割り当てるか選ぶボタン＋ドロップダウン。 */
@Composable
private fun DayAssignButton(dayCount: Int, currentDay: Int?, onAssignDay: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PlanAccent.copy(alpha = 0.12f))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = currentDay?.let { "Day $it" } ?: "日を選択",
                fontSize = 12.sp,
                color = PlanAccent,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = PlanAccent,
                modifier = Modifier.size(16.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (1..dayCount).forEach { d ->
                DropdownMenuItem(
                    text = { Text("Day $d") },
                    onClick = { onAssignDay(d); expanded = false },
                )
            }
            DropdownMenuItem(
                text = { Text("未割り当て") },
                onClick = { onAssignDay(null); expanded = false },
            )
        }
    }
}

/** 日程トグル・日数ステッパーのコントロールカード。 */
@Composable
private fun DayControlCard(
    dayMode: Boolean,
    dayCount: Int,
    onToggle: (Boolean) -> Unit,
    onDayCountChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = PlanAccent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("日程ごとに分ける", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = dayMode, onCheckedChange = onToggle)
        }
        if (dayMode) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0x11000000))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("日数: $dayCount 日", fontSize = 14.sp)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onDayCountChange(dayCount - 1) }, enabled = dayCount > 1) {
                    Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "減らす", tint = if (dayCount > 1) PlanAccent else Color.Gray)
                }
                IconButton(onClick = { onDayCountChange(dayCount + 1) }, enabled = dayCount < 30) {
                    Icon(Icons.Filled.AddCircleOutline, contentDescription = "増やす", tint = if (dayCount < 30) PlanAccent else Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun CreatePlanDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新しいプラン") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("プラン名") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(title) }) { Text("作成") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

@Composable
private fun EditPlanDialog(
    initialTitle: String,
    initialMemo: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var memo by remember { mutableStateOf(initialMemo) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("プランを編集") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("プラン名") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("メモ") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, memo) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}

/** 宿泊/交通/メモを手動追加するダイアログ。iOS 版 CustomPlanItemEditor を簡略移植。 */
@Composable
private fun AddCustomItemDialog(
    onDismiss: () -> Unit,
    onAdd: (PlanItemCategory, String, String) -> Unit,
) {
    var category by remember { mutableStateOf(PlanItemCategory.HOTEL) }
    var name by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    val customCategories = listOf(PlanItemCategory.HOTEL, PlanItemCategory.TRANSPORT, PlanItemCategory.OTHER)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("項目を追加") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 種別選択（チップ）。
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    customCategories.forEach { c ->
                        val selected = category == c
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selected) planCategoryColor(c) else planCategoryColor(c).copy(alpha = 0.12f),
                                )
                                .clickable { category = c }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(
                                c.label,
                                fontSize = 13.sp,
                                color = if (selected) Color.White else planCategoryColor(c),
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("タイトル") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("メモ（任意）") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onAdd(category, name, detail) },
            ) { Text("追加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("キャンセル") }
        },
    )
}
