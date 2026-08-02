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
        val plan = TravelPlan(title = title.ifBlank { "無題のプラン" }, memo = memo)
        plans = plans + plan
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

    /** 指定プランに項目を追加する。 */
    fun addItem(planId: String, item: PlanItem) {
        plans = plans.map { plan ->
            if (plan.id == planId) {
                plan.copy(items = plan.items + item, updatedAt = System.currentTimeMillis())
            } else {
                plan
            }
        }
        persist()
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

    /** プランのタイトル・メモを更新する。 */
    fun updatePlan(planId: String, title: String, memo: String) {
        plans = plans.map { plan ->
            if (plan.id == planId) {
                plan.copy(
                    title = title.ifBlank { "無題のプラン" },
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

    /** 項目を時間ブロックに割り当てる（blockId = null でブロック未割当に戻す）。 */
    fun assignItemToBlock(planId: String, itemId: String, blockId: String?) {
        updatePlanBy(planId) { plan ->
            plan.copy(
                items = plan.items.map { if (it.id == itemId) it.copy(timeBlockId = blockId) else it },
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
