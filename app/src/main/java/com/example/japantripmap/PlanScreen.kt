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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** iOS 互換のブランドアクセント（旧 PlanAccent。詳細画面等で流用）。テーマ本体は PlanTheme.kt。 */
private val PlanAccent = PlanTheme.Primary

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
    // 並び替えモード（2 件以上のとき「編集」で切り替え）。
    var editMode by remember { mutableStateOf(false) }

    // グラデーション背景を画面全体（透明トップバーの裏）まで敷き、段差（境目）を無くす。
    Box(modifier = Modifier
        .fillMaxSize()
        .background(PlanTheme.backgroundGradient)) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("マイプラン", fontWeight = FontWeight.Bold) },
                actions = {
                    // 並び替え・削除の「編集」トグル（プランがあるとき右上に表示）。
                    if (store.plans.isNotEmpty()) {
                        TextButton(onClick = { editMode = !editMode }) {
                            Text(
                                if (editMode) "完了" else "編集",
                                color = PlanTheme.Primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
                // 透明にして背景グラデーションを裏まで繋げる。
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreate = true },
                containerColor = PlanTheme.Primary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "プラン作成", tint = Color.White)
            }
        },
    ) { padding ->
        if (store.plans.isEmpty()) {
            PlanEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onCreate = { showCreate = true },
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(store.plans, key = { it.id }) { plan ->
                    val index = store.plans.indexOfFirst { it.id == plan.id }
                    PlanCard(
                        plan = plan,
                        editMode = editMode,
                        canMoveUp = index > 0,
                        canMoveDown = index < store.plans.size - 1,
                        onClick = { if (!editMode) onOpenPlan(plan.id) },
                        onDelete = { store.deletePlan(plan.id) },
                        onMoveUp = { store.movePlan(index, index - 1) },
                        onMoveDown = { store.movePlan(index, index + 1) },
                    )
                }
            }
        }
    }
    }

    if (showCreate) {
        CreatePlanSheet(
            onDismiss = { showCreate = false },
            onCreate = { title ->
                val plan = store.createPlan(title)
                showCreate = false
                onOpenPlan(plan.id)
            },
        )
    }
}

/** iOS 版 MyPlansView の空状態を移植（グラデーション円＋案内＋作成ボタン）。 */
@Composable
private fun PlanEmptyState(modifier: Modifier = Modifier, onCreate: () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .shadow(16.dp, CircleShape, spotColor = PlanTheme.Primary.copy(alpha = 0.3f))
                .background(PlanTheme.brandGradient, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Luggage,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("プランがまだありません", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "気になった観光地・グルメ・温泉・お祭りを集めて、自分だけの旅行プランを作りましょう。",
            fontSize = 14.sp,
            color = AppTheme.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        PlanPrimaryButton(text = "プランを作成", icon = Icons.Filled.Add, onClick = onCreate)
    }
}

/**
 * プランカード。iOS 版 PlanCardView を移植。
 * 上部：ブランドグラデーションのヘッダー（タイトル＋件数）。
 * 下部：代表カテゴリのアイコンチップ（最大 4）＋日程日数。
 */
@Composable
private fun PlanCard(
    plan: TravelPlan,
    editMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    // 代表カテゴリ（登場順に最大 4 種）。
    val categoryIcons = remember(plan.items) {
        val seen = LinkedHashSet<PlanItemCategory>()
        plan.items.forEach { seen.add(it.category) }
        seen.take(4)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(PlanTheme.cardCorner), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(PlanTheme.cardCorner))
            .background(AppTheme.CardSurface)
            .clickable(enabled = !editMode, onClick = onClick),
    ) {
        // 上部ヘッダー（グラデーション）。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PlanTheme.brandGradient)
                .padding(18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    plan.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "${plan.items.size}件の項目",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            if (editMode) {
                // 並び替え：上下移動。
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = "上へ",
                        tint = Color.White.copy(alpha = if (canMoveUp) 1f else 0.35f),
                    )
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "下へ",
                        tint = Color.White.copy(alpha = if (canMoveDown) 1f else 0.35f),
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "削除", tint = Color.White)
                }
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                )
            }
        }

        // 下部：カテゴリアイコン or プレースホルダ。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (categoryIcons.isEmpty()) {
                Text("まだ項目がありません", fontSize = 12.sp, color = AppTheme.TextSecondary)
            } else {
                categoryIcons.forEach { category ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(planCategoryColor(category), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            planCategoryIcon(category),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                if (plan.groupingMode == PlanGroupingMode.DAY) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = PlanTheme.Primary,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "${plan.dayCount}日間",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlanTheme.Primary,
                    )
                }
            }
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

    // グラデーション背景を画面全体（透明トップバーの裏）まで敷き、段差（境目）を無くす。
    Box(modifier = Modifier
        .fillMaxSize()
        .background(PlanTheme.backgroundGradient)) {
    Scaffold(
        containerColor = Color.Transparent,
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
                        Icon(Icons.Filled.Share, contentDescription = "共有", tint = PlanTheme.Primary)
                    }
                    // 編集（タイトル・メモ）。
                    IconButton(onClick = { showEdit = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "編集", tint = PlanTheme.Primary)
                    }
                },
                // 透明にして背景グラデーションを裏まで繋げる。
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
        floatingActionButton = {
            // カスタム項目（宿泊/交通/メモ）を手動追加。
            FloatingActionButton(onClick = { showAddCustom = true }, containerColor = PlanTheme.Primary) {
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

            // メモ表示（あれば）。iOS 版 memoCard を移植（見出し付きカード）。
            if (plan.memo.isNotBlank()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appCard(corner = PlanTheme.cardCorner)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.EditNote,
                                contentDescription = null,
                                tint = PlanTheme.Primary,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("メモ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                        }
                        Text(plan.memo, fontSize = 14.sp, color = AppTheme.TextPrimary)
                    }
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
    }

    if (showEdit) {
        EditPlanSheet(
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
            .appCard(corner = 14.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 円形カテゴリアイコン（iOS 版 PlanItemCard の左アバター）。
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                planCategoryIcon(item.category),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
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

/** 時間ブロックの時刻・見出しを編集するシート。 */
@OptIn(ExperimentalMaterial3Api::class)
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

    val sheetState = rememberModalBottomSheetState()

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
            Text("時間ブロック", fontSize = 17.sp, fontWeight = FontWeight.Bold)

            // 時刻（時・分のステッパー）。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PlanTheme.Surface2)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("開始時刻", fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                modifier = Modifier.fillMaxWidth(),
            )
            PlanPrimaryButton(
                text = "保存",
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSave(block.copy(startHour = hour, startMinute = minute, title = title)) },
            )
            TextButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("このブロックを削除", color = Color(0xFFE53935))
            }
        }
    }
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
            .appCard(corner = 12.dp)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = PlanAccent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("日程ごとに分ける", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = dayMode,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PlanTheme.Primary,
                    checkedBorderColor = PlanTheme.Primary,
                ),
            )
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

/** プラン新規作成シート。iOS 版 MyPlansView.newPlanSheet を移植（テーマ付きボトムシート）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePlanSheet(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("プランを作成", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("プラン名", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextSecondary)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("例：夏の北海道旅") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PlanPrimaryButton(text = "作成", onClick = { onCreate(title.ifBlank { "無題のプラン" }) })
        }
    }
}

/** プラン編集シート。iOS 版 PlanDetailView.memoEditor を移植（テーマ付きボトムシート）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPlanSheet(
    initialTitle: String,
    initialMemo: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var memo by remember { mutableStateOf(initialMemo) }
    val sheetState = rememberModalBottomSheetState()

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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("プランを編集", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("プラン名", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("例：夏の北海道旅") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("メモ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                )
            }
            PlanPrimaryButton(text = "保存", onClick = { onSave(title, memo) })
        }
    }
}

/** 宿泊/交通/メモを手動追加するシート。iOS 版 CustomPlanItemEditor を簡略移植（テーマ付きボトムシート）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCustomItemDialog(
    onDismiss: () -> Unit,
    onAdd: (PlanItemCategory, String, String) -> Unit,
) {
    var category by remember { mutableStateOf(PlanItemCategory.HOTEL) }
    var name by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    val customCategories = listOf(PlanItemCategory.HOTEL, PlanItemCategory.TRANSPORT, PlanItemCategory.OTHER)
    val sheetState = rememberModalBottomSheetState()

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
            Text("項目を追加", fontSize = 17.sp, fontWeight = FontWeight.Bold)

            // 種別選択（アイコン付きチップ）。
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                customCategories.forEach { c ->
                    val selected = category == c
                    val color = planCategoryColor(c)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) color else color.copy(alpha = 0.12f))
                            .clickable { category = c }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            planCategoryIcon(c),
                            contentDescription = null,
                            tint = if (selected) Color.White else color,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            c.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) Color.White else color,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("タイトル") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = detail,
                onValueChange = { detail = it },
                label = { Text("メモ（任意）") },
                modifier = Modifier.fillMaxWidth(),
            )
            PlanPrimaryButton(
                text = "追加",
                icon = Icons.Filled.Add,
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = { if (name.isNotBlank()) onAdd(category, name, detail) },
            )
        }
    }
}
