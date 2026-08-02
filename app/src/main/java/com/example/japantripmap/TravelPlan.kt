package com.example.japantripmap

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * プラン項目の種別。iOS 版 PlanItemCategory を移植（中核カテゴリのみ）。
 */
@Serializable
enum class PlanItemCategory(val label: String) {
    ATTRACTION("観光スポット"),
    GOURMET("グルメ"),
    ONSEN("温泉"),
    FESTIVAL("祭り"),
    NATURE("自然"),
    SOUVENIR("お土産"),
    HOTEL("宿泊"),
    TRANSPORT("交通"),
    OTHER("メモ"),
}

/** プラン詳細の区切り方。iOS 版 PlanGroupingMode を移植。 */
@Serializable
enum class PlanGroupingMode {
    /** 日程分けなし（フラットな 1 リスト）。 */
    FLAT,
    /** 日程（Day1/Day2…）ごとに分ける。 */
    DAY,
}

/**
 * 日程モードで、1 日の中を時刻で区切るブロック。iOS 版 TimeBlock を移植。
 * 任意の開始時刻（時・分）と見出しを持ち、この中に複数の PlanItem を入れる。
 */
@Serializable
data class TimeBlock(
    val id: String = UUID.randomUUID().toString(),
    /** このブロックが属する日（1 始まり）。 */
    val dayNumber: Int,
    /** 開始「時」。null は時刻未設定ブロック。 */
    val startHour: Int? = null,
    /** 開始「分」。 */
    val startMinute: Int? = null,
    /** 任意の見出し（"午前" 等）。空でも可。 */
    val title: String = "",
) {
    /** "09:05" 形式の時刻ラベル。時刻未設定なら null。 */
    val timeLabel: String?
        get() = startHour?.let { h -> "%02d:%02d".format(h, startMinute ?: 0) }

    /** ソート用の分換算（時刻なしは末尾へ回すため大きな値）。 */
    val sortKey: Int
        get() = startHour?.let { it * 60 + (startMinute ?: 0) } ?: Int.MAX_VALUE
}

/**
 * 1 つの旅行プランに含まれる 1 項目。
 * iOS 版 PlanItem を移植（日程割当 dayNumber・時間ブロック割当 timeBlockId に対応）。
 */
@Serializable
data class PlanItem(
    val id: String = UUID.randomUUID().toString(),
    val category: PlanItemCategory,
    /** 都道府県名（表示用）。カスタム項目では空文字可。 */
    val prefectureName: String = "",
    val name: String,
    /** 説明（元データ由来 or ユーザー入力メモ）。 */
    val detail: String = "",
    /** 日程モードで「何日目か」。null は未割当。 */
    val dayNumber: Int? = null,
    /** 所属する時間ブロックの id。null はブロック未割当。 */
    val timeBlockId: String? = null,
)

/**
 * 1 つの旅行プラン（旅のしおり）。順序を持つ PlanItem のリストを保持する。
 * iOS 版 TravelPlan を移植。日程分け（groupingMode / dayCount）に対応。
 */
@Serializable
data class TravelPlan(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val memo: String = "",
    val items: List<PlanItem> = emptyList(),
    val groupingMode: PlanGroupingMode = PlanGroupingMode.FLAT,
    /** 日程モードでの日数（最低 1）。 */
    val dayCount: Int = 1,
    /** 日程内の時間ブロック定義（日程モードで使用）。 */
    val timeBlocks: List<TimeBlock> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /**
     * 日程モード用に、Day ごと（＋末尾に未割当）でグループ化して返す。
     * 各 day は 1..dayCount、未割当は day = null。項目が無い日も表示できるよう全 Day を含む。
     */
    val itemsByDay: List<Pair<Int?, List<PlanItem>>>
        get() {
            val result = mutableListOf<Pair<Int?, List<PlanItem>>>()
            for (d in 1..dayCount) {
                result.add(d to items.filter { it.dayNumber == d })
            }
            val unassigned = items.filter { it.dayNumber == null || (it.dayNumber ?: 0) !in 1..dayCount }
            if (unassigned.isNotEmpty()) result.add(null to unassigned)
            return result
        }

    /**
     * 指定日を「時間ブロックごと（時刻順）＋ブロック未割当（末尾）」に分けて返す。
     * block が null のセクションはブロック未割当項目。ブロックはあるが項目 0 のものも表示のため含める。
     */
    fun blockSections(day: Int): List<Pair<TimeBlock?, List<PlanItem>>> {
        val dayItems = items.filter { it.dayNumber == day }
        val blocks = timeBlocks.filter { it.dayNumber == day }.sortedBy { it.sortKey }
        val result = mutableListOf<Pair<TimeBlock?, List<PlanItem>>>()
        for (block in blocks) {
            result.add(block to dayItems.filter { it.timeBlockId == block.id })
        }
        val noBlock = dayItems.filter { it.timeBlockId == null || blocks.none { b -> b.id == it.timeBlockId } }
        if (noBlock.isNotEmpty() || blocks.isEmpty()) result.add(null to noBlock)
        return result
    }
}
