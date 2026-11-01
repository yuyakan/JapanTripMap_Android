package com.example.japantripmap

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * プラン項目の種別。iOS 版 PlanItemCategory を移植（中核カテゴリのみ）。
 */
@Serializable
enum class PlanItemCategory(val labelRes: Int) {
    ATTRACTION(R.string.plan_category_attraction),
    GOURMET(R.string.plan_category_gourmet),
    ONSEN(R.string.plan_category_onsen),
    FESTIVAL(R.string.plan_category_festival),
    NATURE(R.string.plan_category_nature),
    SOUVENIR(R.string.plan_category_souvenir),
    HOTEL(R.string.plan_category_hotel),
    TRANSPORT(R.string.plan_category_transport),
    OTHER(R.string.plan_category_memo),
    /** 費用専用（場所・旅程を持たない出費項目。費用タブにのみ表示）。iOS 版 .expense。 */
    EXPENSE(R.string.plan_category_expense);

    /** Context から解決したラベル（非 Composable な場所用）。 */
    fun label(context: android.content.Context): String = context.getString(labelRes)

    /** ユーザーが手動で追加するカスタム項目か（元データ参照を持たない）。 */
    val isCustom: Boolean get() = this == HOTEL || this == TRANSPORT || this == OTHER || this == EXPENSE

    /** 費用専用か（旅程・地図・共有には出さず、費用タブにだけ出す）。 */
    val isExpenseOnly: Boolean get() = this == EXPENSE
}

/** 旅行メンバー。割り勘で支払者・分担者を安定して参照するため id を持つ。iOS 版 PlanMember。 */
@Serializable
data class PlanMember(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
)

/**
 * 旅行プランの進行状態。iOS 版 PlanStatus。
 * - UPCOMING: これから行く（既定）
 * - ONGOING: 旅行中（ホーム最上部の帯カードに優先して出す）
 * - COMPLETED: 旅行済み（その瞬間に県を訪問済みへ反映する）
 */
@Serializable
enum class PlanStatus(val labelRes: Int) {
    UPCOMING(R.string.plan_status_upcoming),
    ONGOING(R.string.plan_status_ongoing),
    COMPLETED(R.string.plan_status_completed),
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
    /** カスタム項目（宿泊/交通/メモ）の緯度。null なら位置なし。iOS 版 customLatitude 相当。 */
    val latitude: Double? = null,
    /** カスタム項目の経度。 */
    val longitude: Double? = null,
    /** 検索で選んだ施設名（表示・マップ起動ラベル用）。iOS 版 customPlaceName 相当。 */
    val placeName: String? = null,
    /** 逆ジオ/検索由来の住所（表示・コピー用）。iOS 版 customAddress 相当。 */
    val address: String? = null,
    /** 金額（円・整数）。null は未入力。費用タブで集計する（費用専用項目のみ）。 */
    val cost: Int? = null,
    /** 支払者（立て替えた人）のメンバー id。null は未指定。 */
    val payerId: String? = null,
    /** 分担者のメンバー id。null は「全メンバーで均等」（人数変動に追従）、空は割り勘対象外。 */
    val splitMemberIds: List<String>? = null,
    /** アプリ項目（観光・グルメ等）にユーザーが付けたメモ。iOS 版では customDetail に保存される値。 */
    val memo: String? = null,
) {
    /** 一覧に 1 行で出すユーザーのメモ（カスタム項目は detail、アプリ項目は memo）。 */
    val userMemo: String get() = (if (category.isCustom) detail else memo.orEmpty()).trim()

    /** カスタム項目に位置が設定されているか。 */
    val hasCoordinate: Boolean get() = latitude != null && longitude != null
}

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
    /** 旅行メンバー（登場順）。割り勘で項目の支払者・分担者を id 参照する。 */
    val members: List<PlanMember> = emptyList(),
    /** 進行状態（計画中 / 進行中 / 旅行済み）。 */
    val status: PlanStatus = PlanStatus.UPCOMING,
    /** 手動で「行き先候補」として登録した県（Prefecture の enum 名）。項目由来の県とは独立。 */
    val plannedPrefectures: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /** 旅程・地図・共有が対象にする項目（費用専用項目を除く）。 */
    val itineraryItems: List<PlanItem> get() = items.filterNot { it.category.isExpenseOnly }

    /** 費用一覧・合計・精算の対象（費用専用項目のみ）。 */
    val costItems: List<PlanItem> get() = items.filter { it.category.isExpenseOnly }

    /** 手動登録した県（未知の名前は除く）。 */
    val plannedPrefectureList: List<Prefecture>
        get() = plannedPrefectures.mapNotNull { n -> Prefecture.entries.firstOrNull { it.name == n } }

    /** 旅程項目から導かれる県（重複なし・登場順）。 */
    val itemPrefectures: List<Prefecture>
        get() = itineraryItems.mapNotNull { prefectureFromDisplayName(it.prefectureName) }.distinct()

    /**
     * プランに関わる県（重複なし）。手動登録した県を先に、続けて項目由来の県。
     * 訪問済み反映・ホームの帯カード写真・県タイルで使う。iOS 版 TravelPlan.prefectures。
     */
    val prefectures: List<Prefecture> get() = (plannedPrefectureList + itemPrefectures).distinct()

    /** 費用合計（未入力は 0 扱い）。 */
    val totalCost: Int get() = costItems.sumOf { it.cost ?: 0 }

    fun memberName(id: String): String? = members.firstOrNull { it.id == id }?.name

    /** 項目の実効分担者 id（null=全員、それ以外は現存メンバーだけ）。 */
    fun effectiveSplitMemberIds(item: PlanItem): List<String> {
        val all = members.map { it.id }
        val ids = item.splitMemberIds ?: return all
        return ids.filter { it in all }
    }

    /**
     * 日程モード用に、Day ごと（＋末尾に未割当）でグループ化して返す。
     * 各 day は 1..dayCount、未割当は day = null。項目が無い日も表示できるよう全 Day を含む。
     */
    val itemsByDay: List<Pair<Int?, List<PlanItem>>>
        get() {
            val result = mutableListOf<Pair<Int?, List<PlanItem>>>()
            val source = itineraryItems
            for (d in 1..dayCount) {
                result.add(d to source.filter { it.dayNumber == d })
            }
            val unassigned = source.filter { it.dayNumber == null || (it.dayNumber ?: 0) !in 1..dayCount }
            if (unassigned.isNotEmpty()) result.add(null to unassigned)
            return result
        }

    /**
     * 指定日を「時間ブロックごと（時刻順）＋ブロック未割当（末尾）」に分けて返す。
     * block が null のセクションはブロック未割当項目。ブロックはあるが項目 0 のものも表示のため含める。
     */
    fun blockSections(day: Int): List<Pair<TimeBlock?, List<PlanItem>>> {
        val dayItems = itineraryItems.filter { it.dayNumber == day }
        val blocks = timeBlocks.filter { it.dayNumber == day }.sortedWith(compareBy({ it.sortKey }, { it.id }))
        val result = mutableListOf<Pair<TimeBlock?, List<PlanItem>>>()
        for (block in blocks) {
            result.add(block to dayItems.filter { it.timeBlockId == block.id })
        }
        val noBlock = dayItems.filter { it.timeBlockId == null || blocks.none { b -> b.id == it.timeBlockId } }
        if (noBlock.isNotEmpty() || blocks.isEmpty()) result.add(null to noBlock)
        return result
    }

    /**
     * 指定日の項目を「訪問順」に一列で返す（iOS 版 orderedItems(forDay:) 相当）。
     * 時間ブロックを開始時刻順に並べ、各ブロック内は手動並び、時刻なし・未割当は末尾。
     * 詳細リスト（blockSections）と同じ順序なので、地図の経路もリスト表示と一致する。
     */
    fun orderedItems(day: Int): List<PlanItem> = blockSections(day).flatMap { it.second }
}
