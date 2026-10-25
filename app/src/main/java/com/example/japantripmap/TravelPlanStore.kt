package com.example.japantripmap

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 旅行プランの保存・読み込みを担う ViewModel。
 * iOS 版 TravelPlanStore を移植。永続化は SharedPreferences に JSON で行う。
 */
class TravelPlanStore(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("travel_plans", 0)
    private val json = Json { ignoreUnknownKeys = true }

    var plans by mutableStateOf<List<TravelPlan>>(emptyList())
        private set

    init {
        load()
    }

    private fun load() {
        val raw = prefs.getString(KEY, null) ?: return
        plans = runCatching { json.decodeFromString<List<TravelPlan>>(raw) }.getOrDefault(emptyList())
    }

    private fun persist() {
        prefs.edit().putString(KEY, json.encodeToString<List<TravelPlan>>(plans)).apply()
    }

    /** 新しいプランを作成して返す。 */
    fun createPlan(title: String, memo: String = ""): TravelPlan {
        val plan = TravelPlan(title = title.ifBlank { getApplication<Application>().getString(R.string.plan_untitled) }, memo = memo)
        // iOS 版と同じく新しいプランは一覧の先頭に入れる。
        plans = listOf(plan) + plans
        persist()
        return plan
    }

    fun deletePlan(planId: String) {
        plans = plans.filterNot { it.id == planId }
        persist()
    }

    /** プランの並び順を入れ替える（一覧の並び替え用）。iOS 版 movePlans 相当。 */
    fun movePlan(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val list = plans.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val moved = list.removeAt(fromIndex)
        list.add(toIndex, moved)
        plans = list
        persist()
    }

    /**
     * 指定プランに項目を追加する。
     * アプリ項目は同一プラン内の重複（同名・同種別）を追加しない。カスタム項目は同名でも常に追加する。
     */
    fun addItem(planId: String, item: PlanItem) {
        plans = plans.map { plan ->
            if (plan.id == planId) {
                val duplicate = !item.category.isCustom &&
                    plan.items.any { it.name == item.name && it.category == item.category }
                if (duplicate) plan else plan.copy(items = plan.items + item, updatedAt = System.currentTimeMillis())
            } else {
                plan
            }
        }
        persist()
    }

    /** 指定した項目（名前＋種別）がプランに含まれているか。 */
    fun contains(planId: String, name: String, category: PlanItemCategory): Boolean =
        plan(planId)?.items?.any { it.name == name && it.category == category } == true

    // ---- 進行状態 ----

    /**
     * 進行状態を設定する。旅行済みにした瞬間、そのプランの県を訪問済みへ追加する（後勝ち）。
     * 旅行済みから外しても訪問済みからは外さない（iOS 版 setStatus と同じ）。
     */
    fun setStatus(planId: String, status: PlanStatus) {
        val target = plan(planId) ?: return
        if (target.status == status) return
        updatePlanBy(planId) { it.copy(status = status, updatedAt = System.currentTimeMillis()) }
        if (status == PlanStatus.COMPLETED) {
            VisitedPrefectureStore.markVisited(target.prefectures)
        }
    }

    /** ホーム最上部の帯に出すプラン（進行中を優先し、無ければ計画中。どちらも最新更新のもの）。 */
    val featuredPlan: TravelPlan?
        get() = plans.filter { it.status == PlanStatus.ONGOING }.maxByOrNull { it.updatedAt }
            ?: plans.filter { it.status == PlanStatus.UPCOMING }.maxByOrNull { it.updatedAt }

    /** 指定県を含むプラン（訪問済みマップの県シートで使う）。 */
    fun plansContaining(prefecture: Prefecture): List<TravelPlan> =
        plans.filter { prefecture in it.prefectures }

    // ---- 行き先候補の県 ----

    /** 「行き先候補」の県をまとめて追加する（既にある県は無視・選択順を保つ）。 */
    fun addPrefectures(planId: String, prefectures: List<Prefecture>) {
        val target = plan(planId) ?: return
        val added = prefectures.map { it.name }.filterNot { it in target.plannedPrefectures }.distinct()
        if (added.isEmpty()) return
        updatePlanBy(planId) {
            it.copy(plannedPrefectures = it.plannedPrefectures + added, updatedAt = System.currentTimeMillis())
        }
    }

    /** 「行き先候補」の県を外す（項目由来の県には影響しない）。 */
    fun removePrefecture(planId: String, prefecture: Prefecture) {
        updatePlanBy(planId) {
            it.copy(plannedPrefectures = it.plannedPrefectures - prefecture.name, updatedAt = System.currentTimeMillis())
        }
    }

    // ---- メンバー・費用 ----

    /** メンバーを追加する（前後の空白を除去。空文字・同名は追加しない）。 */
    fun addMember(planId: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val target = plan(planId) ?: return
        if (target.members.any { it.name == trimmed }) return
        updatePlanBy(planId) {
            it.copy(members = it.members + PlanMember(name = trimmed), updatedAt = System.currentTimeMillis())
        }
    }

    /** メンバーを削除する。支払者に指定していた項目は支払者を未指定に戻す。 */
    fun removeMember(planId: String, memberId: String) {
        updatePlanBy(planId) { plan ->
            plan.copy(
                members = plan.members.filterNot { it.id == memberId },
                items = plan.items.map { if (it.payerId == memberId) it.copy(payerId = null) else it },
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    /** 費用専用項目を追加する（タイトルが空なら何もしない）。 */
    fun addExpenseItem(planId: String, title: String, cost: Int?, payerId: String?, splitMemberIds: List<String>?) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val item = PlanItem(
            category = PlanItemCategory.EXPENSE,
            name = trimmed,
            cost = cost,
            payerId = payerId,
            splitMemberIds = splitMemberIds,
        )
        updatePlanBy(planId) { it.copy(items = it.items + item, updatedAt = System.currentTimeMillis()) }
    }

    fun removeItem(planId: String, itemId: String) {
        plans = plans.map { plan ->
            if (plan.id == planId) {
                plan.copy(items = plan.items.filterNot { it.id == itemId }, updatedAt = System.currentTimeMillis())
            } else {
                plan
            }
        }
        persist()
    }

    /** 既存項目を id 一致で丸ごと置き換える（カスタム項目の編集用）。iOS 版 updateItem 相当。 */
    fun updateItem(planId: String, item: PlanItem) {
        updatePlanBy(planId) { plan ->
            plan.copy(
                items = plan.items.map { if (it.id == item.id) item else it },
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    /**
     * 項目を items 配列内で targetId の直前へ移動する（ドラッグ&ドロップの並べ替え用）。
     * targetId が null / 見つからない場合は末尾へ移動する。日/ブロック跨ぎの移動時は
     * 先に assignItemToDay / assignItemToBlock で割当を変えてから呼ぶ。
     */
    fun moveItemBefore(planId: String, itemId: String, targetId: String?) {
        if (itemId == targetId) return
        updatePlanBy(planId) { plan ->
            val moving = plan.items.firstOrNull { it.id == itemId } ?: return@updatePlanBy plan
            val rest = plan.items.filterNot { it.id == itemId }
            val insertAt = targetId
                ?.let { t -> rest.indexOfFirst { it.id == t } }
                ?.takeIf { it >= 0 }
                ?: rest.size
            val reordered = rest.toMutableList().apply { add(insertAt, moving) }
            plan.copy(items = reordered, updatedAt = System.currentTimeMillis())
        }
    }

    /** タイトルだけを更新する（空なら保存しない）。 */
    fun updateTitle(planId: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        updatePlanBy(planId) { it.copy(title = trimmed, updatedAt = System.currentTimeMillis()) }
    }

    /** メモだけを更新する。 */
    fun updateMemo(planId: String, memo: String) {
        updatePlanBy(planId) { it.copy(memo = memo, updatedAt = System.currentTimeMillis()) }
    }

    /** プランのタイトル・メモを更新する。 */
    fun updatePlan(planId: String, title: String, memo: String) {
        plans = plans.map { plan ->
            if (plan.id == planId) {
                plan.copy(
                    title = title.ifBlank { getApplication<Application>().getString(R.string.plan_untitled) },
                    memo = memo,
                    updatedAt = System.currentTimeMillis(),
                )
            } else {
                plan
            }
        }
        persist()
    }

    /** 日程分けモードを切り替える。 */
    fun setGroupingMode(planId: String, mode: PlanGroupingMode) {
        updatePlanBy(planId) { it.copy(groupingMode = mode, updatedAt = System.currentTimeMillis()) }
    }

    /** 日数を変更する（1..30）。減らした場合、その日を超える割当・時間ブロックは削除する。 */
    fun setDayCount(planId: String, count: Int) {
        val c = count.coerceIn(1, 30)
        updatePlanBy(planId) { plan ->
            val removedBlockIds = plan.timeBlocks.filter { it.dayNumber > c }.map { it.id }.toSet()
            val fixedItems = plan.items.map {
                var item = it
                if ((item.dayNumber ?: 0) > c) item = item.copy(dayNumber = null, timeBlockId = null)
                if (item.timeBlockId in removedBlockIds) item = item.copy(timeBlockId = null)
                item
            }
            plan.copy(
                dayCount = c,
                items = fixedItems,
                timeBlocks = plan.timeBlocks.filter { it.dayNumber <= c },
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    /** 項目を指定の日に割り当てる（day = null で未割当に戻す）。日が変わるとブロック割当も解除。 */
    fun assignItemToDay(planId: String, itemId: String, day: Int?) {
        updatePlanBy(planId) { plan ->
            plan.copy(
                items = plan.items.map {
                    if (it.id == itemId) it.copy(dayNumber = day, timeBlockId = null) else it
                },
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    // ---- 時間ブロック ----

    /** 指定日に時間ブロックを追加し、その id を返す。 */
    fun addTimeBlock(planId: String, day: Int, startHour: Int?, startMinute: Int?, title: String = ""): String {
        val block = TimeBlock(dayNumber = day, startHour = startHour, startMinute = startMinute, title = title)
        updatePlanBy(planId) { it.copy(timeBlocks = it.timeBlocks + block, updatedAt = System.currentTimeMillis()) }
        return block.id
    }

    /** 時間ブロックの時刻・見出しを更新する。 */
    fun updateTimeBlock(planId: String, block: TimeBlock) {
        updatePlanBy(planId) { plan ->
            plan.copy(
                timeBlocks = plan.timeBlocks.map { if (it.id == block.id) block else it },
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    /** 時間ブロックを削除する。所属していた項目はブロック未割当に戻す。 */
    fun removeTimeBlock(planId: String, blockId: String) {
        updatePlanBy(planId) { plan ->
            plan.copy(
                timeBlocks = plan.timeBlocks.filterNot { it.id == blockId },
                items = plan.items.map { if (it.timeBlockId == blockId) it.copy(timeBlockId = null) else it },
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    /** 項目を時間ブロックに割り当てる（blockId = null でブロック未割当に戻す）。割当先ブロックの日に揃える。 */
    fun assignItemToBlock(planId: String, itemId: String, blockId: String?) {
        updatePlanBy(planId) { plan ->
            val blockDay = blockId?.let { id -> plan.timeBlocks.firstOrNull { it.id == id }?.dayNumber }
            plan.copy(
                items = plan.items.map {
                    if (it.id == itemId) it.copy(timeBlockId = blockId, dayNumber = blockDay ?: it.dayNumber) else it
                },
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    private inline fun updatePlanBy(planId: String, transform: (TravelPlan) -> TravelPlan) {
        plans = plans.map { if (it.id == planId) transform(it) else it }
        persist()
    }

    fun plan(planId: String): TravelPlan? = plans.firstOrNull { it.id == planId }

    companion object {
        private const val KEY = "plans_json"
    }
}
