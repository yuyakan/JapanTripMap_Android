package com.example.japantripmap

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// MARK: - 精算ロジック（iOS 版 SettlementCalculator）

/** メンバー 1 人分の純収支（> 0 受け取る / < 0 支払う）。 */
data class MemberBalance(val id: String, val name: String, val balance: Int)

/** 1 件の送金（from が to へ amount 円払う）。 */
data class SettlementTransfer(val fromName: String, val toName: String, val amount: Int)

object SettlementCalculator {
    /**
     * 各項目の支払者・分担者から各メンバーの純収支を求め、送金回数を減らした送金リストを返す。
     * 分担額は切り捨てで各分担者に配り、割り切れない端数は支払者の負担に寄せる（合計が必ず一致する）。
     */
    fun settle(plan: TravelPlan): Pair<List<MemberBalance>, List<SettlementTransfer>> {
        if (plan.members.isEmpty()) return emptyList<MemberBalance>() to emptyList()
        val net = LinkedHashMap<String, Int>().apply { plan.members.forEach { put(it.id, 0) } }
        for (item in plan.costItems) {
            val cost = item.cost ?: continue
            val payer = item.payerId ?: continue
            if (cost <= 0 || payer !in net) continue
            val sharers = plan.effectiveSplitMemberIds(item)
            if (sharers.isEmpty()) continue
            val base = cost / sharers.size
            val remainder = cost - base * sharers.size
            net[payer] = net.getValue(payer) + cost
            sharers.forEach { s -> net[s] = (net[s] ?: 0) - base }
            net[payer] = net.getValue(payer) - remainder
        }
        val balances = plan.members.map { MemberBalance(it.id, it.name, net[it.id] ?: 0) }
        return balances to minimizeTransfers(net, plan)
    }

    /** 最大の債務者から最大の債権者へまとめて払う Greedy 法。 */
    private fun minimizeTransfers(net: Map<String, Int>, plan: TravelPlan): List<SettlementTransfer> {
        val debtors = net.filter { it.value < 0 }.map { it.key to it.value }.sortedBy { it.second }.toMutableList()
        val creditors = net.filter { it.value > 0 }.map { it.key to it.value }.sortedByDescending { it.second }.toMutableList()
        val result = mutableListOf<SettlementTransfer>()
        var di = 0
        var ci = 0
        while (di < debtors.size && ci < creditors.size) {
            val debt = -debtors[di].second
            val credit = creditors[ci].second
            val pay = minOf(debt, credit)
            if (pay > 0) {
                result += SettlementTransfer(
                    plan.memberName(debtors[di].first) ?: "",
                    plan.memberName(creditors[ci].first) ?: "",
                    pay,
                )
            }
            debtors[di] = debtors[di].first to debtors[di].second + pay
            creditors[ci] = creditors[ci].first to creditors[ci].second - pay
            if (debtors[di].second == 0) di++
            if (creditors[ci].second == 0) ci++
        }
        return result
    }

    /** 精算が意味を持つか（メンバーがいて、支払者・分担が揃う金額付き項目が 1 件以上）。 */
    fun hasSettlementData(plan: TravelPlan): Boolean {
        if (plan.members.isEmpty()) return false
        return plan.costItems.any { (it.cost ?: 0) > 0 && it.payerId != null && plan.effectiveSplitMemberIds(it).isNotEmpty() }
    }
}

// MARK: - 費用タブ

/**
 * プラン詳細の「費用」タブ。iOS 版 PlanCostView。
 * メンバー（1 行サマリー→編集画面）、一覧/精算の切替、合計カード、費用専用項目の一覧と追加。
 */
@Composable
fun PlanCostTab(plan: TravelPlan, store: TravelPlanStore) {
    var showMembers by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PlanItem?>(null) }
    var addingExpense by remember { mutableStateOf(false) }
    var settlementTab by remember { mutableStateOf(false) }

    if (showMembers) {
        FullScreenSheet(onDismiss = { showMembers = false }) {
            MemberEditorScreen(plan = plan, store = store, onDone = { showMembers = false })
        }
    }
    if (addingExpense || editing != null) {
        FullScreenSheet(onDismiss = { addingExpense = false; editing = null }) {
        ExpenseEditorScreen(
            plan = plan,
            editing = editing,
            onDismiss = { addingExpense = false; editing = null },
            onSave = { title, cost, payer, split ->
                val target = editing
                if (target != null) {
                    store.updateItem(plan.id, target.copy(name = title, cost = cost, payerId = payer, splitMemberIds = split))
                } else {
                    store.addExpenseItem(plan.id, title, cost, payer, split)
                }
                addingExpense = false
                editing = null
            },
        )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MembersSummaryRow(plan) { showMembers = true }

        SegmentedControl(
            titles = listOf(stringResource(R.string.plan_cost_subtab_list), stringResource(R.string.plan_cost_subtab_settlement)),
            selectedIndex = if (settlementTab) 1 else 0,
            onSelect = { settlementTab = it == 1 },
        )

        if (!settlementTab) {
            TotalCard(plan.totalCost)
            val items = plan.costItems
            if (items.isEmpty()) {
                HintCard(stringResource(R.string.plan_cost_empty))
            } else {
                Column(modifier = Modifier.fillMaxWidth().planCard(padding = 8.dp)) {
                    items.forEachIndexed { i, item ->
                        CostRow(item, plan, onClick = { editing = item }, onDelete = { store.removeItem(plan.id, item.id) })
                        if (i < items.size - 1) HorizontalDivider(modifier = Modifier.padding(start = 48.dp), color = Color(0x14000000))
                    }
                }
            }
            DashedAddButton(
                text = stringResource(R.string.plan_expense_add),
                icon = Icons.Filled.Add,
                onClick = { addingExpense = true },
            )
        } else {
            SettlementSection(plan)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** iOS の segmented Picker に相当する 2 択の切替。 */
@Composable
private fun SegmentedControl(titles: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFFEEEEF0))
            .padding(2.dp),
    ) {
        titles.forEachIndexed { i, title ->
            val selected = i == selectedIndex
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = AppTheme.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .then(if (selected) Modifier.shadow(1.dp, RoundedCornerShape(7.dp)) else Modifier)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun MembersSummaryRow(plan: TravelPlan, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PlanTheme.groupedSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Group, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        if (plan.members.isEmpty()) {
            Text(stringResource(R.string.plan_members_add), fontSize = 15.sp, color = PlanTheme.Primary, modifier = Modifier.weight(1f))
        } else {
            Text(
                plan.members.joinToString("・") { it.name },
                fontSize = 15.sp,
                color = AppTheme.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.plan_members_count_format, plan.members.size), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextSecondary)
            Spacer(modifier = Modifier.weight(1f))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AppTheme.TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TotalCard(total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(PlanTheme.cardCorner), spotColor = PlanTheme.Primary.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(PlanTheme.cardCorner))
            .background(PlanTheme.brandGradient)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.plan_cost_total), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.95f))
        Spacer(modifier = Modifier.weight(1f))
        Text(CurrencyFormat.string(total), fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun HintCard(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        color = AppTheme.TextSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().planCard(),
    )
}

/** 費用一覧の 1 行（アイコン・名前・支払者・金額・削除）。タップで編集。 */
@Composable
private fun CostRow(item: PlanItem, plan: TravelPlan, onClick: () -> Unit, onDelete: () -> Unit) {
    val payerName = item.payerId?.let { plan.memberName(it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(item.category, size = 32, iconSize = 16)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (payerName != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = AppTheme.TextSecondary, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(payerName, fontSize = 11.sp, color = AppTheme.TextSecondary, maxLines = 1)
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            item.cost?.let { CurrencyFormat.string(it) } ?: "—",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (item.cost == null) AppTheme.TextSecondary else AppTheme.TextPrimary,
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AppTheme.TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            Icons.Filled.Delete,
            contentDescription = stringResource(R.string.common_delete),
            tint = AppTheme.TextSecondary,
            modifier = Modifier.size(20.dp).clickable(onClick = onDelete),
        )
    }
}

// MARK: - 精算

@Composable
private fun SettlementSection(plan: TravelPlan) {
    when {
        plan.members.isEmpty() -> HintCard(stringResource(R.string.plan_members_empty))
        !SettlementCalculator.hasSettlementData(plan) -> HintCard(stringResource(R.string.plan_settlement_empty))
        else -> {
            val (balances, transfers) = SettlementCalculator.settle(plan)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().planCard(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CardLabel(Icons.AutoMirrored.Filled.CompareArrows, stringResource(R.string.plan_settlement_balances))
                    balances.forEach { b ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(b.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextPrimary, modifier = Modifier.weight(1f))
                            Text(balanceText(b.balance), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = balanceColor(b.balance))
                        }
                    }
                }
                Column(modifier = Modifier.fillMaxWidth().planCard(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CardLabel(Icons.Filled.CurrencyExchange, stringResource(R.string.plan_settlement_transfers))
                    if (transfers.isEmpty()) {
                        Text(stringResource(R.string.plan_settlement_even), fontSize = 14.sp, color = AppTheme.TextSecondary)
                    } else {
                        transfers.forEach { t ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(t.fromName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = AppTheme.TextSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(t.toName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextPrimary, modifier = Modifier.weight(1f))
                                Text(CurrencyFormat.string(t.amount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
    }
}

/** 受け取り「+¥X」／支払い「−¥X」／ゼロは「¥0」。 */
private fun balanceText(balance: Int): String = when {
    balance > 0 -> "+" + CurrencyFormat.string(balance)
    balance < 0 -> "−" + CurrencyFormat.string(-balance)
    else -> CurrencyFormat.string(0)
}

private fun balanceColor(balance: Int): Color = when {
    balance > 0 -> Color(0xFF339959)
    balance < 0 -> PlanTheme.Accent
    else -> AppTheme.TextSecondary
}

// MARK: - 費用エディタ

/**
 * 費用専用項目の追加・編集画面。iOS 版 ExpenseItemEditor。
 * タイトル・金額・（メンバーがいれば）支払者・分担者。分担者が全員なら null（人数変動に追従）で保存する。
 */
@Composable
private fun ExpenseEditorScreen(
    plan: TravelPlan,
    editing: PlanItem?,
    onDismiss: () -> Unit,
    onSave: (title: String, cost: Int?, payerId: String?, split: List<String>?) -> Unit,
) {
    BackHandler(onBack = onDismiss)
    var title by remember { mutableStateOf(editing?.name ?: "") }
    var amountText by remember { mutableStateOf(editing?.cost?.toString() ?: "") }
    var payerId by remember { mutableStateOf(editing?.payerId) }
    var sharers by remember {
        mutableStateOf(if (editing != null) plan.effectiveSplitMemberIds(editing).toSet() else plan.members.map { it.id }.toSet())
    }
    val canSave = title.isNotBlank()
    val amount = amountText.toIntOrNull()

    Column(modifier = Modifier.fillMaxSize().background(PlanTheme.pageBackground)) {
        SheetTopBar(
            title = stringResource(if (editing == null) R.string.plan_expense_add else R.string.plan_expense_edit),
            leading = stringResource(R.string.common_cancel),
            onLeading = onDismiss,
        )
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth().planCard()) {
                Text(stringResource(R.string.plan_custom_title_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                PlainField(title, { title = it }, stringResource(R.string.plan_expense_title_placeholder))
            }
            Column(modifier = Modifier.fillMaxWidth().planCard()) {
                Text(stringResource(R.string.plan_section_cost), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(CurrencyFormat.symbol, fontSize = 20.sp, color = AppTheme.TextSecondary)
                    PlainField(
                        value = amountText,
                        onValueChange = { amountText = it.filter(Char::isDigit) },
                        placeholder = "0",
                        keyboardType = KeyboardType.Number,
                    )
                }
                if (amount != null && sharers.isNotEmpty()) {
                    Text(
                        stringResource(R.string.plan_split_perhead_format, CurrencyFormat.string(amount / sharers.size), sharers.size),
                        fontSize = 12.sp,
                        color = AppTheme.TextSecondary,
                    )
                }
            }
            if (plan.members.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().planCard(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.plan_split_payer_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                    WrapRow {
                        plan.members.forEach { m ->
                            val selected = payerId == m.id
                            Text(
                                m.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else PlanTheme.Primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (selected) PlanTheme.Primary else PlanTheme.Primary.copy(alpha = 0.12f))
                                    // 同じ人を再タップで未指定に戻す。
                                    .clickable { payerId = if (selected) null else m.id }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
                Column(modifier = Modifier.fillMaxWidth().planCard(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val allSelected = sharers.size == plan.members.size
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.plan_split_sharers_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary, modifier = Modifier.weight(1f))
                        Text(
                            stringResource(if (allSelected) R.string.plan_split_selectnone else R.string.plan_split_selectall),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlanTheme.Primary,
                            modifier = Modifier.clickable { sharers = if (allSelected) emptySet() else plan.members.map { it.id }.toSet() },
                        )
                    }
                    plan.members.forEach { m ->
                        val on = m.id in sharers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sharers = if (on) sharers - m.id else sharers + m.id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (on) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (on) PlanTheme.Primary else AppTheme.TextSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(m.name, fontSize = 15.sp, color = AppTheme.TextPrimary)
                        }
                    }
                }
            }
            PlanPrimaryButton(
                text = stringResource(R.string.common_save),
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    // 全員選択なら null（全員均等・人数変動に追従）、一部なら登場順の id 配列。
                    val allIds = plan.members.map { it.id }
                    val split = if (sharers == allIds.toSet()) null else allIds.filter { it in sharers }
                    onSave(title.trim(), amount, payerId, split)
                },
            )
        }
    }
}

@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onDone: (() -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontSize = 20.sp) },
        singleLine = true,
        textStyle = TextStyle(fontSize = 20.sp, color = AppTheme.TextPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = PlanTheme.Primary,
        ),
        modifier = modifier,
    )
}

// MARK: - メンバー編集

/** メンバーの追加・削除画面。iOS 版 MemberEditorSheet（追加後も入力欄は開いたままで連続追加）。 */
@Composable
private fun MemberEditorScreen(plan: TravelPlan, store: TravelPlanStore, onDone: () -> Unit) {
    BackHandler(onBack = onDone)
    var newName by remember { mutableStateOf("") }
    val canAdd = newName.isNotBlank()
    fun add() {
        if (!canAdd) return
        store.addMember(plan.id, newName)
        newName = ""
    }
    Column(modifier = Modifier.fillMaxSize().background(PlanTheme.pageBackground)) {
        SheetTopBar(
            title = stringResource(R.string.plan_members_label),
            leading = null,
            onLeading = {},
            trailing = stringResource(R.string.common_done),
            onTrailing = onDone,
        )
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (plan.members.isEmpty()) {
                Text(stringResource(R.string.plan_members_empty), fontSize = 14.sp, color = AppTheme.TextSecondary, modifier = Modifier.fillMaxWidth().planCard())
            } else {
                WrapRow(
                    modifier = Modifier.fillMaxWidth().planCard(),
                ) {
                    plan.members.forEach { m ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(PlanTheme.Primary.copy(alpha = 0.12f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(m.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary, maxLines = 1)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.Cancel,
                                contentDescription = stringResource(R.string.common_delete),
                                tint = PlanTheme.Primary,
                                modifier = Modifier.size(14.dp).clickable { store.removeMember(plan.id, m.id) },
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().planCard(padding = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = AppTheme.TextSecondary, modifier = Modifier.padding(start = 8.dp).size(20.dp))
                Box(modifier = Modifier.weight(1f)) {
                    PlainField(newName, { newName = it }, stringResource(R.string.plan_members_placeholder), onDone = { add() })
                }
                Icon(
                    Icons.Filled.AddCircle,
                    contentDescription = stringResource(R.string.common_add),
                    tint = if (canAdd) PlanTheme.Primary else AppTheme.TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(26.dp)
                        .alpha(if (canAdd) 1f else 0.6f)
                        .clickable(enabled = canAdd) { add() },
                )
            }
        }
    }
}
