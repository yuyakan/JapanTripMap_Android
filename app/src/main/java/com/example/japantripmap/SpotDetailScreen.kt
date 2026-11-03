package com.example.japantripmap

import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * 単体詳細に渡す汎用データ。観光/グルメ/温泉/自然/祭りの各項目を共通表現にする。
 */
data class SpotDetail(
    val title: String,
    val prefecture: Prefecture,
    val accent: Color,
    val icon: ImageVector,
    val description: String,
    val planCategory: PlanItemCategory,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** ヒーローに出す写真の drawable リソース名（拡張子なし）。null なら写真なし。 */
    val photoResName: String? = null,
    /** バッジ（カテゴリ/タイプ）ラベル。null なら非表示。 */
    val badge: String? = null,
    /** 人気度（1-5）。null なら星非表示。 */
    val popularity: Int? = null,
    /** 「基本情報」の行（ラベル→値）。空なら情報カード非表示。 */
    val infoRows: List<Pair<String, String>> = emptyList(),
    /** 特徴タグ（祭り用）。 */
    val features: List<String> = emptyList(),
    /** 食べログ検索を出すか（グルメ用）。 */
    val tabelogKeyword: String? = null,
    /**
     * SNS 検索・プラン保存に使う元の名称（原則そのスポットの日本語名）。
     * title は英語ロケールで英訳されるが、検索や食べログは日本語のほうが結果が良いので分ける。
     * null なら title を使う。
     */
    val searchQuery: String? = null,
    /**
     * 写真が無くても「写真あり」と同じ本文スタイル（白背景＋カード枠線・アイコン丸なし）にするか。
     * iOS では観光スポット詳細だけが常にこの見た目（AttractionDetailView）。
     */
    val alwaysPhotoStyle: Boolean = false,
    /** 「おすすめの楽しみ方」の文言リソース（グルメのみ。カテゴリごとに 3 つ）。 */
    val tipResIds: List<Int> = emptyList(),
    /** ユーザーカードの id。写真・URL・編集/削除の表示に使い、広告は出さない（iOS UserCardDetailView）。 */
    val userCardId: String? = null,
)

/**
 * プラン経由で詳細を開いたときの文脈。iOS 版 PlanLocatableDetailView の位置編集機能に対応。
 * この項目（planItem）が属するプラン planId を持ち、グルメ・お土産に「位置を追加」機能を付ける。
 */
data class SpotPlanContext(
    val planId: String,
    val planItem: PlanItem,
)

/**
 * 汎用の単体詳細画面。iOS 版 AttractionDetailView / GourmetDetailView / OnsenDetailView 等を統合移植。
 * ヒーローヘッダー → アクション（プランに追加＋SNS検索） → 地図 → 説明 → 基本情報。
 *
 * planContext が渡され、かつグルメ・お土産（元データに座標を持たない）の場合は、
 * iOS 版と同じく右下に「位置を追加」ボタンを出し、設定した位置を地図カードとして表示する。
 *
 * ユーザーカード（[SpotDetail.userCardId]）は編集がすぐ反映されるよう最新のカードから組み直す。
 * 削除直後は開いた時点の値で表示を保つ。
 */
@Composable
fun SpotDetailScreen(
    detail: SpotDetail,
    store: TravelPlanStore,
    onClose: () -> Unit,
    planContext: SpotPlanContext? = null,
) {
    val userCard = detail.userCardId?.let(UserCardStore::card)
    SpotDetailContent(userCard?.toSpotDetail() ?: detail, userCard, store, onClose, planContext)
}

@Composable
private fun SpotDetailContent(
    detail: SpotDetail,
    userCard: UserCard?,
    store: TravelPlanStore,
    onClose: () -> Unit,
    planContext: SpotPlanContext?,
) {
    val context = LocalContext.current
    // 詳細を閉じたら広告カウントを +2（iOS 各詳細ビューの戻る処理と同じ。どこから開いても加算する）。
    val close: () -> Unit = {
        InterstitialAdManager.addDetailPoints()
        onClose()
    }
    BackHandler(onBack = close)
    var showAddDialog by remember { mutableStateOf(false) }

    // プラン経由 かつ 元データに座標が無いカテゴリ（グルメ・お土産）でのみ位置編集を許可する。
    // iOS 版 allowsLocation（allowsUserCoordinate && !isCustom）に対応。
    val allowsLocation = planContext != null &&
        detail.planCategory in setOf(PlanItemCategory.GOURMET, PlanItemCategory.SOUVENIR)
    // store から最新の項目を追う（位置の保存後に地図カードへ即反映するため）。
    val plans = store.plans
    val currentItem = remember(plans, planContext?.planItem?.id) {
        planContext?.let { ctx ->
            store.plan(ctx.planId)?.items?.firstOrNull { it.id == ctx.planItem.id } ?: ctx.planItem
        }
    }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showMemoEditor by remember { mutableStateOf(false) }
    var showCardMenu by remember { mutableStateOf(false) }
    var showCardEditor by remember { mutableStateOf(false) }
    var showCardDeleteConfirm by remember { mutableStateOf(false) }

    // 位置ピッカー表示中は最前面に全画面で出す（ボトムシートと違い確実に前面へ）。
    if (showLocationPicker && currentItem != null && planContext != null) {
        LocationPickerScreen(
            initialLat = currentItem.latitude,
            initialLng = currentItem.longitude,
            onCancel = { showLocationPicker = false },
            onPick = { lat, lng, resolvedAddress ->
                store.updateItem(
                    planContext.planId,
                    currentItem.copy(
                        latitude = lat,
                        longitude = lng,
                        placeName = null, // 手動ピンでは施設名を持たない（住所を主表示に使う）
                        address = resolvedAddress,
                    ),
                )
                showLocationPicker = false
            },
        )
        return
    }

    if (showAddDialog) {
        AddToPlanDialog(
            store = store,
            item = PlanItem(
                category = detail.planCategory,
                prefectureName = detail.prefecture.displayName,
                name = detail.title,
                detail = detail.description,
            ),
            onDismiss = { showAddDialog = false },
            onAdded = { planTitle ->
                showAddDialog = false
                Toast.makeText(context, context.getString(R.string.added_to_plan, planTitle), Toast.LENGTH_SHORT).show()
            },
        )
    }

    // 地図ドラッグ中は本文スクロールを止め、地図のパンを親に横取りされないようにする。
    var mapTouched by remember { mutableStateOf(false) }
    // 写真ありの見た目（iOS PhotoHeaderStyle）: 本文は白・カードに枠線。写真なしはブランドの淡いグラデーション。
    val photoStyle = detail.hasPhoto(context) || detail.alwaysPhotoStyle
    val cardLocation = userCard?.takeIf { it.hasCoordinate }?.toLocationItem()
    val cardModifier = if (photoStyle) {
        Modifier.border(1.dp, AppTheme.TextPrimary, RoundedCornerShape(AppTheme.CardCorner))
    } else {
        Modifier
    }
    // ユーザーカードは広告が無く本文が短いので、画面の余りに基調色が見えないよう背景を本文に揃える。
    val pageBackground = if (detail.userCardId != null) AppTheme.Background else detail.accent
    Box(modifier = Modifier.fillMaxSize().background(pageBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState(), enabled = !mapTouched),
        ) {
            // ヒーローヘッダー。
            HeroHeader(detail, userCard)

            // 本文（薄い背景の上にカードを重ねる）。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (photoStyle) Modifier.background(AppTheme.Background)
                        else Modifier.background(PlanTheme.backgroundGradient),
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // アクションカード（プランに追加 + SNS検索）。
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appCard()
                        .then(cardModifier)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AddToPlanButton(detail.accent) { showAddDialog = true }
                    userCard?.openableUrl?.let { url ->
                        WebsiteButton(label = userCard.url ?: url, accent = detail.accent) {
                            ExternalLinks.openWebsite(context, url)
                        }
                    }
                    Text(stringResource(R.string.detail_more_search), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    SocialSearchButtons(
                        query = detail.searchQuery ?: detail.title,
                        tabelogArea = if (detail.tabelogKeyword != null) detail.prefecture.slug else null,
                        tabelogKeyword = detail.tabelogKeyword ?: detail.searchQuery ?: detail.title,
                    )
                }

                // プランで位置を設定したグルメ・お土産は、その位置を地図カードで表示する
                // （iOS 版 PlanItemPlaceMap 相当）。
                if (allowsLocation && currentItem?.hasCoordinate == true && cardLocation == null) {
                    PlanItemLocationCard(item = currentItem, accent = detail.accent, onMapTouch = { mapTouched = it })
                }

                // 説明カード。
                if (detail.description.isNotBlank()) {
                    DetailCard(cardModifier) {
                        SectionHeader(Icons.Filled.LocationOn, stringResource(R.string.detail_more_info), detail.accent)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(localizeData(detail.description), fontSize = 14.sp, color = Color(0xFF444444), lineHeight = 22.sp)
                    }
                }

                // 説明の下にレクタングル(300x250)（iOS 版の各詳細ビューと同じ位置）。
                if (detail.userCardId == null) {
                    MediumRectangleAd(adUnitId = AdConfig.detailBannerUnitId)
                }

                // 基本情報カード。
                if (detail.infoRows.isNotEmpty()) {
                    DetailCard(cardModifier) {
                        SectionHeader(Icons.Filled.Star, stringResource(R.string.detail_basic_info), detail.accent)
                        Spacer(modifier = Modifier.height(6.dp))
                        detail.infoRows.forEachIndexed { i, (label, value) ->
                            if (i > 0) HorizontalDivider(color = Color(0x11000000))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(localizeInfoLabel(label), fontSize = 14.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(localizeData(value), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // おすすめの楽しみ方（グルメ。iOS GourmetDetailView.recommendationsCard）。
                if (detail.tipResIds.isNotEmpty()) {
                    DetailCard(cardModifier) {
                        SectionHeader(Icons.Filled.Lightbulb, stringResource(R.string.gourmet_recommended_enjoyment), detail.accent)
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            detail.tipResIds.forEach { res ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = detail.accent, modifier = Modifier.padding(top = 2.dp).size(14.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(stringResource(res), fontSize = 14.sp, color = Color(0xFF444444))
                                }
                            }
                        }
                    }
                }

                // 特徴タグ（祭り）。
                if (detail.features.isNotEmpty()) {
                    DetailCard(cardModifier) {
                        SectionHeader(Icons.Filled.Star, stringResource(R.string.detail_features), detail.accent)
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            detail.features.forEach { feat ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(detail.accent, CircleShape),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(localizeData(feat), fontSize = 14.sp, color = Color(0xFF444444))
                                }
                            }
                        }
                    }
                }

                // 地図（iOS: 観光・温泉・自然は広告の直下、祭りは見どころの後＝情報カード類の後ろ）。
                detail.mapPlace()?.let { place ->
                    PlacesMapSection(
                        title = stringResource(R.string.common_map),
                        places = listOf(place),
                        onMapTouch = { mapTouched = it },
                    )
                }

                // ユーザーカードに設定した位置。
                cardLocation?.let { item ->
                    PlanItemLocationCard(item = item, accent = detail.accent, onMapTouch = { mapTouched = it })
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // 右上フローティング（ユーザーカードのメニュー・閉じる）。
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 44.dp, end = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (userCard != null) {
                Box {
                    TopCircleButton(Icons.Filled.MoreHoriz, stringResource(R.string.usercard_menu), detail.accent) { showCardMenu = true }
                    DropdownMenu(expanded = showCardMenu, onDismissRequest = { showCardMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.usercard_edit)) },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = {
                                showCardMenu = false
                                showCardEditor = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.usercard_delete), color = Color.Red) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.Red) },
                            onClick = {
                                showCardMenu = false
                                showCardDeleteConfirm = true
                            },
                        )
                    }
                }
            }
            TopCircleButton(Icons.Filled.Close, stringResource(R.string.common_close), detail.accent, close)
        }

        // プラン経由で開いたときの右下フローティング（iOS 版 PlanLocatableDetailView）。
        // 上から「位置」（グルメ・お土産のみ）→「メモ」（全カテゴリ）。
        if (planContext != null && currentItem != null) {
            Column(
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (allowsLocation) {
                    val hasLocation = currentItem.hasCoordinate
                    PlanFab(
                        if (hasLocation) Icons.Filled.EditLocation else Icons.Filled.AddLocationAlt,
                        stringResource(if (hasLocation) R.string.detail_change_location else R.string.detail_add_location),
                    ) { showLocationPicker = true }
                }
                val hasMemo = !currentItem.memo.isNullOrEmpty()
                PlanFab(
                    if (hasMemo) Icons.Filled.EditNote else Icons.AutoMirrored.Filled.Notes,
                    stringResource(if (hasMemo) R.string.plan_item_editmemo else R.string.plan_item_addmemo),
                ) { showMemoEditor = true }
            }
        }
    }

    if (userCard != null && showCardEditor) {
        FullScreenSheet(onDismiss = { showCardEditor = false }) {
            UserCardEditor(
                prefecture = detail.prefecture,
                kind = userCard.kind,
                store = store,
                editing = userCard,
                onClose = { showCardEditor = false },
            )
        }
    }

    if (userCard != null && showCardDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showCardDeleteConfirm = false },
            text = { Text(stringResource(R.string.usercard_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    UserCardStore.delete(userCard)
                    onClose()
                }) {
                    Text(stringResource(R.string.common_delete), color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCardDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showMemoEditor && planContext != null && currentItem != null) {
        FullScreenSheet(onDismiss = { showMemoEditor = false }) {
            PlanMemoEditorScreen(
                initial = currentItem.memo.orEmpty(),
                title = stringResource(R.string.plan_item_memo_title),
                onDismiss = { showMemoEditor = false },
                onSave = { text ->
                    store.updateItem(planContext.planId, currentItem.copy(memo = text.takeIf { it.isNotBlank() }))
                    showMemoEditor = false
                },
            )
        }
    }
}

/** 右上に浮かぶ丸いボタン。 */
@Composable
private fun TopCircleButton(icon: ImageVector, description: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** ユーザーカードの Web サイトを開くボタン。 */
@Composable
private fun WebsiteButton(label: String, accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Language, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.usercard_url_open), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            // 長い URL は途中を省略して、ドメインと末尾の両方を見せる。
            Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun HeroHeader(detail: SpotDetail, userCard: UserCard?) {
    val context = LocalContext.current
    val photoId = detail.photoResName?.let {
        context.resources.getIdentifier(it, "drawable", context.packageName)
    } ?: 0
    val cardPhoto = userCard?.let(UserCardStore::photoFile)

    Box(modifier = Modifier.fillMaxWidth()) {
        // 写真があれば背景に敷く。無ければアクセント色のグラデーション。
        if (photoId != 0 || cardPhoto != null) {
            if (cardPhoto != null) {
                AsyncImage(
                    model = cardPhoto,
                    contentDescription = detail.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Image(
                    painter = painterResource(id = photoId),
                    contentDescription = detail.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize(),
                )
            }
            // 下部を暗くして文字を読みやすくするオーバーレイ。
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.65f)),
                        ),
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(listOf(detail.accent, detail.accent.copy(alpha = 0.85f))),
                    ),
            )
        }

        // 写真ありは iOS PhotoHeaderStyle.topPadding（150pt）でタイトルを下寄せにし、アイコン丸は出さない。
        HeroContent(
            detail,
            showIconCircle = photoId == 0 && !detail.alwaysPhotoStyle,
            topPadding = if (photoId != 0 || detail.alwaysPhotoStyle) 150 else 90,
        )
    }
}

/** この詳細に表示できる写真があるか。 */
fun SpotDetail.hasPhoto(context: android.content.Context): Boolean =
    (photoResName?.let { context.resources.getIdentifier(it, "drawable", context.packageName) } ?: 0) != 0

@Composable
private fun HeroContent(detail: SpotDetail, showIconCircle: Boolean, topPadding: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = topPadding.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 写真がないときだけアイコン丸を出す（写真時は写真が主役）。
        if (showIconCircle) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(detail.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }
        Text(localizeData(detail.title), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "${detail.prefecture.localizedName()}（${detail.prefecture.localizedRegionName()}）",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
        // バッジ＋星。
        if (detail.badge != null || detail.popularity != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (detail.badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(localizeTypeLabel(detail.badge), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
                detail.popularity?.let { pop ->
                    Row {
                        repeat(5) { i ->
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                tint = if (i < pop) Color.White else Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCard(
    extra: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .appCard()
            .then(extra)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .background(accent, RoundedCornerShape(2.dp)),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AddToPlanButton(accent: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(accent)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.common_add_to_plan), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
