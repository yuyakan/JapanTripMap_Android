package com.example.japantripmap

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

// MARK: - テーマ（切り口別の都道府県紹介）

/** テーマの中身。県を紹介するか、「もの」（ビーチ）を紹介するか。 */
sealed interface ThemeContent {
    data class Prefectures(val list: List<Prefecture>) : ThemeContent
    data class Items(val list: List<ThemeItem>) : ThemeContent
}

/**
 * テーマが紹介する「もの」（ビーチ）。タップでその県の詳細を開き、続けて自然スポット詳細まで開く。
 * @param name 日本語名（自然データの名前と一致。表示時に localizeData する）。
 */
data class ThemeItem(val name: String, val prefecture: Prefecture) {
    val photoResName: String? get() = BEACH_PHOTOS[name]
}

/**
 * ホームの「テーマで探す」。iOS 版 PrefectureTheme を移植。
 * 固定指標（三景・三名泉・三名園・三大夜景）は県を、海はビーチ（もの）を紹介する。
 */
enum class PrefectureTheme(@param:StringRes val titleRes: Int) {
    JAPAN_THREE_VIEWS(R.string.home_theme_japan_three_views),
    THREE_FAMOUS_ONSEN(R.string.home_theme_three_famous_onsen),
    THREE_GARDENS(R.string.home_theme_three_gardens),
    THREE_NIGHT_VIEWS(R.string.home_theme_three_night_views),
    SEA_AND_BEACH(R.string.home_theme_sea_and_beach);

    val content: ThemeContent
        get() = when (this) {
            // 日本三景（林春斎）: 松島=宮城 / 天橋立=京都 / 宮島=広島
            JAPAN_THREE_VIEWS -> ThemeContent.Prefectures(listOf(Prefecture.MIYAGI, Prefecture.KYOTO, Prefecture.HIROSHIMA))
            // 日本三名泉（林羅山）: 草津=群馬 / 下呂=岐阜 / 有馬=兵庫
            THREE_FAMOUS_ONSEN -> ThemeContent.Prefectures(listOf(Prefecture.GUNMA, Prefecture.GIFU, Prefecture.HYOGO))
            // 日本三名園: 兼六園=石川 / 後楽園=岡山 / 偕楽園=茨城
            THREE_GARDENS -> ThemeContent.Prefectures(listOf(Prefecture.ISHIKAWA, Prefecture.OKAYAMA, Prefecture.IBARAKI))
            // 日本三大夜景: 函館山=北海道 / 摩耶山=兵庫 / 稲佐山=長崎
            THREE_NIGHT_VIEWS -> ThemeContent.Prefectures(listOf(Prefecture.HOKKAIDO, Prefecture.HYOGO, Prefecture.NAGASAKI))
            // 有名なビーチ（北→南の地理順）。
            SEA_AND_BEACH -> ThemeContent.Items(POPULAR_BEACHES)
        }

    /** このテーマ×県でカードに使う「まさにそのスポット」（観光スポット名）。無ければ null。 */
    fun heroSpotName(prefecture: Prefecture): String? = when (this) {
        JAPAN_THREE_VIEWS -> mapOf(Prefecture.MIYAGI to "松島", Prefecture.KYOTO to "天橋立", Prefecture.HIROSHIMA to "厳島神社")[prefecture]
        THREE_FAMOUS_ONSEN -> mapOf(Prefecture.GUNMA to "草津温泉", Prefecture.GIFU to "下呂温泉", Prefecture.HYOGO to "有馬温泉")[prefecture]
        THREE_GARDENS -> mapOf(Prefecture.ISHIKAWA to "兼六園", Prefecture.OKAYAMA to "後楽園", Prefecture.IBARAKI to "偕楽園")[prefecture]
        THREE_NIGHT_VIEWS -> mapOf(Prefecture.HOKKAIDO to "函館山", Prefecture.HYOGO to "摩耶山", Prefecture.NAGASAKI to "稲佐山")[prefecture]
        SEA_AND_BEACH -> null
    }

    companion object {
        /** ホーム「おすすめ」の定番県（固定・表示順）。 */
        val popularPrefectures = listOf(
            Prefecture.OKINAWA, Prefecture.FUKUOKA, Prefecture.KYOTO, Prefecture.HOKKAIDO, Prefecture.OSAKA,
            Prefecture.HIROSHIMA, Prefecture.HYOGO, Prefecture.AICHI, Prefecture.MIYAGI, Prefecture.KANAGAWA,
        )

        /**
         * 有名なビーチ。高浜（五島）・水晶浜は写真が無いため iOS 同様ホームの一覧からは外している
         * （自然タブには海スポットとして残る）。
         */
        private val POPULAR_BEACHES = listOf(
            ThemeItem("浄土ヶ浜", Prefecture.IWATE),
            ThemeItem("九十九里浜", Prefecture.CHIBA),
            ThemeItem("由比ヶ浜", Prefecture.KANAGAWA),
            ThemeItem("伊豆白浜", Prefecture.SHIZUOKA),
            ThemeItem("竹野浜", Prefecture.HYOGO),
            ThemeItem("白良浜", Prefecture.WAKAYAMA),
            ThemeItem("エメラルドビーチ", Prefecture.OKINAWA),
            ThemeItem("ニシ浜（波照間）", Prefecture.OKINAWA),
        )
    }
}

/** 県の代表写真（写真つき観光スポットの先頭＝県詳細カルーセルの先頭と同じ）。 */
fun Prefecture.heroPhotoResName(): String? =
    tourismInfo?.attractions?.firstNotNullOfOrNull { ATTRACTION_PHOTOS[it.name] }

/** 県カード列の絞り込み（最近見た / お気に入り / おすすめ）。 */
private enum class PrefectureFilter(@param:StringRes val titleRes: Int) {
    RECENT(R.string.home_tab_recent),
    FAVORITE(R.string.home_tab_favorite),
    RECOMMENDED(R.string.home_tab_recommended),
}

/** ホームに出すトレンドのカテゴリと並び順（カフェは県詳細だけに残す）。 */
private val HOME_TREND_CATEGORIES = listOf("spot", "gourmet")

private const val CARD_RADIUS = 18
private const val PREF_CARD_WIDTH = 230
private const val PREF_CARD_HEIGHT = 190

/**
 * ホームタブ。iOS 版 HomeView を移植。ユーザーの行動（最近見た県 / お気に入り）を軸にした玄関。
 *
 * 上から: 次の旅（帯カード）→ ルーレット導線 → 県カード列（最近見た/お気に入り/おすすめ）＋主役の県の動画
 * → テーマ（ピル＋カード）→ YouTube 出典。
 * 要件C: 動画は必ず「県」に紐づけて出し、トレンド単独の一覧は作らない。この画面には広告を置かない。
 *
 * @param onOpenPrefecture 県詳細を開く。spot が非 null なら県詳細の上にその詳細まで続けて開く（テーマのカード）。
 * @param onOpenPlan プラン詳細を開く（ホームの上に重ねる）。
 */
@Composable
fun HomeScreen(
    store: TravelPlanStore,
    onOpenPrefecture: (Prefecture, SpotDetail?) -> Unit,
    onOpenPlan: (String) -> Unit,
    /** Shorts フィードを開く（下タブも覆う全画面。iOS の fullScreenCover）。 */
    onOpenShorts: (ShortsFeedRequest) -> Unit,
) {
    LaunchedEffect(Unit) { TrendRepository.loadIfNeeded() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        // ⓪ 次の旅（進行中 > 計画中）。無ければプレースホルダー。
        val featured = store.featuredPlan
        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
            if (featured != null) {
                OngoingPlanBanner(featured) { onOpenPlan(featured.id) }
            } else {
                OngoingPlanEmpty { AppRouter.openMyPlans(newPlan = store.plans.isEmpty()) }
            }
        }

        // ① ルーレット導線。
        RouletteEntryRow(
            onClick = { AppRouter.openRouletteMap() },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        // ② 県カード（最近見た／お気に入り／おすすめ）＋主役の県の動画。
        PrefectureSection(
            onOpenPrefecture = { onOpenPrefecture(it, null) },
            onOpenShorts = { videos, index -> onOpenShorts(ShortsFeedRequest(videos, index)) },
        )

        // ③ テーマ（ピル選択 → 下に展開）。
        ThemeSection(onOpenPrefecture = onOpenPrefecture)

        // フッター（要件D: 出典）。
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DevelopedWithYouTubeBadge(height = 22)
            Text(stringResource(R.string.home_trend_source_note), fontSize = 11.sp, color = AppTheme.TextSecondary)
        }
    }
}

// MARK: - ⓪ 次の旅

@Composable
private fun OngoingPlanBanner(plan: TravelPlan, onClick: () -> Unit) {
    val context = LocalContext.current
    val heroRes = plan.prefectures.firstNotNullOfOrNull { it.heroPhotoResName() }
    val heroId = heroRes?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0
    val ongoing = plan.status == PlanStatus.ONGOING
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .shadow(6.dp, RoundedCornerShape(PlanTheme.cardCorner), spotColor = PlanTheme.cardShadow)
            .clip(RoundedCornerShape(PlanTheme.cardCorner))
            .clickable(onClick = onClick),
    ) {
        if (heroId != 0) {
            Image(painterResource(heroId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize().background(PlanTheme.brandGradient))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Black.copy(alpha = 0.15f)))),
        )
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (ongoing) Icons.Filled.Flight else Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        stringResource(if (ongoing) R.string.home_section_ongoing else R.string.home_section_upcoming),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.95f),
                    )
                }
                Text(plan.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White.copy(alpha = 0.9f))
        }
    }
}

@Composable
private fun OngoingPlanEmpty(onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PlanTheme.cardCorner))
            .background(PlanTheme.groupedSurface)
            .border(1.dp, PlanTheme.Primary.copy(alpha = 0.12f), RoundedCornerShape(PlanTheme.cardCorner))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Flight, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(stringResource(R.string.home_ongoing_empty_title), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppTheme.TextPrimary)
                Text(stringResource(R.string.home_ongoing_empty_subtitle), fontSize = 12.sp, color = AppTheme.TextSecondary)
            }
        }
        Text(
            stringResource(R.string.home_ongoing_empty_action),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(PlanTheme.Primary)
                .clickable(onClick = onAction)
                .padding(vertical = 11.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

// MARK: - ① ルーレット導線

/**
 * ルーレット導線（1 行の帯）。iOS 版 RouletteEntryRow。
 * 表示のたびに盤が 1 回だけ回って止まり、ルーレット機能だと分かる形にする。
 */
@Composable
private fun RouletteEntryRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val angle = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val step = 360f / 8
        val landing = Random.nextInt(8) * step + step / 2
        angle.snapTo(0f)
        angle.animateTo(720f + landing, tween(durationMillis = 2000, easing = FastOutSlowInEasing))
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = PlanTheme.Primary.copy(alpha = 0.28f))
            .clip(RoundedCornerShape(14.dp))
            .background(PlanTheme.brandGradient)
            .background(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(40f, 40f),
                    radius = 220f,
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.TopCenter) {
            Canvas(modifier = Modifier.size(24.dp).rotate(angle.value)) {
                val sweep = 360f / 8
                for (i in 0 until 8) {
                    drawArc(
                        color = Color.White.copy(alpha = if (i % 2 == 0) 0.28f else 0.08f),
                        startAngle = i * sweep - 90f,
                        sweepAngle = sweep,
                        useCenter = true,
                    )
                }
            }
            Canvas(modifier = Modifier.size(24.dp)) {
                drawCircle(Color.White.copy(alpha = 0.9f), style = Stroke(width = 1.dp.toPx()))
                drawCircle(Color.White, radius = 5.dp.toPx())
                val path = Path().apply {
                    moveTo(size.width / 2, 6.dp.toPx())
                    lineTo(size.width / 2 - 3.dp.toPx(), 1.dp.toPx())
                    lineTo(size.width / 2 + 3.dp.toPx(), 1.dp.toPx())
                    close()
                }
                drawPath(path, Color.White)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            stringResource(R.string.home_roulette_title),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
    }
}

// MARK: - ② 県カード＋主役の県の動画

@Composable
private fun PrefectureSection(
    onOpenPrefecture: (Prefecture) -> Unit,
    onOpenShorts: (List<TrendVideo>, Int) -> Unit,
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(PrefectureFilter.RECENT) }

    fun prefecturesFor(filter: PrefectureFilter): List<Prefecture> = when (filter) {
        PrefectureFilter.RECENT -> RecentPrefectureStore.recents
        PrefectureFilter.FAVORITE -> Prefecture.entries.filter { FavoritePrefectureStore.isFavorite(it) }
        PrefectureFilter.RECOMMENDED -> PrefectureTheme.popularPrefectures
    }

    // 中身が空の絞り込みは出さない（新規ユーザーに空タブを見せない）。おすすめは常に残る。
    val filters = PrefectureFilter.entries.filter { prefecturesFor(it).isNotEmpty() }
    val active = if (selectedFilter in filters) selectedFilter else filters.firstOrNull() ?: PrefectureFilter.RECOMMENDED
    val prefs = prefecturesFor(active)

    val listState = rememberLazyListState()
    // 主役の県: 先頭から見て可視率が 90% を超える最初の県（どれも届かなければ最も見えている県）。
    val focused by remember(prefs) {
        derivedStateOf {
            val info = listState.layoutInfo
            val start = info.viewportStartOffset
            val end = info.viewportEndOffset
            val fractions = info.visibleItemsInfo.associate { item ->
                val visible = (minOf(item.offset + item.size, end) - maxOf(item.offset, start)).coerceAtLeast(0)
                item.index to (if (item.size > 0) visible.toFloat() / item.size else 0f)
            }
            val index = prefs.indices.firstOrNull { (fractions[it] ?: 0f) > 0.9f }
                ?: fractions.maxByOrNull { it.value }?.key
            index?.let { prefs.getOrNull(it) } ?: prefs.firstOrNull()
        }
    }
    LaunchedEffect(prefs) { listState.scrollToItem(0) }

    val trendGroups = TrendRepository.loadedGroups
    // 列に並ぶ県のサムネを先読み（主役が切り替わった時点で手元にあるように）。
    LaunchedEffect(prefs, trendGroups.size) {
        prefetchThumbnails(context, prefs.flatMap { homeTrendGroups(it) }.flatMap { g -> g.videos.map { it.thumbnailUrl } })
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (filters.size > 1) {
            Row(modifier = Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                filters.forEach { filter ->
                    val isActive = filter == active
                    Column(
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .clickable { selectedFilter = filter },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            stringResource(filter.titleRes),
                            fontSize = 15.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) AppTheme.TextPrimary else AppTheme.TextSecondary,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(if (isActive) PlanTheme.Primary else Color.Transparent),
                        )
                    }
                }
            }
        } else {
            Overline(stringResource(active.titleRes))
        }

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        ) {
            items(prefs, key = { it.name }) { pref ->
                PrefectureVisualCard(pref) { onOpenPrefecture(pref) }
            }
        }

        // 主役の県の動画（人気スポット → グルメ）。動画が無い県では何も出さない。
        focused?.let { pref ->
            val groups = homeTrendGroups(pref)
            if (groups.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(top = 2.dp)) {
                    groups.forEach { group ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                trendGroupTitle(group),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.TextSecondary,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                            TrendScrollRow(group = group, onOpen = onOpenShorts)
                        }
                    }
                }
            }
        }
    }
}

/** ホームで主役の県に出すトレンドのグループ（人気スポット → グルメ）。 */
private fun homeTrendGroups(pref: Prefecture): List<TrendGroup> =
    HOME_TREND_CATEGORIES.mapNotNull { TrendRepository.group(pref, it) }

@Composable
private fun Overline(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = AppTheme.TextSecondary,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

/** 県ビジュアルカード（代表写真＋県名）。タップで県詳細へ。 */
@Composable
private fun PrefectureVisualCard(pref: Prefecture, onClick: () -> Unit) {
    val context = LocalContext.current
    val heroId = pref.heroPhotoResName()?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0
    Box(
        modifier = Modifier
            .width(PREF_CARD_WIDTH.dp)
            .height(PREF_CARD_HEIGHT.dp)
            .clip(RoundedCornerShape(CARD_RADIUS.dp))
            .clickable(onClick = onClick),
    ) {
        PhotoOrGradient(heroId)
        BottomScrim()
        Text(
            pref.localizedName(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
        )
    }
}

@Composable
private fun PhotoOrGradient(resId: Int) {
    if (resId != 0) {
        Image(painterResource(resId), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    } else {
        Box(modifier = Modifier.fillMaxSize().background(PlanTheme.brandGradient))
    }
}

@Composable
private fun BottomScrim(alpha: Float = 0.5f) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = alpha))),
    )
}

// MARK: - ③ テーマ

@Composable
private fun ThemeSection(onOpenPrefecture: (Prefecture, SpotDetail?) -> Unit) {
    var selected by remember { mutableStateOf(PrefectureTheme.entries.first()) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 2.dp),
        ) {
            items(PrefectureTheme.entries) { theme ->
                val isSelected = theme == selected
                Text(
                    stringResource(theme.titleRes),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else AppTheme.TextPrimary,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) PlanTheme.Primary else Color.Black.copy(alpha = 0.06f))
                        .clickable { selected = theme }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                )
            }
        }

        val content = selected.content
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        ) {
            when (content) {
                is ThemeContent.Prefectures -> items(content.list, key = { "${selected.name}_${it.name}" }) { pref ->
                    ThemePrefectureCard(pref, selected, onOpenPrefecture)
                }
                is ThemeContent.Items -> items(content.list, key = { it.name }) { item ->
                    ThemeItemCard(item, onOpenPrefecture)
                }
            }
        }
    }
}

/** 固定指標テーマの県カード（写真＝テーマが指す名所。名所名＋県名）。タップで県詳細→名所詳細。 */
@Composable
private fun ThemePrefectureCard(pref: Prefecture, theme: PrefectureTheme, onOpenPrefecture: (Prefecture, SpotDetail?) -> Unit) {
    val context = LocalContext.current
    // テーマが指すスポットを最優先。無ければ県の写真つきスポットの先頭へフォールバック（iOS と同じ）。
    val heroName = theme.heroSpotName(pref)
        ?: pref.tourismInfo?.attractions?.firstOrNull { ATTRACTION_PHOTOS[it.name] != null }?.name
    val heroId = heroName?.let { ATTRACTION_PHOTOS[it] }
        ?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0
    val attraction = heroName?.let { name -> pref.tourismInfo?.attractions?.firstOrNull { it.name == name } }
    ThemeCardFrame(
        onClick = { onOpenPrefecture(pref, attraction?.let { attractionSpotDetail(pref, it) }) },
    ) {
        PhotoOrGradient(heroId)
        BottomScrim()
        CardCaption(
            title = heroName?.let { localizeData(it) } ?: pref.localizedName(),
            subtitle = if (heroName == null) null else pref.localizedName(),
        )
    }
}

/** 「もの」カード（ビーチ）。写真が無ければブランドグラデ＋アイコン。タップで県詳細→自然スポット詳細。 */
@Composable
private fun ThemeItemCard(item: ThemeItem, onOpenPrefecture: (Prefecture, SpotDetail?) -> Unit) {
    val context = LocalContext.current
    val photoId = item.photoResName?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0
    ThemeCardFrame(onClick = { onOpenPrefecture(item.prefecture, natureSpotDetail(item.prefecture, item.name)) }) {
        if (photoId != 0) {
            PhotoOrGradient(photoId)
            BottomScrim()
        } else {
            Box(modifier = Modifier.fillMaxSize().background(PlanTheme.brandGradient))
            Icon(
                Icons.Filled.BeachAccess,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.align(Alignment.Center).size(34.dp),
            )
            BottomScrim(alpha = 0.35f)
        }
        CardCaption(title = localizeData(item.name), subtitle = item.prefecture.localizedName())
    }
}

@Composable
private fun ThemeCardFrame(onClick: () -> Unit, content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .width(150.dp)
            .height(190.dp)
            .clip(RoundedCornerShape(CARD_RADIUS.dp))
            .clickable(onClick = onClick),
        content = content,
    )
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.CardCaption(title: String, subtitle: String?) {
    Column(
        modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (subtitle != null) {
            Text(subtitle, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

