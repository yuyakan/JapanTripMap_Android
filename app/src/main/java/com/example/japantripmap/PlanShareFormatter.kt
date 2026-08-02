package com.example.japantripmap

/**
 * 旅行プランを共有用のプレーンテキストに整形する。
 * iOS 版 PlanShareFormatter.swift を移植（フラット版）。
 * Android の共有シート（ACTION_SEND）に渡す。
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

    fun text(plan: TravelPlan): String {
        val lines = mutableListOf<String>()
        lines.add(plan.title.ifBlank { "無題のプラン" })
        if (plan.memo.isNotBlank()) {
            lines.add("")
            lines.add(plan.memo)
        }
        if (plan.groupingMode == PlanGroupingMode.DAY) {
            // 日程モード：Day 見出しで区切る（項目のある日／未割当のみ）。
            for ((day, dayItems) in plan.itemsByDay) {
                if (dayItems.isEmpty()) continue
                lines.add("")
                lines.add("──── " + (day?.let { "Day $it" } ?: "未割り当て") + " ────")
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
            var line = "・${emoji(item.category)}${item.name}"
            if (item.prefectureName.isNotBlank()) line += "（${item.prefectureName}）"
            lines.add(line)
            if (item.detail.isNotBlank()) lines.add("    ${item.detail}")
        }
    }
}
