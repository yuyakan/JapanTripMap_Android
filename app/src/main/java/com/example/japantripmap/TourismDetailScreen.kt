package com.example.japantripmap

import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CurrencyYen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import coil.compose.AsyncImage
import java.io.File

// MARK: - セクションのアクセント色（iOS 版の各 Rich セクション accent に合わせる）
internal val GourmetAccent = Color(0xFFED7321) // グルメ: オレンジ (0.93,0.45,0.13)
internal val OnsenAccent = Color(0xFF2194A8) // 温泉: ティール (0.13,0.58,0.66)
internal val SouvenirAccent = Color(0xFFDB5994) // お土産: ピンク (0.86,0.35,0.58)

// ヘッダーのグラデーション（青→紫）。iOS の LinearGradient([.blue,.purple]) 相当。
private val HeaderGradient = Brush.horizontalGradient(listOf(Color(0xFF0A84FF), Color(0xFFAF52DE)))

/** グルメ／お土産カテゴリ -> 表示色（iOS 版の手調整カラーに合わせる）。 */
private val CATEGORY_COLORS = mapOf(
    // gourmet (FoodCategory)
    "ramen" to Color(0xFFED7321),
    "seafood" to Color(0xFF2980D9),
    "meat" to Color(0xFFD6454D),
    "sweets" to Color(0xFFDB5994),
    "local" to Color(0xFF3D9E66),
    "drinks" to Color(0xFF8C61C7),
    "vegetables" to Color(0xFFD1951F),
    // souvenir (SouvenirCategory)
    "food" to Color(0xFFED7321),
    "crafts" to Color(0xFF996B47),
    "textiles" to Color(0xFF2980D9),
    "ceramics" to Color(0xFF737380),
    "regional" to Color(0xFF3D9E66),
)

/** カテゴリの日本語ラベル（タグ表示用の短縮名）。 */
private val CATEGORY_LABELS = mapOf(
    "ramen" to "ラーメン", "seafood" to "海鮮", "meat" to "肉", "sweets" to "スイーツ",
    "local" to "郷土料理", "drinks" to "ドリンク", "vegetables" to "野菜・果物",
    "food" to "食品", "crafts" to "工芸品", "textiles" to "織物", "ceramics" to "陶磁器",
    "regional" to "地域特産",
)

/** 温泉タイプ -> 表示色（iOS 版 OnsenType.color）。 */
private val ONSEN_TYPE_COLORS = mapOf(
    "scenic" to Color(0xFF2980D9),
    "historical" to Color(0xFF996B47),
    "therapeutic" to Color(0xFF3D9E66),
    "resort" to Color(0xFF8C61C7),
    "mountain" to Color(0xFFD9732E),
    "seaside" to Color(0xFF2194A8),
    "ski" to Color(0xFF38A394),
)

private val ONSEN_TYPE_LABELS = mapOf(
    "scenic" to "絶景", "historical" to "歴史", "therapeutic" to "療養",
    "resort" to "リゾート", "mountain" to "山あい", "seaside" to "海辺", "ski" to "スキー",
)

// プラン詳細のルーター（PlanItemDetail.kt）からグルメ/お土産の色・ラベルを再構築するため internal。
// onsen 系は OnsenDetailScreen.kt にも同名 private があるため衝突を避けて private のまま残す。
// ラベルは表示専用なのでロケールに応じて英訳して返す（iOS のフル名称）。
internal fun categoryColor(cat: String) = CATEGORY_COLORS[cat] ?: Color.Gray
internal fun categoryLabel(cat: String) = localizeTypeLabel(CATEGORY_LABELS[cat] ?: cat)
private fun onsenColor(t: String) = ONSEN_TYPE_COLORS[t] ?: Color.Gray
private fun onsenLabel(t: String) = localizeTypeLabel(ONSEN_TYPE_LABELS[t] ?: t)

/** 最初に見せる件数（iOS 版の Array(items.prefix(4)) に合わせる）。 */
private const val INITIAL_GRID_COUNT = 4

// iOS 版の各 Rich セクションの accent。
internal val NatureSectionAccent = Color(0xFF338C5C) // (0.20, 0.55, 0.36)
internal val FestivalSectionAccent = Color(0xFF8C61C7) // (0.55, 0.38, 0.78)
internal val SeasonalSectionAccent = Color(0xFF2980D9) // (0.16, 0.50, 0.85)
private val BrandAccent = PlanTheme.Primary

/**
 * 県詳細画面。iOS 版 TourismDetailView（3.0.0）を移植。
 *
 * 並び（iOS と同一）: 県名 → 人気スポット動画 → 観光地マップ＋観光カード → レクタングル広告
 * → ご当地グルメ → グルメ動画 → お土産 → バナー（温泉か自然がある県のみ）→ 温泉 → 自然
 * → カフェ動画 → 夏祭り・花火大会 → 春・秋・冬の祭り → バナー → 写真カルーセル。
 *
 * - 開いた瞬間に「最近見た県」へ記録する（ホームの最近見た列の元データ）。
 * - 右上のハートでお気に入りをトグル（ホームのお気に入り列）。
 * - 動画（YouTube Shorts）はホームと同じ取得結果を共有し、未取得ならここで読み込む。
 * - 各セクションの見出しからユーザーカードを追加できる。追加したカードは同梱データの前に並べる。
 *   カードを足せるよう、同梱データが無くてもセクションは出す（iOS 3.1.0 と同じ）。
 */
@Composable
fun TourismDetailScreen(
    prefecture: Prefecture,
    store: TravelPlanStore,
    onOpenSpot: (SpotDetail) -> Unit,
    onBack: () -> Unit,
) {
    val info = prefecture.tourismInfo
    val gourmets = prefecture.gourmets
    val souvenirs = prefecture.souvenirs
    val onsens = prefecture.onsens
    val natureSpots = remember(prefecture) { SpotRepository.all(SpotKind.NATURE).filter { it.prefecture == prefecture } }
    val (summerFestivals, seasonalFestivals) = remember(prefecture) { prefecture.splitFestivals() }

    // LazyListScope（非 Composable）内では stringResource を呼べないため、
    // セクション見出し類はここで先に解決しておく。
    val gourmetTitle = stringResource(R.string.tourism_section_gourmet)
    val gourmetSubtitle = stringResource(R.string.tourism_section_gourmet_subtitle, prefecture.localizedName())
    val souvenirTitle = stringResource(R.string.tourism_section_souvenir)
    val souvenirSubtitle = stringResource(R.string.tourism_section_souvenir_subtitle, prefecture.localizedName())
    val natureTitle = stringResource(R.string.tourism_section_nature)
    val onsenTitle = stringResource(R.string.tourism_section_onsen)
    val festivalTitle = stringResource(R.string.tourism_section_festival)
    val festivalSubtitle = stringResource(R.string.tourism_section_festival_subtitle, prefecture.localizedName())
    val seasonalTitle = stringResource(R.string.tourism_section_seasons)
    val seasonalSubtitle = stringResource(R.string.tourism_section_seasons_subtitle, prefecture.localizedName())
    val loadMoreText = stringResource(R.string.tourism_load_more)

    // 全画面で開く Shorts フィード / 写真ビューア。
    var shortsFeed by remember { mutableStateOf<ShortsFeedRequest?>(null) }
    var photoViewerStart by remember { mutableStateOf<Int?>(null) }
    // ユーザーカードの作成・詳細。シートで重ね、閉じたときにスクロール位置を保つ。
    var addingCardKind by remember { mutableStateOf<UserCardKind?>(null) }
    var selectedCard by remember { mutableStateOf<UserCard?>(null) }
    val cardSlot: (UserCardKind, Int) -> UserCardSlot = { kind, tileHeight ->
        UserCardSlot(
            kind = kind,
            cards = UserCardStore.cards(prefecture, kind),
            tileHeight = tileHeight,
            onAdd = { addingCardKind = kind },
            onOpen = { selectedCard = it },
        )
    }

    // 戻ると広告カウントを +2（iOS TourismDetailView の戻るボタンと同じ）。
    val back: () -> Unit = {
        InterstitialAdManager.addDetailPoints()
        onBack()
    }
    BackHandler(onBack = back)

    LaunchedEffect(prefecture) {
        // ホームの「最近見た県」に記録する（マップ・検索・ホームのどこから来てもここを通る）。
        RecentPrefectureStore.record(prefecture)
        // ホームを経由せず直接来た場合でも動画を出せるよう読み込む（取得済みなら何もしない）。
        TrendRepository.loadIfNeeded()
    }

    val photographed = remember(prefecture) {
        info?.attractions?.filter { ATTRACTION_PHOTOS[it.name] != null } ?: emptyList()
    }

    shortsFeed?.let { feed ->
        ShortsFeedScreen(request = feed, onClose = { shortsFeed = null })
        return
    }
    photoViewerStart?.let { start ->
        PhotoViewerScreen(
            photos = photographed.map { ViewerPhoto(it.name, ATTRACTION_PHOTOS[it.name]!!) },
            startIndex = start,
            onClose = { photoViewerStart = null },
        )
        return
    }

    // トレンド（カテゴリごとに対応セクションへ分散配置）。
    val spotTrend = TrendRepository.group(prefecture, "spot")
    val gourmetTrend = TrendRepository.group(prefecture, "gourmet")
    val cafeTrend = TrendRepository.group(prefecture, "cafe")
    val openShorts: (List<TrendVideo>, Int) -> Unit = { videos, index -> shortsFeed = ShortsFeedRequest(videos, index) }
    val isFavorite = FavoritePrefectureStore.isFavorite(prefecture)
    // 観光地カードは地図とカード列に混ぜて出す（位置の無いカードはカード列のみ）。
    val attractions = info?.attractions.orEmpty()
    val attractionCards = UserCardStore.cards(prefecture, UserCardKind.ATTRACTION)
    val mapPlaces = attractionCards.filter { it.hasCoordinate }.map { c ->
        MapPlace(id = c.id, title = c.name, subtitle = c.description, latitude = c.latitude!!, longitude = c.longitude!!)
    } + attractions.map { a ->
        MapPlace(id = a.name, title = a.name, subtitle = a.description, latitude = a.latitude, longitude = a.longitude)
    }

    // 地図ドラッグ中はリストスクロールを止め、地図のパンを親に横取りされないようにする。
    var mapTouched by remember { mutableStateOf(false) }
    // ヘッダーは持たず、戻るボタンを左上・お気に入りを右上にフローティングで重ねる（iOS 版準拠）。
    Box(modifier = Modifier.fillMaxSize().background(AppTheme.Background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            userScrollEnabled = !mapTouched,
        ) {
            // 戻るボタン + 県名の分だけ上部に余白を確保する（ステータスバー + フローティング分）。
            item { Spacer(modifier = Modifier.statusBarsPadding().height(44.dp)) }

            // 県名のグラデーション見出し（中央寄せ）。
            item {
                Text(
                    text = prefecture.localizedName(),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(brush = HeaderGradient),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 人気スポットの Shorts（県名の直下）。
            spotTrend?.let { group -> item(key = "trend_spot") { TrendCategoryRow(group, openShorts) } }

            // 観光地図 + 横スクロールの観光カード。
            item {
                RichSectionHeader(
                    icon = Icons.Filled.Map,
                    title = stringResource(R.string.tourism_section_map),
                    accent = BrandAccent,
                    trailing = { AddUserCardButton(BrandAccent) { addingCardKind = UserCardKind.ATTRACTION } },
                )
            }
            if (mapPlaces.isNotEmpty()) {
                item {
                    PlacesMapSection(
                        showSelectedPlace = false,
                        onMapTouch = { mapTouched = it },
                        places = mapPlaces,
                    )
                }
            }
            if (attractionCards.isNotEmpty() || attractions.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(attractionCards.size) { i ->
                            val c = attractionCards[i]
                            AttractionMiniCard(
                                name = c.name,
                                description = c.description,
                                photo = UserCardStore.photoFile(c),
                                onClick = { selectedCard = c },
                            )
                        }
                        items(attractions.size) { i ->
                            val a = attractions[i]
                            AttractionMiniCard(
                                name = a.name,
                                description = a.description,
                                onClick = { onOpenSpot(attractionSpotDetail(prefecture, a)) },
                            )
                        }
                    }
                }
            }

            // 観光地マップの下だけレクタングル(300x250)（iOS 版と同じ位置・セクション間ユニット）。
            item { MediumRectangleAd(adUnitId = AdConfig.bannerUnitId) }

            // ご当地グルメ（2列リッチカード + もっと見る）。
            richGridSection(
                icon = Icons.Filled.Restaurant,
                title = gourmetTitle,
                subtitle = gourmetSubtitle,
                accent = GourmetAccent,
                count = gourmets.size,
                loadMoreText = loadMoreText,
                slot = cardSlot(UserCardKind.GOURMET, 224),
            ) { index ->
                val g = gourmets[index]
                // iOS GourmetItemCard（プランへの追加は詳細画面から。カードに「＋」は置かない）。
                RichItemCard(
                    name = g.name,
                    description = g.description,
                    accent = categoryColor(g.category),
                    icon = Icons.Filled.Restaurant,
                    tagLabel = categoryLabel(g.category),
                    popularity = g.popularity,
                    price = g.price,
                    season = g.bestSeason,
                    height = 224,
                    onClick = { onOpenSpot(gourmetSpotDetail(prefecture, g)) },
                )
            }

            // グルメの Shorts（グルメセクションの一番下）。
            gourmetTrend?.let { group -> item(key = "trend_gourmet") { TrendCategoryRow(group, openShorts) } }

            // お土産（2列リッチカード + もっと見る）。
            richGridSection(
                icon = Icons.Filled.CardGiftcard,
                title = souvenirTitle,
                subtitle = souvenirSubtitle,
                accent = SouvenirAccent,
                count = souvenirs.size,
                loadMoreText = loadMoreText,
                slot = cardSlot(UserCardKind.SOUVENIR, 200),
            ) { index ->
                val s = souvenirs[index]
                // iOS SouvenirItemCard（時期＋カテゴリタグのみ。価格はカードに出さない）。
                RichItemCard(
                    name = s.name,
                    description = s.description,
                    accent = categoryColor(s.category),
                    icon = Icons.Filled.CardGiftcard,
                    tagLabel = categoryLabel(s.category),
                    popularity = s.popularity,
                    price = null,
                    season = s.bestSeason,
                    height = 200,
                    onClick = { onOpenSpot(souvenirSpotDetail(prefecture, s)) },
                )
            }

            // 温泉・自然は見出しだけでも出るが、カードが 1 枚も無いと実質的にこの広告が直下のカフェ動画と
            // 隣り合ってしまうため出さない（iOS hasSectionBelowAd。広告と動画を隣接させない）。
            val onsenSlot = cardSlot(UserCardKind.ONSEN, 180)
            val natureSlot = cardSlot(UserCardKind.NATURE, 180)
            if (onsens.isNotEmpty() || natureSpots.isNotEmpty() || onsenSlot.cards.isNotEmpty() || natureSlot.cards.isNotEmpty()) {
                item { AdaptiveBannerAd(adUnitId = AdConfig.bannerUnitId) }
            }

            // 温泉（2列リッチカード・全件）。
            richGridSection(
                icon = Icons.Filled.Spa,
                title = onsenTitle,
                subtitle = null,
                accent = OnsenAccent,
                count = onsens.size,
                loadMoreText = loadMoreText,
                slot = onsenSlot,
                initialCount = Int.MAX_VALUE,
            ) { index ->
                val o = onsens[index]
                // iOS OnsenCard（名前→タイプタグ→説明。自然セクションのカードと同じ構成）。
                val meta = SpotKind.ONSEN.typeMeta(o.type)
                SpotSectionCard(
                    name = o.name,
                    description = o.description,
                    color = meta.color,
                    icon = meta.icon,
                    tagLabel = localizeTypeLabel(meta.tagName),
                    popularity = o.popularity,
                    onClick = { onsenSpotDetail(prefecture, o.name)?.let(onOpenSpot) },
                )
            }

            // 自然（自然タブと同じスポットを県で絞り込み。4件＋もっと見る）。
            richGridSection(
                icon = Icons.Filled.Park,
                title = natureTitle,
                subtitle = null,
                accent = NatureSectionAccent,
                count = natureSpots.size,
                loadMoreText = loadMoreText,
                slot = natureSlot,
            ) { index ->
                val spot = natureSpots[index]
                SpotSectionCard(
                    name = spot.name,
                    description = spot.description,
                    color = spot.typeMeta.color,
                    icon = spot.typeMeta.icon,
                    tagLabel = localizeTypeLabel(spot.typeMeta.name),
                    popularity = spot.popularity,
                    onClick = { onOpenSpot(spot.toSpotDetail()) },
                )
            }

            // カフェの Shorts（自然セクションの下に独立セクション）＋作成したカード。
            // カードを追加できるよう、動画が無い県でも見出しは出す。
            item(key = "cafe_header") {
                RichSectionHeader(
                    icon = Icons.Filled.LocalCafe,
                    title = stringResource(R.string.home_trend_category_cafe),
                    accent = BrandAccent,
                    trailing = { AddUserCardButton(BrandAccent) { addingCardKind = UserCardKind.CAFE } },
                )
            }
            cafeTrend?.let { group ->
                item(key = "trend_cafe_row") {
                    TrendScrollRow(group = group, horizontalPadding = 20, onOpen = openShorts, modifier = Modifier.bleedHorizontally(16.dp))
                }
            }
            richGrid(
                key = "cafe",
                accent = BrandAccent,
                count = 0,
                loadMoreText = loadMoreText,
                slot = cardSlot(UserCardKind.CAFE, 200),
                initialCount = INITIAL_GRID_COUNT,
            ) {}

            // 夏祭り・花火大会（4件＋もっと見る）。
            richGridSection(
                icon = Icons.Filled.Celebration,
                title = festivalTitle,
                subtitle = festivalSubtitle,
                accent = FestivalSectionAccent,
                count = summerFestivals.size,
                loadMoreText = loadMoreText,
                slot = cardSlot(UserCardKind.FESTIVAL, 200),
            ) { index ->
                val f = summerFestivals[index]
                FestivalSectionCard(f) { onOpenSpot(festivalSpotDetail(prefecture, f)) }
            }

            // 春・秋・冬の祭り（4件＋もっと見る）。
            richGridSection(
                icon = Icons.Filled.AcUnit,
                title = seasonalTitle,
                subtitle = seasonalSubtitle,
                accent = SeasonalSectionAccent,
                count = seasonalFestivals.size,
                loadMoreText = loadMoreText,
                slot = cardSlot(UserCardKind.OTHER_FESTIVAL, 200),
            ) { index ->
                val f = seasonalFestivals[index]
                FestivalSectionCard(f) { onOpenSpot(festivalSpotDetail(prefecture, f)) }
            }

            item { AdaptiveBannerAd(adUnitId = AdConfig.bannerUnitId) }

            // 実写フォトカルーセル（写真があるスポットのみ）。タップで全画面ビューア。
            if (photographed.isNotEmpty()) {
                item {
                    RichSectionHeader(
                        icon = Icons.Filled.PhotoLibrary,
                        title = stringResource(R.string.tourism_section_photos),
                        accent = BrandAccent,
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(photographed.size) { i ->
                            val a = photographed[i]
                            PhotoCard(
                                spotName = a.name,
                                resName = ATTRACTION_PHOTOS[a.name]!!,
                                onClick = { photoViewerStart = i },
                            )
                        }
                    }
                }
            }

            // 最下部の余白。
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }

        // 戻るボタン（左上フローティング・白い角丸ピル）。
        FloatingBackButton(
            onBack = back,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 10.dp),
        )

        // お気に入りトグル（右上。ホームの「お気に入り」列に反映）。
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 8.dp)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { FavoritePrefectureStore.toggle(prefecture) }
                .padding(10.dp),
        ) {
            Icon(
                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = stringResource(if (isFavorite) R.string.tourism_unfavorite else R.string.tourism_favorite),
                tint = if (isFavorite) Color(0xFFFF2D55) else Color.Gray,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    addingCardKind?.let { kind ->
        FullScreenSheet(onDismiss = { addingCardKind = null }) {
            UserCardEditor(prefecture = prefecture, kind = kind, store = store, onClose = { addingCardKind = null })
        }
    }
    selectedCard?.let { card ->
        FullScreenSheet(onDismiss = { selectedCard = null }) {
            SpotDetailScreen(detail = card.toSpotDetail(), store = store, onClose = { selectedCard = null })
        }
    }
}

/** カテゴリ名の見出し＋動画の横スクロール（画面先頭・グルメ末尾に差し込む用）。 */
@Composable
private fun TrendCategoryRow(group: TrendGroup, onOpen: (List<TrendVideo>, Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        Text(trendCategoryName(group), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppTheme.TextSecondary)
        TrendScrollRow(group = group, horizontalPadding = 20, onOpen = onOpen, modifier = Modifier.bleedHorizontally(16.dp))
    }
}

/**
 * 県詳細の温泉・自然セクションのカード（iOS OnsenCard / NatureSectionCard）。
 * アイコン丸＋星 → 名前 → タイプのタグ → 説明。
 */
@Composable
private fun SpotSectionCard(
    name: String,
    description: String,
    color: Color,
    icon: ImageVector,
    tagLabel: String,
    popularity: Int,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
            .fillMaxHeight()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x14000000))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Brush.linearGradient(listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.1f))), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(color.copy(alpha = 0.8f), color))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            StarRow(popularity, Color(0xFFFFCC00))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(localizeData(name), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(tagLabel, fontSize = 11.sp, color = color, maxLines = 1)
            }
        }
        Text(localizeData(description), fontSize = 12.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** 県詳細の祭りセクションのカード（iOS FestivalItemCard: アイコン丸＋規模の星＋名前＋説明＋時期・期間・タグ）。 */
@Composable
private fun FestivalSectionCard(festival: Festival, onClick: () -> Unit) {
    val color = festColor(festival.category)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 224.dp)
            .fillMaxHeight()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Brush.linearGradient(listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.1f))), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(color.copy(alpha = 0.8f), color))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(festIcon(festival.category), contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            StarRow(festival.scale, Color(0xFFFF9500))
        }
        Text(localizeData(festival.name), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(localizeData(festival.description), fontSize = 12.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(localizeData(festival.month), fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                localizeData(festival.duration),
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                festLabel(festival.category),
                fontSize = 11.sp,
                color = color,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun StarRow(count: Int, color: Color) {
    Row {
        repeat(5) { i ->
            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                tint = if (i < count) color else Color(0x4D9E9E9E),
                modifier = Modifier.size(11.dp),
            )
        }
    }
}

/** 左上に浮かぶ白い角丸ピルの戻るボタン（chevron + 「戻る」）。 */
@Composable
private fun FloatingBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(onClick = onBack)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBackIos,
            contentDescription = stringResource(R.string.common_back),
            tint = Color.Black,
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(stringResource(R.string.common_back), fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
    }
}

/** セクションに並べるユーザーカードと、その追加・表示の操作。 */
private class UserCardSlot(
    val kind: UserCardKind,
    val cards: List<UserCard>,
    val tileHeight: Int,
    val onAdd: () -> Unit,
    val onOpen: (UserCard) -> Unit,
)

/**
 * 「見出し + 2列グリッド + もっと見る」をまとめて LazyColumn に流し込むヘルパー。
 * iOS 版 RichGourmetSection / RichSouvenirSection の「4件まで表示→もっと見る」挙動を再現する。
 * 見出しにカード追加ボタンを置き、[slot] のカードを同梱データの前に並べる。
 */
private fun androidx.compose.foundation.lazy.LazyListScope.richGridSection(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    accent: Color,
    count: Int,
    loadMoreText: String,
    slot: UserCardSlot,
    initialCount: Int = INITIAL_GRID_COUNT,
    card: @Composable (index: Int) -> Unit,
) {
    item(key = "header_$title") {
        RichSectionHeader(
            icon = icon,
            title = title,
            subtitle = subtitle,
            accent = accent,
            trailing = { AddUserCardButton(accent, slot.onAdd) },
        )
    }
    richGrid(title, accent, count, loadMoreText, slot, initialCount, card)
}

/** 2列グリッド + もっと見る。ユーザーカードは常に全件出し、同梱データだけ [initialCount] 件で畳む。 */
private fun androidx.compose.foundation.lazy.LazyListScope.richGrid(
    key: String,
    accent: Color,
    count: Int,
    loadMoreText: String,
    slot: UserCardSlot,
    initialCount: Int,
    card: @Composable (index: Int) -> Unit,
) {
    val cardCount = slot.cards.size
    if (cardCount + count == 0) return
    // showAll をこのセクション内で保持したいので、状態は item スコープ内で持つ。
    // 展開状態はヘッダー直下の 1 item にまとめて描画し、内部で行を組む。
    item(key = "grid_$key") {
        var showAll by remember { mutableStateOf(false) }
        val visible = cardCount + if (showAll) count else minOf(count, initialCount)
        val cell: @Composable (index: Int) -> Unit = { index ->
            if (index < cardCount) {
                val userCard = slot.cards[index]
                UserCardTile(userCard, slot.tileHeight) { slot.onOpen(userCard) }
            } else {
                card(index - cardCount)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            var row = 0
            while (row * 2 < visible) {
                val left = row * 2
                val right = left + 1
                // 同じ行の 2 枚は高さを揃える（中身の多い方に合わせる）。
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) { cell(left) }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (right < visible) cell(right)
                    }
                }
                row++
            }

            if (!showAll && count > initialCount) {
                LoadMoreButton(title = loadMoreText, accent = accent) { showAll = true }
            }
        }
    }
}

/**
 * セクション見出し（アクセントバー + アイコンチップ + タイトル + サブタイトル）。
 * iOS 版 RichSectionHeader を再現。
 */
@Composable
private fun RichSectionHeader(
    icon: ImageVector,
    title: String,
    accent: Color,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // アクセントバー
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Spacer(modifier = Modifier.width(12.dp))
        // アイコンチップ
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

/** 「もっと見る」ボタン（アクセント色ソフト塗り）。iOS 版 LoadMoreButton を再現。 */
@Composable
private fun LoadMoreButton(title: String, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.width(6.dp))
        Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

/** 観光地リストの横スクロールミニカード。iOS 版の attraction カードを再現。 */
@Composable
private fun AttractionMiniCard(name: String, description: String, photo: File? = null, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(220.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, AttractionAccent.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xCC0A84FF), Color(0x99AF52DE)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (photo != null) {
                    AsyncImage(
                        model = photo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                localizeData(name),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            localizeData(description),
            fontSize = 12.sp,
            color = Color.Gray,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * グルメ／お土産の 2列リッチカード。iOS 版 GourmetItemCard / SouvenirItemCard と同じ構成:
 * グラデーション円アイコン＋星 → 名前・説明 → 下部に「¥ 価格」（グルメのみ）と「📅 時期＋カテゴリタグ」。
 * プランへの追加は詳細画面から行う（iOS と同じく、カードに「＋」は置かない）。
 */
@Composable
private fun RichItemCard(
    name: String,
    description: String,
    accent: Color,
    icon: ImageVector,
    tagLabel: String,
    popularity: Int,
    price: String?,
    season: String?,
    height: Int,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // iOS の固定高さを最小値にし、Android の文字メトリクスで溢れるときは伸ばす（下部の文字が切れないように）。
            .heightIn(min = height.dp)
            .fillMaxHeight()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(
                1.dp,
                Brush.linearGradient(listOf(accent.copy(alpha = 0.3f), accent.copy(alpha = 0.1f))),
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // トップ: グラデーション円アイコン + 星
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.8f), accent))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            StarRow(popularity, Color(0xFFFFCC00))
        }

        // 名前 + 説明
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(localizeData(name), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(localizeData(description), fontSize = 12.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }

        Spacer(modifier = Modifier.weight(1f))

        // 下部: 価格（グルメ）/ 時期 + カテゴリタグ
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (!price.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CurrencyYen, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(localizeData(price), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AppTheme.TextPrimary, maxLines = 1)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!season.isNullOrBlank()) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        localizeData(season),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    tagLabel,
                    fontSize = 11.sp,
                    color = accent,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/** フォトギャラリーの 1 枚（写真 + スポット名）。 */
@Composable
private fun PhotoCard(spotName: String, resName: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val photoId = context.resources.getIdentifier(resName, "drawable", context.packageName)
    if (photoId == 0) return
    Column(
        modifier = Modifier
            .width(260.dp)
            .appCard()
            .clickable(onClick = onClick),
    ) {
        Image(
            painter = painterResource(id = photoId),
            contentDescription = spotName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
        Text(
            text = localizeData(spotName),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}
