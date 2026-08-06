package com.example.japantripmap

import android.content.Context

/**
 * 旅行プランを共有用のプレーンテキストに整形する。
 * iOS 版 PlanShareFormatter.swift を移植（フラット版）。
 * Android の共有シート（ACTION_SEND）に渡す。スポット名・県名はロケールに応じて英訳する。
 */
object PlanShareFormatter {

    /** カテゴリごとの絵文字（共有テキストで見分けやすく）。 */
    private fun emoji(category: PlanItemCategory): String = when (category) {
        PlanItemCategory.ATTRACTION -> "📍"
        PlanItemCategory.GOURMET -> "🍴"
        PlanItemCategory.ONSEN -> "♨️"
        PlanItemCategory.FESTIVAL -> "🎆"
        PlanItemCategory.NATURE -> "🌿"
        PlanItemCategory.SOUVENIR -> "🎁"
        PlanItemCategory.HOTEL -> "🏨"
        PlanItemCategory.TRANSPORT -> "🚉"
        PlanItemCategory.OTHER -> "📝"
    }

    fun text(context: Context, plan: TravelPlan): String {
        val lines = mutableListOf<String>()
        lines.add(plan.title.ifBlank { context.getString(R.string.plan_untitled) })
        if (plan.memo.isNotBlank()) {
            lines.add("")
            lines.add(plan.memo)
        }
        if (plan.groupingMode == PlanGroupingMode.DAY) {
            // 日程モード：Day 見出しで区切る（項目のある日／未割当のみ）。
            for ((day, dayItems) in plan.itemsByDay) {
                if (dayItems.isEmpty()) continue
                lines.add("")
                val heading = day?.let { "Day $it" } ?: context.getString(R.string.plan_unassigned)
                lines.add("──── $heading ────")
                appendItems(dayItems, lines)
            }
        } else if (plan.items.isNotEmpty()) {
            lines.add("")
            appendItems(plan.items, lines)
        }
        return lines.joinToString("\n")
    }

    private fun appendItems(items: List<PlanItem>, lines: MutableList<String>) {
        for (item in items) {
            var line = "・${emoji(item.category)}${localizeData(item.name)}"
            if (item.prefectureName.isNotBlank()) line += "（${localizeData(item.prefectureName)}）"
            lines.add(line)
            if (item.detail.isNotBlank()) lines.add("    ${localizeData(item.detail)}")
        }
    }
}
