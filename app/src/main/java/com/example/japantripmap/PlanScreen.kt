package com.example.japantripmap

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

/** ドラッグ&ドロップで項目 id を運ぶための MIME タイプ。 */
private const val PLAN_ITEM_MIME = "text/plain"

/**
 * 日程モードのドロップ先。ドロップされた項目を移動先の day / block に割り当て、
 * targetItemId が指定されていればその直前へ並べ替える（null なら末尾）。
 */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.planDropTarget(
    store: TravelPlanStore,
    planId: String,
    day: Int?,
    blockId: String?,
    targetItemId: String?,
    onHover: (Boolean) -> Unit,
): Modifier = composed {
    val callback = remember(planId, day, blockId, targetItemId) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val dropped = event.toAndroidDragEvent().clipData
                    ?.takeIf { it.itemCount > 0 }
                    ?.getItemAt(0)?.text?.toString()
                    ?: return false
                if (dropped == targetItemId) return false
                // 割当（日 → ブロック）を先に更新してから並べ替える。
                store.assignItemToDay(planId, dropped, day)
                if (day != null) store.assignItemToBlock(planId, dropped, blockId)
                store.moveItemBefore(planId, dropped, targetItemId)
                onHover(false)
                return true
            }

            override fun onEntered(event: DragAndDropEvent) { onHover(true) }
            override fun onExited(event: DragAndDropEvent) { onHover(false) }
            override fun onEnded(event: DragAndDropEvent) { onHover(false) }
        }
    }
    dragAndDropTarget(
        shouldStartDragAndDrop = { event -> event.mimeTypes().contains(PLAN_ITEM_MIME) },
        target = callback,
    )
}

/**
 * 長押しでプラットフォームのドラッグ&ドロップを開始し、項目 id をプレーンテキストで運ぶ。
 * 受け側は [planDropTarget]（Compose の dragAndDropTarget）。長押し前に指が動いたときは
 * 何もしないので、親の縦スクロールはそのまま効く。
 */
private fun Modifier.longPressDragSource(itemId: String): Modifier = composed {
    val view = LocalView.current
    val density = LocalDensity.current
    pointerInput(itemId) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                val shadow = PlanDragShadow(with(density) { 220.dp.roundToPx() }, with(density) { 48.dp.roundToPx() })
                view.startDragAndDrop(ClipData.newPlainText(itemId, itemId), shadow, null, 0)
            },
            onDrag = { change, _ -> change.consume() },
        )
    }
}

/** ドラッグ中に指の下に出す影（ブランド色の角丸バー）。 */
private class PlanDragShadow(private val w: Int, private val h: Int) : android.view.View.DragShadowBuilder() {
    override fun onProvideShadowMetrics(outShadowSize: android.graphics.Point, outShadowTouchPoint: android.graphics.Point) {
        outShadowSize.set(w, h)
        outShadowTouchPoint.set(w / 2, h / 2)
    }

    override fun onDrawShadow(canvas: android.graphics.Canvas) {
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCFF5100.toInt() }
        canvas.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), h / 2f, h / 2f, paint)
    }
}

/**
 * マイプランタブ。プラン一覧（一覧／訪問済み）⇄ プラン詳細（旅程／費用）を切り替える。
 * iOS 版 MyPlansView / PlanDetailView（3.0.0）を移植。
 *
 * @param fixedPlanId ホームの「次の旅」から開いたときのプラン。指定時は詳細だけを出し、戻ると [onExitFixedPlan]。
 */
@Composable
fun PlanScreen(
    store: TravelPlanStore,
    fixedPlanId: String? = null,
    onExitFixedPlan: () -> Unit = {},
) {
    var openPlanId by remember { mutableStateOf(fixedPlanId) }
    // 項目タップで開く詳細（アプリ項目 = SpotDetail、カスタム項目 = PlanItem）。プラン詳細の上に重ねる。
    var openedSpot by remember { mutableStateOf<SpotDetail?>(null) }
    // グルメ・お土産で「位置を追加」を出すため、開いた元のプラン項目も保持する（それ以外は null）。
    var openedSpotItem by remember { mutableStateOf<PlanItem?>(null) }
    var openedCustom by remember { mutableStateOf<PlanItem?>(null) }
    var editingCustom by remember { mutableStateOf<PlanItem?>(null) }
    var showMap by remember { mutableStateOf(false) }
    var showPhotoCredits by remember { mutableStateOf(false) }
    // 県タイルから開く県詳細（プラン詳細から抜けずにスポットを追加できる）。
    var openedPrefecture by remember { mutableStateOf<Prefecture?>(null) }

    val current = openPlanId?.let { store.plan(it) }

    val openItem: (PlanItem) -> Unit = { item ->
        if (item.category in CUSTOM_PLAN_CATEGORIES) {
            openedCustom = item
        } else {
            spotDetailForPlanItem(item)?.let {
                openedSpot = it
                openedSpotItem = item
            }
        }
    }

    if (current != null) {
        PlanDetailScreen(
            plan = current,
            store = store,
            onBack = {
                if (fixedPlanId != null) onExitFixedPlan() else openPlanId = null
            },
            onOpenItem = openItem,
            onOpenMap = { showMap = true },
            onOpenPrefecture = { openedPrefecture = it },
        )
    } else if (fixedPlanId != null) {
        // 削除などでプランが無くなった。
        LaunchedEffect(Unit) { onExitFixedPlan() }
    } else {
        PlanListScreen(
            store = store,
            onOpenPlan = { openPlanId = it },
            onOpenPhotoCredits = { showPhotoCredits = true },
        )
    }

    // 以下は iOS の sheet / fullScreenCover。下タブも覆う全画面で、下のプラン画面の上に重ねる。
    if (showPhotoCredits) {
        FullScreenSheet(onDismiss = { showPhotoCredits = false }) {
            PhotoCreditsScreen(onBack = { showPhotoCredits = false })
        }
    }
    if (showMap && current != null) {
        FullScreenSheet(onDismiss = { showMap = false }) {
            PlanMapScreen(plan = current, onOpenItem = openItem, onClose = { showMap = false })
        }
    }
    openedPrefecture?.let { pref ->
        FullScreenSheet(onDismiss = { openedPrefecture = null }) {
            TourismDetailScreen(
                prefecture = pref,
                store = store,
                onOpenSpot = { openedSpot = it },
                onBack = { openedPrefecture = null },
            )
        }
    }
    val spot = openedSpot
    if (spot != null) {
        val planContext = openedSpotItem?.let { item -> current?.let { SpotPlanContext(planId = it.id, planItem = item) } }
        val close = {
            openedSpot = null
            openedSpotItem = null
        }
        FullScreenSheet(onDismiss = close) {
            SpotDetailScreen(detail = spot, store = store, onClose = close, planContext = planContext)
        }
    }
    val customItem = openedCustom
    if (customItem != null && current != null) {
        val latest = current.items.firstOrNull { it.id == customItem.id } ?: customItem
        FullScreenSheet(onDismiss = { openedCustom = null }) {
            CustomPlanItemScreen(item = latest, onEdit = { editingCustom = latest }, onClose = { openedCustom = null })
        }
    }
    val editing = editingCustom
    if (editing != null && current != null) {
        FullScreenSheet(onDismiss = { editingCustom = null }) {
            CustomPlanItemSheet(
                editing = editing,
                onDismiss = { editingCustom = null },
                onSave = { updated ->
                    store.updateItem(current.id, updated)
                    editingCustom = null
                    openedCustom = updated
                },
            )
        }
    }
}

// MARK: - 一覧

@Composable
private fun PlanListScreen(
    store: TravelPlanStore,
    onOpenPlan: (String) -> Unit,
    onOpenPhotoCredits: () -> Unit,
) {
    var section by remember { mutableStateOf(0) } // 0: 一覧 / 1: 訪問済み
    var showCreate by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }

    // ホームの「マイプランへ」から新規作成シートの予約が来ていたら消化する。
    LaunchedEffect(AppRouter.pendingNewPlan) {
        if (AppRouter.pendingNewPlan) {
            AppRouter.pendingNewPlan = false
            section = 0
            showCreate = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(PlanTheme.pageBackground).statusBarsPadding()) {
        // 上部バー（左=フォトクレジット、右=並び替えの編集トグル）。
        Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
            IconButton(onClick = onOpenPhotoCredits, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.photo_credits_attribution), tint = AppTheme.TextSecondary)
            }
            if (section == 0 && store.plans.size > 1) {
                TextButton(onClick = { editMode = !editMode }, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Text(
                        stringResource(if (editMode) R.string.common_done else R.string.common_edit),
                        color = PlanTheme.Primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                }
            }
        }
        PlanSectionBand(
            titles = listOf(stringResource(R.string.plan_section_list), stringResource(R.string.plan_section_visited)),
            selectedIndex = section,
            onSelect = {
                section = it
                editMode = false
            },
        )

        Box(modifier = Modifier.weight(1f)) {
            if (section == 1) {
                VisitedMapScreen(store = store, onOpenPlan = onOpenPlan)
            } else if (store.plans.isEmpty()) {
                PlanEmptyState(modifier = Modifier.fillMaxSize(), onCreate = { showCreate = true })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(store.plans, key = { it.id }) { plan ->
                        val index = store.plans.indexOfFirst { it.id == plan.id }
                        PlanCard(
                            plan = plan,
                            store = store,
                            editMode = editMode,
                            canMoveUp = index > 0,
                            canMoveDown = index < store.plans.size - 1,
                            onClick = { if (!editMode) onOpenPlan(plan.id) },
                            onDelete = { store.deletePlan(plan.id) },
                            onMoveUp = { store.movePlan(index, index - 1) },
                            onMoveDown = { store.movePlan(index, index + 1) },
                        )
                    }
                    // 新規プラン追加（最後のカードの下・破線枠）。並び替え中は隠す。
                    if (!editMode) {
                        item(key = "add_plan") {
                            DashedAddButton(
                                text = stringResource(R.string.plan_create_title),
                                icon = Icons.Filled.Add,
                                onClick = { showCreate = true },
                                corner = PlanTheme.cardCorner,
                                verticalPadding = 16.dp,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreatePlanSheet(
            onDismiss = { showCreate = false },
            onCreate = { title ->
                store.createPlan(title)
                showCreate = false
            },
        )
    }
}

/** 空状態（グラデーション円＋案内＋作成ボタン）。 */
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
            Icon(Icons.Filled.Luggage, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(stringResource(R.string.plan_empty_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Text(stringResource(R.string.plan_empty_message), fontSize = 14.sp, color = AppTheme.TextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        PlanPrimaryButton(text = stringResource(R.string.plan_create_title), icon = Icons.Filled.Add, onClick = onCreate)
    }
}

/** 状態のアイコン（iOS: calendar / airplane / checkmark.seal.fill）。 */
private fun PlanStatus.icon(): ImageVector = when (this) {
    PlanStatus.UPCOMING -> Icons.Filled.CalendarMonth
    PlanStatus.ONGOING -> Icons.Filled.Flight
    PlanStatus.COMPLETED -> Icons.Filled.Verified
}

/**
 * プランカード。iOS 版 PlanCardView。
 * 上部：ブランドグラデのヘッダー（タイトル＋状態チップ＋件数）。下部：代表カテゴリのアイコン（最大 4）＋日程日数。
 * 状態チップはその場で 3 択メニューを開き、詳細へ進まずに状態を変えられる。
 */
@Composable
private fun PlanCard(
    plan: TravelPlan,
    store: TravelPlanStore,
    editMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val categoryIcons = remember(plan.items) { plan.itineraryItems.map { it.category }.distinct().take(4) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(PlanTheme.cardCorner), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(PlanTheme.cardCorner))
            .background(AppTheme.CardSurface)
            .clickable(enabled = !editMode, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(PlanTheme.brandGradient).padding(18.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        plan.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusMenuChip(plan.status, onSelect = { store.setStatus(plan.id, it) })
                }
                Text(
                    stringResource(R.string.plan_itemcount_format, plan.itineraryItems.size),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            if (editMode) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.plan_move_up), tint = Color.White.copy(alpha = if (canMoveUp) 1f else 0.35f))
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.plan_move_down), tint = Color.White.copy(alpha = if (canMoveDown) 1f else 0.35f))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_delete), tint = Color.White)
                }
            } else {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White.copy(alpha = 0.8f))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(PlanTheme.groupedSurface).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (categoryIcons.isEmpty()) {
                Text(stringResource(R.string.plan_card_empty), fontSize = 12.sp, color = AppTheme.TextSecondary)
            } else {
                categoryIcons.forEach { category ->
                    CategoryAvatar(category, size = 30, iconSize = 15)
                    Spacer(modifier = Modifier.width(10.dp))
                }
                if (plan.groupingMode == PlanGroupingMode.DAY) {
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.plan_daycount_short, plan.dayCount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                }
            }
        }
    }
}

/** 状態チップ兼メニュー（白カプセル。タップで 計画中/進行中/旅行済み を選ぶ）。 */
@Composable
private fun StatusMenuChip(status: PlanStatus, onSelect: (PlanStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(status.icon(), contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(status.labelRes), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
            Spacer(modifier = Modifier.width(2.dp))
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(12.dp))
        }
        StatusDropdown(expanded, status, onDismiss = { expanded = false }, onSelect = {
            expanded = false
            onSelect(it)
        })
    }
}

@Composable
private fun StatusDropdown(expanded: Boolean, current: PlanStatus, onDismiss: () -> Unit, onSelect: (PlanStatus) -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = Color.White) {
        PlanStatus.entries.forEach { s ->
            DropdownMenuItem(
                text = { Text(stringResource(s.labelRes), fontWeight = if (s == current) FontWeight.Bold else FontWeight.Normal) },
                leadingIcon = { Icon(s.icon(), contentDescription = null, tint = if (s == current) PlanTheme.Primary else AppTheme.TextSecondary) },
                onClick = { onSelect(s) },
            )
        }
    }
}

// MARK: - 詳細

/**
 * プラン詳細。iOS 版 PlanDetailView（3.0.0）。
 * 上部の切替帯で旅程／費用。旅程は通常モードでは静かに表示し、「編集」で設定カード・削除・
 * ドラッグ・各種追加ボタン・状態切替を出す。右下に「スポット追加」（編集中・フラット表示）と「地図」の FAB。
 */
@Composable
private fun PlanDetailScreen(
    plan: TravelPlan,
    store: TravelPlanStore,
    onBack: () -> Unit,
    onOpenItem: (PlanItem) -> Unit,
    onOpenMap: () -> Unit,
    onOpenPrefecture: (Prefecture) -> Unit,
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)
    var section by remember { mutableStateOf(0) } // 0: 旅程 / 1: 費用
    var isEditing by remember { mutableStateOf(false) }
    var showTitleEditor by remember { mutableStateOf(false) }
    var showMemoEditor by remember { mutableStateOf(false) }
    var showPrefecturePicker by remember { mutableStateOf(false) }
    // スポットのクイック追加（Day 起点なら日を持つ。-1 は未表示）。
    var quickAddDay by remember { mutableStateOf<Int?>(null) }
    var showQuickAdd by remember { mutableStateOf(false) }
    var showAddCustom by remember { mutableStateOf(false) }
    var addCustomDay by remember { mutableStateOf<Int?>(null) }
    var editingBlock by remember { mutableStateOf<TimeBlock?>(null) }
    val hasMappable = remember(plan) { plan.itineraryItems.any { coordinateForPlanItem(it) != null } }

    // シート類は下タブも覆う全画面で重ねる（iOS の sheet）。
    if (showMemoEditor) {
        FullScreenSheet(onDismiss = { showMemoEditor = false }) {
            PlanMemoEditorScreen(
                initial = plan.memo,
                onDismiss = { showMemoEditor = false },
                onSave = {
                    store.updateMemo(plan.id, it)
                    showMemoEditor = false
                },
            )
        }
    }
    if (showPrefecturePicker) {
        FullScreenSheet(onDismiss = { showPrefecturePicker = false }) {
            PrefecturePickerScreen(
                excluded = plan.prefectures.toSet(),
                onAdd = {
                    store.addPrefectures(plan.id, it)
                    showPrefecturePicker = false
                },
                onDismiss = { showPrefecturePicker = false },
            )
        }
    }
    if (showQuickAdd) {
        FullScreenSheet(onDismiss = { showQuickAdd = false }) {
            SpotQuickAddScreen(
                store = store,
                planId = plan.id,
                suggestedPrefectures = plan.prefectures,
                initialDay = quickAddDay,
                onDismiss = { showQuickAdd = false },
            )
        }
    }
    if (showAddCustom) {
        FullScreenSheet(onDismiss = { showAddCustom = false }) {
            CustomPlanItemSheet(
                onDismiss = { showAddCustom = false },
                onSave = { item ->
                    store.addItem(plan.id, item.copy(dayNumber = addCustomDay))
                    showAddCustom = false
                },
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(PlanTheme.pageBackground)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // トップバー（戻る / タイトル（編集中はタップで編集） / 共有・編集）。
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = isEditing) { showTitleEditor = true }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(plan.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (isEditing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(14.dp))
                    }
                }
                if (section == 0 && !isEditing) {
                    IconButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, PlanShareFormatter.text(context, plan))
                        }
                        context.startActivity(Intent.createChooser(send, context.getString(R.string.plan_share_chooser)))
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.plan_share), tint = PlanTheme.Primary)
                    }
                }
                if (section == 0) {
                    TextButton(onClick = { isEditing = !isEditing }) {
                        Text(
                            stringResource(if (isEditing) R.string.common_done else R.string.common_edit),
                            color = PlanTheme.Primary,
                            fontWeight = if (isEditing) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 16.sp,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            PlanSectionBand(
                titles = listOf(stringResource(R.string.plan_section_itinerary), stringResource(R.string.plan_section_cost)),
                selectedIndex = section,
                onSelect = {
                    section = it
                    // 費用タブへ移ったら編集モードは抜ける（編集は旅程タブ専用）。
                    if (it != 0) isEditing = false
                },
            )

            if (section == 1) {
                Box(modifier = Modifier.weight(1f)) { PlanCostTab(plan = plan, store = store) }
            } else {
                ItineraryContent(
                    plan = plan,
                    store = store,
                    isEditing = isEditing,
                    onOpenItem = onOpenItem,
                    onOpenPrefecture = onOpenPrefecture,
                    onAddPrefecture = { showPrefecturePicker = true },
                    onEditMemo = { showMemoEditor = true },
                    onAddCustom = { day -> addCustomDay = day; showAddCustom = true },
                    onQuickAdd = { day -> quickAddDay = day; showQuickAdd = true },
                    onEditBlock = { editingBlock = it },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 右下の FAB 群（旅程タブのみ）。上から「スポット追加」→「地図」。
        if (section == 0) {
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // スポット追加は編集中のフラット表示だけ（日程モードは各 Day に追加ボタンがある）。
                if (isEditing && plan.groupingMode != PlanGroupingMode.DAY) {
                    PlanFab(Icons.Filled.Add, stringResource(R.string.plan_spot_add)) { quickAddDay = null; showQuickAdd = true }
                }
                if (hasMappable) {
                    PlanFab(Icons.Filled.Map, stringResource(R.string.plan_show_on_map), onClick = onOpenMap)
                }
            }
        }
    }

    if (showTitleEditor) {
        PlanTitleEditDialog(
            initial = plan.title,
            onDismiss = { showTitleEditor = false },
            onSave = {
                store.updateTitle(plan.id, it)
                showTitleEditor = false
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

/** 旅程タブの本体（県タイル → 設定カード → メモ → 項目 → 状態フッター）。 */
@Composable
private fun ItineraryContent(
    plan: TravelPlan,
    store: TravelPlanStore,
    isEditing: Boolean,
    onOpenItem: (PlanItem) -> Unit,
    onOpenPrefecture: (Prefecture) -> Unit,
    onAddPrefecture: () -> Unit,
    onEditMemo: () -> Unit,
    onAddCustom: (Int?) -> Unit,
    onQuickAdd: (Int?) -> Unit,
    onEditBlock: (TimeBlock) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PrefectureTilesCard(plan, store, isEditing, onOpenPrefecture, onAddPrefecture)

        if (isEditing) {
            ControlCard(plan, store, onEditMemo)
        }

        if (plan.memo.isNotEmpty()) {
            PlanMemoCard(memo = plan.memo, collapsible = !isEditing)
        }

        when {
            plan.groupingMode == PlanGroupingMode.DAY -> {
                plan.itemsByDay.forEach { (day, items) ->
                    DayDropSection(
                        day = day,
                        items = items,
                        plan = plan,
                        store = store,
                        isEditing = isEditing,
                        onOpenItem = onOpenItem,
                        onAddCustom = { onAddCustom(day) },
                        onQuickAdd = { onQuickAdd(day) },
                        onEditBlock = onEditBlock,
                    )
                }
            }
            plan.itineraryItems.isEmpty() -> {
                if (isEditing) {
                    Column(modifier = Modifier.fillMaxWidth().planCard(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(stringResource(R.string.plan_detail_empty), fontSize = 14.sp, color = AppTheme.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        DashedAddButton(stringResource(R.string.plan_add_custom), Icons.Filled.Add, onClick = { onAddCustom(null) })
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().planCard(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.plan_detail_empty), fontSize = 14.sp, color = AppTheme.TextSecondary, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.plan_detail_empty_edithint), fontSize = 12.sp, color = AppTheme.TextSecondary.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                    }
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    plan.itineraryItems.forEach { item ->
                        PlanItemCard(item, store, plan.id, draggable = false, isEditing = isEditing, onClick = { onOpenItem(item) })
                    }
                    if (isEditing) {
                        DashedAddButton(stringResource(R.string.plan_add_custom), Icons.Filled.Add, onClick = { onAddCustom(null) })
                    }
                }
            }
        }

        if (isEditing) {
            StatusFooter(plan.status) { store.setStatus(plan.id, it) }
        }
    }
}

/** 県タイル（代表写真＋県名）。タップで県詳細。編集中は末尾に「＋」、手動追加の県は長押しで削除。 */
@Composable
private fun PrefectureTilesCard(
    plan: TravelPlan,
    store: TravelPlanStore,
    isEditing: Boolean,
    onOpenPrefecture: (Prefecture) -> Unit,
    onAddPrefecture: () -> Unit,
) {
    val prefs = plan.prefectures
    if (prefs.isEmpty()) {
        if (isEditing) {
            Box(modifier = Modifier.fillMaxWidth().planCard()) {
                DashedAddButton(stringResource(R.string.plan_prefectures_add), Icons.Filled.Add, onClick = onAddPrefecture)
            }
        }
        return
    }
    WrapRow(
        modifier = Modifier.fillMaxWidth().planCard(),
    ) {
        prefs.forEach { pref ->
            PrefectureTile(
                prefecture = pref,
                isEditing = isEditing,
                isRemovable = pref.name in plan.plannedPrefectures,
                onClick = { onOpenPrefecture(pref) },
                onDelete = { store.removePrefecture(plan.id, pref) },
            )
        }
        if (isEditing) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .dashedBorder(PlanTheme.Primary.copy(alpha = 0.4f), 10.dp, width = 1.5.dp)
                    .clickable(onClick = onAddPrefecture),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.plan_prefectures_add), tint = PlanTheme.Primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PrefectureTile(prefecture: Prefecture, isEditing: Boolean, isRemovable: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val heroId = prefecture.heroPhotoResName()?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0
    var menu by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier = Modifier
                .width(70.dp)
                .height(44.dp)
                .shadow(5.dp, RoundedCornerShape(10.dp), spotColor = PlanTheme.cardShadow)
                .clip(RoundedCornerShape(10.dp))
                // 長押しメニューは編集モードのときだけ（出すものが無いときに長押しを効かせない）。
                .combinedClickable(onClick = onClick, onLongClick = if (isEditing) ({ menu = true }) else null),
        ) {
            if (heroId != 0) {
                Image(painterResource(heroId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(modifier = Modifier.fillMaxSize().background(PlanTheme.brandGradient))
            }
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.65f)))))
            Text(
                prefecture.localizedName(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, end = 6.dp, bottom = 5.dp),
            )
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Color.White) {
            if (isRemovable) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.common_delete), color = Color(0xFFFF3B30)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFF3B30)) },
                    onClick = {
                        menu = false
                        onDelete()
                    },
                )
            } else {
                // スポット由来の県。スポットを消せば自動で消えることを伝える（押せないようにする）。
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.plan_prefectures_derived)) },
                    leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    onClick = {},
                    enabled = false,
                )
            }
        }
    }
}

/** 設定カード（メモ編集・日程で分ける・日数）。編集モードのときだけ出す。 */
@Composable
private fun ControlCard(plan: TravelPlan, store: TravelPlanStore, onEditMemo: () -> Unit) {
    val dayMode = plan.groupingMode == PlanGroupingMode.DAY
    Column(modifier = Modifier.fillMaxWidth().planCard(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onEditMemo),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.plan_item_editmemo), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AppTheme.TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        }
        HorizontalDivider(color = Color(0x14000000))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = AppTheme.TextPrimary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.plan_split_by_day), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Switch(
                checked = dayMode,
                onCheckedChange = { store.setGroupingMode(plan.id, if (it) PlanGroupingMode.DAY else PlanGroupingMode.FLAT) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PlanTheme.Primary, checkedBorderColor = PlanTheme.Primary),
            )
        }
        if (dayMode) {
            HorizontalDivider(color = Color(0x14000000))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.plan_days_count, plan.dayCount), fontSize = 15.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { store.setDayCount(plan.id, plan.dayCount - 1) }, enabled = plan.dayCount > 1) {
                    Icon(Icons.Filled.RemoveCircleOutline, contentDescription = stringResource(R.string.plan_decrease), tint = if (plan.dayCount > 1) PlanTheme.Primary else Color.Gray)
                }
                IconButton(onClick = { store.setDayCount(plan.id, plan.dayCount + 1) }, enabled = plan.dayCount < 30) {
                    Icon(Icons.Filled.AddCircleOutline, contentDescription = stringResource(R.string.plan_increase), tint = if (plan.dayCount < 30) PlanTheme.Primary else Color.Gray)
                }
            }
        }
    }
}

/** メモ表示カード。collapsible のとき 3 行を超えるメモは畳み、「続きを読む」で展開する。 */
@Composable
private fun PlanMemoCard(memo: String, collapsible: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    var truncatable by remember(memo) { mutableStateOf(false) }
    val showFull = !collapsible || expanded
    Column(modifier = Modifier.fillMaxWidth().planCard(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.plan_memo_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
        }
        Text(
            memo,
            fontSize = 15.sp,
            color = AppTheme.TextPrimary,
            maxLines = if (showFull) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!showFull) truncatable = it.hasVisualOverflow },
            modifier = Modifier.fillMaxWidth(),
        )
        if (collapsible && (truncatable || expanded)) {
            Text(
                stringResource(if (expanded) R.string.common_collapse else R.string.common_readmore),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PlanTheme.Primary,
                modifier = Modifier.clickable { expanded = !expanded },
            )
        }
    }
}

/** リスト末尾の控えめな進行状態の切替（編集モードのみ）。旅行済みにすると訪問済みに反映される。 */
@Composable
private fun StatusFooter(status: PlanStatus, onSelect: (PlanStatus) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Flag, contentDescription = null, tint = AppTheme.TextSecondary, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(stringResource(R.string.plan_status_label), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextSecondary, modifier = Modifier.weight(1f))
        Box {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { expanded = true }.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(status.icon(), contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(status.labelRes), fontSize = 15.sp, color = PlanTheme.Primary)
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(16.dp))
            }
            StatusDropdown(expanded, status, onDismiss = { expanded = false }, onSelect = {
                expanded = false
                onSelect(it)
            })
        }
    }
}

// MARK: - 日程（Day）セクション

/** 1 日分のセクション。実日は時間ブロックごとのタイムライン、未割当はフラット。カードの隙間へのドロップは末尾へ。 */
@Composable
private fun DayDropSection(
    day: Int?,
    items: List<PlanItem>,
    plan: TravelPlan,
    store: TravelPlanStore,
    isEditing: Boolean,
    onOpenItem: (PlanItem) -> Unit,
    onAddCustom: () -> Unit,
    onQuickAdd: () -> Unit,
    onEditBlock: (TimeBlock) -> Unit,
) {
    var hovered by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(PlanTheme.cardCorner), spotColor = PlanTheme.cardShadow)
            .clip(RoundedCornerShape(PlanTheme.cardCorner))
            .background(PlanTheme.groupedSurface)
            .then(if (hovered) Modifier.dashedBorder(PlanTheme.Primary, PlanTheme.cardCorner, width = 2.dp, dash = 6.dp) else Modifier)
            .then(
                if (isEditing) Modifier.planDropTarget(store, plan.id, day, null, null) { hovered = it } else Modifier,
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (day != null) stringResource(R.string.plan_day_format, day) else stringResource(R.string.plan_unassigned),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (day == null) AppTheme.TextSecondary else PlanTheme.Primary,
                modifier = Modifier.weight(1f),
            )
            if (isEditing && day != null) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onAddCustom).padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(stringResource(R.string.plan_add_custom), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                }
            }
        }

        if (day == null) {
            if (items.isEmpty()) {
                Text(
                    stringResource(R.string.plan_day_empty),
                    fontSize = 12.sp,
                    color = AppTheme.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(top = 12.dp),
                )
            } else {
                items.forEach { item ->
                    PlanItemCard(item, store, plan.id, draggable = isEditing, isEditing = isEditing, onClick = { onOpenItem(item) }, dropDay = null, dropBlockId = null)
                }
            }
        } else {
            plan.blockSections(day).forEach { (block, blockItems) ->
                TimelineBlockRow(
                    day = day,
                    block = block,
                    items = blockItems,
                    plan = plan,
                    store = store,
                    isEditing = isEditing,
                    onOpenItem = onOpenItem,
                    onEditBlock = onEditBlock,
                )
            }
            if (isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .dashedBorder(PlanTheme.Primary.copy(alpha = 0.4f), 12.dp)
                        .clickable {
                            // 現在時刻で時間帯を追加する（iOS と同じ）。
                            val now = Calendar.getInstance()
                            store.addTimeBlock(plan.id, day, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
                        }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.MoreTime, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.plan_add_time_block), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PlanTheme.brandGradient)
                        .clickable(onClick = onQuickAdd)
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.plan_spot_add), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * 時間ブロック 1 行（左=時刻・ドット・接続線、右=見出し＋項目）。block が null は「時間未割当」の受け皿。
 * ブロック全体へのドロップはそのブロックの末尾へ移動。
 */
@Composable
private fun TimelineBlockRow(
    day: Int,
    block: TimeBlock?,
    items: List<PlanItem>,
    plan: TravelPlan,
    store: TravelPlanStore,
    isEditing: Boolean,
    onOpenItem: (PlanItem) -> Unit,
    onEditBlock: (TimeBlock) -> Unit,
) {
    var hovered by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(12.dp))
            .background(if (hovered) PlanTheme.Primary.copy(alpha = 0.06f) else Color.Transparent)
            .then(if (isEditing) Modifier.planDropTarget(store, plan.id, day, block?.id, null) { hovered = it } else Modifier)
            .padding(vertical = 4.dp),
    ) {
        // 時刻列（時刻未設定のブロックはラベルを出さずドットと線だけ）。
        Column(modifier = Modifier.width(52.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            val label = block?.timeLabel
            if (label != null) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(if (block == null) AppTheme.TextSecondary.copy(alpha = 0.5f) else PlanTheme.Primary),
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(2.dp)
                    .weight(1f)
                    .background(if (block == null) AppTheme.TextSecondary.copy(alpha = 0.3f) else PlanTheme.Primary.copy(alpha = 0.35f)),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (block != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (block.title.isNotEmpty()) {
                        Text(block.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    if (isEditing) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = stringResource(R.string.plan_edit_time),
                            tint = PlanTheme.Primary,
                            modifier = Modifier.size(16.dp).clickable { onEditBlock(block) },
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.plan_delete_block),
                            tint = AppTheme.TextSecondary,
                            modifier = Modifier.size(16.dp).clickable { confirmDelete = true },
                        )
                    }
                }
            }
            if (items.isEmpty()) {
                // 空ブロックのドロップ受け皿は編集モードのときだけ。
                if (isEditing) {
                    Text(
                        stringResource(R.string.plan_timeblock_empty),
                        fontSize = 11.sp,
                        color = AppTheme.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .dashedBorder(PlanTheme.Primary.copy(alpha = 0.3f), 12.dp, width = 1.5.dp, dash = 5.dp)
                            .padding(top = 20.dp),
                    )
                }
            } else {
                items.forEach { item ->
                    PlanItemCard(item, store, plan.id, draggable = isEditing, isEditing = isEditing, onClick = { onOpenItem(item) }, dropDay = day, dropBlockId = block?.id)
                }
            }
        }
    }

    if (confirmDelete && block != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.plan_timeblock_delete_confirm)) },
            text = { Text(stringResource(R.string.plan_timeblock_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    store.removeTimeBlock(plan.id, block.id)
                }) { Text(stringResource(R.string.common_delete), color = Color(0xFFFF3B30)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.common_cancel), color = PlanTheme.Primary) }
            },
            containerColor = Color.White,
        )
    }
}

/**
 * 旅程の項目カード。通常は「タップで詳細」だけの静かなカード。
 * 編集中は削除ボタン（−）を出し、日程モードではドラッグハンドル＋長押しドラッグ・この項目の直前へのドロップを受ける。
 * フラット表示の編集中は長押しで削除メニュー。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlanItemCard(
    item: PlanItem,
    store: TravelPlanStore,
    planId: String,
    draggable: Boolean,
    isEditing: Boolean,
    onClick: () -> Unit,
    dropDay: Int? = null,
    dropBlockId: String? = null,
) {
    var hovered by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val memo = item.userMemo

    val interaction = when {
        draggable -> Modifier
            .planDropTarget(store, planId, dropDay, dropBlockId, item.id) { hovered = it }
            .longPressDragSource(item.id)
            .clickable(onClick = onClick)
        isEditing -> Modifier.combinedClickable(onClick = onClick, onLongClick = { menu = true })
        else -> Modifier.clickable(onClick = onClick)
    }

    Box {
        Column {
            // ドロップ先がこのカードのとき、直前に挿入されることを示す線。
            if (hovered) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(PlanTheme.Primary),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (draggable) PlanTheme.groupedSurface2 else Color.White)
                    .then(interaction)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryAvatar(item.category, size = 36, iconSize = 18)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(localizeData(item.name), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (memo.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = AppTheme.TextSecondary, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(memo, fontSize = 11.sp, color = AppTheme.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    } else if (!item.category.isCustom) {
                        val pref = prefectureFromDisplayName(item.prefectureName)
                        Text(
                            listOfNotNull(stringResource(item.category.labelRes), pref?.localizedName()).joinToString(" · "),
                            fontSize = 11.sp,
                            color = AppTheme.TextSecondary,
                            maxLines = 1,
                        )
                    }
                }
                if (isEditing) {
                    Icon(
                        Icons.Filled.RemoveCircle,
                        contentDescription = stringResource(R.string.common_delete),
                        tint = AppTheme.TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp).clickable { store.removeItem(planId, item.id) },
                    )
                    if (draggable) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.DragHandle, contentDescription = stringResource(R.string.plan_reorder_hint), tint = PlanTheme.Primary.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                } else {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AppTheme.TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = Color.White) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.common_delete), color = Color(0xFFFF3B30)) },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFF3B30)) },
                onClick = {
                    menu = false
                    store.removeItem(planId, item.id)
                },
            )
        }
    }
}

// MARK: - 時間ブロック編集

/** 時間ブロックの開始時刻（設定あり/なし）・見出しの編集シート。削除もここから行う。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeBlockEditDialog(
    block: TimeBlock,
    onDismiss: () -> Unit,
    onSave: (TimeBlock) -> Unit,
    onDelete: () -> Unit,
) {
    var hasTime by remember { mutableStateOf(block.startHour != null) }
    var hour by remember { mutableStateOf(block.startHour ?: 9) }
    var minute by remember { mutableStateOf(block.startMinute ?: 0) }
    var title by remember { mutableStateOf(block.title) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = PlanTheme.Primary) }
                Text(stringResource(R.string.plan_time_block), fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    onSave(
                        block.copy(
                            startHour = if (hasTime) hour else null,
                            startMinute = if (hasTime) minute else null,
                            title = title.trim(),
                        ),
                    )
                }) { Text(stringResource(R.string.common_save), color = PlanTheme.Primary, fontWeight = FontWeight.SemiBold) }
            }
            Column(modifier = Modifier.fillMaxWidth().planCard(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = AppTheme.TextPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.plan_timeblock_settime), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Switch(
                        checked = hasTime,
                        onCheckedChange = { hasTime = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PlanTheme.Primary, checkedBorderColor = PlanTheme.Primary),
                    )
                }
                if (hasTime) {
                    HorizontalDivider(color = Color(0x14000000))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.plan_start_time), fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        StepperField(value = hour, range = 0..23, onChange = { hour = it }, suffix = stringResource(R.string.plan_hour_suffix))
                        Spacer(modifier = Modifier.width(8.dp))
                        StepperField(value = minute, range = 0..59, step = 5, onChange = { minute = it }, suffix = stringResource(R.string.plan_minute_suffix))
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth().planCard(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.plan_timeblock_title_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(stringResource(R.string.plan_timeblock_title_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PlanTheme.Primary, cursorColor = PlanTheme.Primary),
                )
            }
            TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFF3B30), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.plan_delete_this_block), color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 数値の＋/−ステッパー。 */
@Composable
private fun StepperField(value: Int, range: IntRange, step: Int = 1, onChange: (Int) -> Unit, suffix: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange((value - step).coerceIn(range)) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.RemoveCircleOutline, contentDescription = stringResource(R.string.plan_decrease), tint = PlanTheme.Primary, modifier = Modifier.size(20.dp))
        }
        Text("%02d%s".format(value, suffix), fontSize = 15.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onChange((value + step).coerceIn(range)) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.AddCircleOutline, contentDescription = stringResource(R.string.plan_increase), tint = PlanTheme.Primary, modifier = Modifier.size(20.dp))
        }
    }
}

// MARK: - 新規作成

/** プラン新規作成シート。iOS 版 MyPlansView.newPlanSheet。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePlanSheet(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel), color = PlanTheme.Primary) }
                Text(stringResource(R.string.plan_create_title), fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(72.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.plan_title_label), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextSecondary)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(stringResource(R.string.plan_title_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PlanTheme.Primary, cursorColor = PlanTheme.Primary),
                )
            }
            PlanPrimaryButton(text = stringResource(R.string.common_create), modifier = Modifier.fillMaxWidth(), onClick = { onCreate(title.trim()) })
        }
    }
}
