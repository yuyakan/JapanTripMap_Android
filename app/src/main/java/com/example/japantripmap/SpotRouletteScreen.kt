package com.example.japantripmap

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * マーカー/一覧タップで開く詳細画面用の SpotDetail に変換する。
 * title / description / badge / infoRows は日本語のまま渡し、SpotDetailScreen 側の
 * 描画時に localizeData / localizeInfoLabel でロケールに応じて英訳する。
 */
fun RouletteSpot.toSpotDetail(): SpotDetail = SpotDetail(
    title = name,
    prefecture = prefecture,
    accent = typeMeta.color,
    icon = typeMeta.icon,
    description = description,
    planCategory = if (kind == SpotKind.ONSEN) PlanItemCategory.ONSEN else PlanItemCategory.NATURE,
    latitude = latitude,
    longitude = longitude,
    badge = localizeTypeLabel(typeMeta.name),
    popularity = popularity,
    infoRows = listOf((if (kind == SpotKind.ONSEN) "泉質タイプ" else "種別") to localizeTypeLabel(typeMeta.name)),
)

private val MapFillStart = Color(0x6633C481)
private val MapFillEnd = Color(0x8014B8A6)
private val MapStroke = Color(0x66FFFFFF)

/**
 * 温泉・自然の「スポット単位ルーレット」タブ画面。
 * iOS 版 OnsenMapView / NatureSpotMapView を移植。
 *
 * 上部に設定ボタン（左）と地図／リスト切替（右）。中央に地図（有効スポットのマーカー）またはリスト。
 * 右下に Start/Stop ボタン。停止シーケンス中は選択スポット名を大きく表示、停止後に結果モーダル。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotRouletteScreen(
    viewModel: SpotRouletteViewModel,
    onOpenSpot: (RouletteSpot) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 画面が前面に来たら（iOS の handleMapAppear 相当）広告発火判定＋次の広告を先読みする。
    LaunchedEffect(Unit) { InterstitialAdManager.handleScreenAppear(context) }

    val isBusy = viewModel.isSpinning || viewModel.isStopping
    val visibleSpots = viewModel.visibleEnabledSpots

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.Background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // 上部バー：設定（左）＋地図/リスト切替アイコン（右）。スピン中・結果表示中は隠す。
            // 隠すときは通常時ヘッダーと同じ高さの placeholder を出し、スピン開始で地図の縦位置が
            // ズレないようにする（都道府県タブと同じ実測方式。測定前は 52dp をフォールバック）。
            var headerHeight by remember { mutableStateOf(52.dp) }
            val density = androidx.compose.ui.platform.LocalDensity.current
            if (!isBusy && !viewModel.showResultModal) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { with(density) { headerHeight = it.height.toDp() } },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // iOS: 設定はアイコン＋「設定」ラベルの白丸ボタン。
                        PillButton(icon = Icons.Filled.Settings, label = stringResource(R.string.common_settings)) { showSettings = true }
                        Spacer(modifier = Modifier.weight(1f))
                        // iOS: 切替はアイコンのみ。
                        IconPillButton(
                            icon = if (viewModel.isListView) Icons.Filled.Map else Icons.AutoMirrored.Filled.List,
                            contentDescription = stringResource(if (viewModel.isListView) R.string.spot_roulette_map_view else R.string.spot_roulette_list_view),
                        ) { viewModel.isListView = !viewModel.isListView }
                    }
                    // iOS(自然): 現在表示中のタイプを右上に小さくチップ表示。
                    // 都道府県タブの 22dp スペーサーと合わせ、3 タブで地図の縦位置を一致させるため
                    // チップ行の高さ（22dp）は常に固定で確保し、自然タブのときだけ中身を描く
                    // （温泉は空スロットで同じ高さを占有）。
                    // チップ（アイコン＋10sp テキスト）は 22dp に収まるので、
                    // bottom パディングは付けない（付けると実効高さが減ってチップが見切れる）。
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .padding(end = 16.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        if (viewModel.kind == SpotKind.NATURE) {
                            DisplayTypeChips(
                                types = viewModel.allTypes.filter { it.key in viewModel.selectedDisplayTypes },
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(headerHeight))
            }

            if (viewModel.isListView) {
                SpotListView(
                    viewModel = viewModel,
                    onOpenSpot = { if (!isBusy) onOpenSpot(it) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
                SpotMap(
                    spots = visibleSpots,
                    selected = viewModel.selectedSpot,
                    onTapSpot = { if (!isBusy && !viewModel.showResultModal) onOpenSpot(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                // スピン中／停止中に選択スポット名を大きく表示。
                // Box の高さは 64dp 固定にして地図の縦位置を動かさない。
                // 名前が 2〜3 行になったときは Text を unbounded で描き、Box の中央から
                // 上下対称にはみ出させることで、地図を押しのけず・見切れずに全行を表示する。
                Box(
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val sel = viewModel.selectedSpot
                    if (viewModel.isStopping && sel != null && !viewModel.showResultModal) {
                        Text(
                            localizeData(sel.name),
                            fontSize = 32.sp,
                            // 2〜3 行になったとき行同士が重ならないよう行間を明示する。
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = sel.typeMeta.color,
                            // 名前が長くて 2 行になっても中央寄せを保つ。
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(unbounded = true)
                                .padding(horizontal = 24.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // リスト表示中に、停止中の選択名を中央にオーバーレイ。
        if (viewModel.isListView && viewModel.isStopping && !viewModel.showResultModal) {
            viewModel.selectedSpot?.let { sel ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        localizeData(sel.name),
                        fontSize = 32.sp,
                        // 2〜3 行になったとき行同士が重ならないよう行間を明示する。
                        lineHeight = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = sel.typeMeta.color,
                        // 名前が長くて 2 行になっても中央寄せを保つ。
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xF2FFFFFF))
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
        }

        // 結果モーダル。
        if (viewModel.showResultModal) {
            SpotResultOverlay(
                spot = viewModel.selectedSpot,
                onReset = {
                    // 結果画面から次へ進むタイミングで iOS 版と同じ発火判定を行う
                    // （ポイント加算＋広告／レビュー／Play レビューの試行）。
                    InterstitialAdManager.registerRouletteSpin(context)
                    viewModel.reset()
                },
                onShowDetail = { viewModel.selectedSpot?.let(onOpenSpot) },
            )
        }

        // Start / Stop ボタン（停止シーケンス中・結果表示中は隠す）。
        if (!viewModel.isStopping && !viewModel.showResultModal) {
            SpinButton(
                isSpinning = viewModel.isSpinning,
                enabled = viewModel.canSpin,
                onClick = { if (viewModel.isSpinning) viewModel.stopSpin() else viewModel.startSpin() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 40.dp),
            )
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState,
            containerColor = AppTheme.Background,
        ) {
            SpotSettingsScreen(
                viewModel = viewModel,
                onDone = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) showSettings = false
                    }
                },
            )
        }
    }
}

/** 白い角丸ピルボタン（設定 / 表示切替）。iOS のヘッダーボタンに対応。 */
@Composable
private fun PillButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .appCard(corner = 20.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Black)
    }
}

/** アイコンのみの白い角丸ボタン（地図/リスト切替）。iOS の切替ボタンに対応。 */
@Composable
private fun IconPillButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .appCard(corner = 20.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.Black, modifier = Modifier.size(18.dp))
    }
}

/** 現在表示中のタイプを小さくチップ表示（自然タブ）。iOS の selectedDisplayTypes 表示に対応。 */
@Composable
private fun DisplayTypeChips(types: List<SpotTypeMeta>, modifier: Modifier = Modifier) {
    if (types.isEmpty()) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        types.forEach { meta ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(meta.color.copy(alpha = 0.12f))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(meta.icon, contentDescription = null, tint = meta.color, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(2.dp))
                // デフォルトの行の余白でテキストが下寄りに見えるのを防ぎ、
                // アイコンと縦中央で揃えるため、フォントパディングを外し行を中央トリムする。
                Text(
                    localizeTypeLabel(meta.name),
                    fontSize = 10.sp,
                    color = meta.color,
                    style = androidx.compose.ui.text.TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both,
                        ),
                    ),
                )
            }
        }
    }
}

/** スポットマーカーを載せた県ポリゴン地図。iOS の mapView に対応。 */
@Composable
private fun SpotMap(
    spots: List<RouletteSpot>,
    selected: RouletteSpot?,
    onTapSpot: (RouletteSpot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spotPrefectures = remember(spots) { spots.map { it.prefecture }.toSet() }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .padding(horizontal = 8.dp)
            .clipToBounds(),
    ) {
        val side = maxWidth
        fun mapPos(v: Float) = side * (v / Prefecture.MAP_SIZE)

        Canvas(modifier = Modifier.fillMaxSize()) {
            for (prefecture in Prefecture.entries) {
                drawPrefecturePolygon(prefecture, prefecture in spotPrefectures)
            }
        }

        spots.forEach { spot ->
            val pos = GeoProjection.project(spot.name, spot.latitude, spot.longitude, spot.prefecture)
            val isSelected = selected?.id == spot.id
            SpotMarker(
                // iOS: 温泉マーカーは全て温泉アイコン固定、自然はタイプ別アイコン。
                icon = if (spot.kind == SpotKind.ONSEN) Icons.Filled.Spa else spot.typeMeta.icon,
                color = spot.typeMeta.color,
                isSelected = isSelected,
                onClick = { onTapSpot(spot) },
                modifier = Modifier.offset(
                    x = mapPos(pos.x) - 18.dp,
                    y = mapPos(pos.y) - 18.dp,
                ),
            )
        }
    }
}

/** アイコンマーカー。選択中は拡大＋色を強調する（iOS の選択マーカー相当）。 */
@Composable
private fun SpotMarker(
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dotSize by animateDpAsState(if (isSelected) 30.dp else 18.dp, label = "dot")
    val bg by animateColorAsState(if (isSelected) Color(0xFFFF9500) else color, label = "bg")
    Box(
        modifier = modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .shadow(if (isSelected) 4.dp else 2.dp, CircleShape, spotColor = Color(0x40000000))
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(bg.copy(alpha = 0.85f), bg))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (isSelected) 16.dp else 10.dp),
            )
        }
    }
}

/** リスト表示（タイプ別グループのカードグリッド）。iOS の onsenListView / spotListView に対応。 */
@Composable
private fun SpotListView(
    viewModel: SpotRouletteViewModel,
    onOpenSpot: (RouletteSpot) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = remember(viewModel.visibleEnabledSpots) {
        SpotRepository.groupedByType(viewModel.kind, viewModel.visibleEnabledSpots)
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        sections.forEach { (meta, spots) ->
            item(key = "lsec_${meta.key}") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(meta.icon, contentDescription = null, tint = meta.color, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(localizeTypeLabel(meta.name), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = meta.color)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.spot_roulette_count, spots.size), fontSize = 12.sp, color = AppTheme.TextSecondary)
                }
            }
            // 2 列のカード。
            items(spots.chunked(2), key = { it.first().id }) { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { spot ->
                        SpotCard(
                            spot = spot,
                            isSelected = viewModel.selectedSpot?.id == spot.id,
                            onClick = { onOpenSpot(spot) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** リスト表示の 1 カード。iOS の OnsenCardView / NatureSpotCardView に対応。 */
@Composable
private fun SpotCard(
    spot: RouletteSpot,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val meta = spot.typeMeta
    Column(
        modifier = modifier
            .appCard(corner = 12.dp)
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(meta.color.copy(alpha = 0.85f), meta.color))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(meta.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            localizeData(spot.name),
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isSelected) Color(0xFFFF9500) else AppTheme.TextPrimary,
            maxLines = 2,
        )
        Text(spot.prefecture.localizedName(), fontSize = 10.sp, color = AppTheme.TextSecondary, modifier = Modifier.padding(top = 2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Text(localizeTypeLabel(meta.tagName), fontSize = 9.sp, color = meta.color)
            Spacer(modifier = Modifier.width(4.dp))
            repeat(spot.popularity) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(7.dp))
            }
        }
    }
}

@Composable
private fun SpinButton(
    isSpinning: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(6.dp, CircleShape, spotColor = Color(0x22000000))
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, AppTheme.Hairline, CircleShape)
            .then(if (enabled) Modifier else Modifier.background(Color(0x80FFFFFF)))
            .clickableIf(enabled, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isSpinning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = stringResource(if (isSpinning) R.string.common_stop else R.string.common_start),
            tint = if (enabled) Color.Black else Color.Gray,
            modifier = Modifier.size(28.dp),
        )
    }
}

/** 結果モーダル。iOS の showResultModal に対応。 */
@Composable
private fun SpotResultOverlay(
    spot: RouletteSpot?,
    onReset: () -> Unit,
    onShowDetail: () -> Unit,
) {
    val accent = spot?.typeMeta?.color ?: Color(0xFFFF9500)
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xB3FFFFFF)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                spot?.name?.let { localizeData(it) } ?: "",
                fontSize = 40.sp,
                // 2 行になったとき行同士が重ならないよう行間を明示する。
                lineHeight = 46.sp,
                fontWeight = FontWeight.Black,
                color = accent,
                // 名前が長くて 2 行になっても中央寄せを保つ。
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
            spot?.let {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                    Icon(it.typeMeta.icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(localizeTypeLabel(it.typeMeta.name), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = accent)
                }
                Text(
                    it.prefecture.localizedName(),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    localizeData(it.description),
                    fontSize = 13.sp,
                    color = AppTheme.TextSecondary,
                    modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
                )
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    repeat(it.popularity) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(14.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.7f))))
                    .clickable(onClick = onShowDetail)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.spot_roulette_detail_info), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 30.dp, bottom = 40.dp)
                .size(50.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xB34285F4), Color(0xB39C27B0))))
                .clickableIf(true, onReset),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.common_retry), tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

/** Canvas 用: 県ポリゴンを描く（スポットのある県は強調）。 */
private fun DrawScope.drawPrefecturePolygon(prefecture: Prefecture, highlighted: Boolean) {
    fun norm(p: Offset) = Offset(
        x = p.x / Prefecture.MAP_SIZE * size.width,
        y = p.y / Prefecture.MAP_SIZE * size.height,
    )

    val pts = prefecture.points
    if (pts.size >= 3) {
        val path = Path()
        val first = norm(pts.first())
        path.moveTo(first.x, first.y)
        for (p in pts.drop(1)) {
            val n = norm(p)
            path.lineTo(n.x, n.y)
        }
        path.close()

        drawPath(
            path = path,
            brush = Brush.linearGradient(listOf(MapFillStart, MapFillEnd)),
            alpha = if (highlighted) 1f else 0.25f,
        )
        if (highlighted) drawPath(path = path, color = Color(0x40FF9500))
        drawPath(
            path = path,
            color = MapStroke,
            alpha = if (highlighted) 1f else 0.4f,
            style = Stroke(width = 0.6.dp.toPx()),
        )
    }

    if (prefecture == Prefecture.OKINAWA) {
        val line = prefecture.okinawaLinePoints
        if (line.size >= 2) {
            val path = Path()
            val first = norm(line.first())
            path.moveTo(first.x, first.y)
            for (p in line.drop(1)) {
                val n = norm(p)
                path.lineTo(n.x, n.y)
            }
            drawPath(
                path = path,
                color = Color.Gray,
                alpha = 0.5f,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f)),
                ),
            )
        }
    }
}
