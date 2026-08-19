package com.example.japantripmap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/** ルーレットの配色（iOS 版の雰囲気に合わせた緑系＋選択時オレンジ）。 */
private val SeaColor = Color.White
private val PrefectureFillStart = Color(0x6633C481) // green 0.4
private val PrefectureFillEnd = Color(0x8014B8A6)   // teal 0.5
private val SelectedColor = Color(0xFFFF9500)
private val SelectedShadow = Color(0xCC000000)
private val StrokeColor = Color(0x66FFFFFF)
private val AccentBlue = Color(0xFF4285F4)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JapanMapScreen(
    modifier: Modifier = Modifier,
    viewModel: RouletteViewModel = viewModel(),
    onOpenTourism: (Prefecture) -> Unit = {},
) {
    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // スピン中は設定を触れないようにする（iOS 版と同じ）。
    val isBusy = viewModel.isSpinning || viewModel.isStopping

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SeaColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 上部バー：設定ボタン（左）＋対象県数（右）。結果表示中・スピン中は中身を隠すが、
            // 常にヘッダーと同じ高さを確保して地図の縦位置がスピン開始で動かないようにする。
            // 高さは実測して placeholder に反映するので、内容が見切れることはない。
            val headerContentVisible = !isBusy && !viewModel.showResultModal
            var headerHeight by remember { mutableStateOf(0.dp) }
            val density = androidx.compose.ui.platform.LocalDensity.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (headerHeight > 0.dp) Modifier.height(headerHeight) else Modifier),
            ) {
                if (headerContentVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged {
                                with(density) { headerHeight = it.height.toDp() }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SettingsChip(onClick = { showSettings = true })
                        Spacer(modifier = Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.map_target_prefectures, viewModel.effectiveEnabled.size),
                                fontSize = 13.sp,
                                color = Color.Gray,
                            )
                            if (viewModel.hasCustomWeights) {
                                Text(
                                    text = stringResource(R.string.map_weighting_enabled),
                                    fontSize = 11.sp,
                                    color = AccentBlue,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .background(AccentBlue.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 6.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }
                }
            }

            // 温泉・自然タブが常に確保しているタイプチップ行（22dp）と同じ高さを都道府県でも確保し、
            // ヘッダー高さを揃えて 3 タブで地図の縦位置を一致させる。スピン中も高さは維持する。
            Spacer(modifier = Modifier.height(22.dp))

            // 地図＋県名を縦方向中央に寄せる。
            Spacer(modifier = Modifier.weight(1f))

            JapanMap(
                selected = viewModel.selectedPrefecture,
                enabled = viewModel.effectiveEnabled,
                available = viewModel.availablePrefectures.toSet(),
                onPrefectureTap = { pref ->
                    // スピン中・停止中・結果表示中はタップ無効（iOS 版と同じ）。
                    if (!isBusy && !viewModel.showResultModal) onOpenTourism(pref)
                },
                // 温泉・自然タブの SpotMap と同じサイズ指定（横 8dp パディング＋clipToBounds）に
                // 揃えて、3 タブで日本地図の描画位置・大きさを一致させる。
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 8.dp)
                    .clipToBounds(),
            )

            // スピン中／停止中に選択県名を大きく表示。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                val name = viewModel.selectedPrefecture?.localizedName()
                if (viewModel.isStopping && name != null && !viewModel.showResultModal) {
                    Text(
                        text = name,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = SelectedColor,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // 結果モーダル。
        if (viewModel.showResultModal) {
            ResultOverlay(
                prefecture = viewModel.selectedPrefecture,
                mode = viewModel.mode,
                onReset = {
                    // 結果画面から次へ進むタイミングで完走を記録し、3 回ごとにレビューを促す。
                    scope.launch { AppReviewManager.onRouletteCompleted(context) }
                    viewModel.reset()
                },
                onShowTourism = { viewModel.selectedPrefecture?.let(onOpenTourism) },
            )
        }

        // Start / Stop ボタン（結果表示中・停止シーケンス中は隠す）。
        if (!viewModel.isStopping && !viewModel.showResultModal) {
            SpinButton(
                isSpinning = viewModel.isSpinning,
                enabled = viewModel.canSpin,
                onClick = {
                    if (viewModel.isSpinning) viewModel.stopSpin() else viewModel.startSpin()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 30.dp, bottom = 40.dp),
            )
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = sheetState,
            containerColor = AppTheme.Background,
        ) {
            SettingsScreen(
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

@Composable
private fun SettingsChip(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .appCard(corner = 20.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = stringResource(R.string.common_settings), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Black)
    }
}

@Composable
private fun JapanMap(
    selected: Prefecture?,
    enabled: Set<Prefecture>,
    available: Set<Prefecture> = Prefecture.entries.toSet(),
    onPrefectureTap: (Prefecture) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // タップ判定に使う実際の Canvas サイズ（px）。描画時に更新する。
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Canvas(
        modifier = modifier.pointerInput(available) {
            detectTapGestures { offset ->
                if (canvasSize.width <= 0f || canvasSize.height <= 0f) return@detectTapGestures
                // タップ座標を 500x500 座標系に逆正規化。
                val px = offset.x / canvasSize.width * Prefecture.MAP_SIZE
                val py = offset.y / canvasSize.height * Prefecture.MAP_SIZE
                // 母集団内（表示されている）県のうち、ポリゴンに含まれる最初の県へ。
                val hit = Prefecture.entries.firstOrNull { pref ->
                    pref in available && pointInPolygon(px, py, pref.points)
                }
                if (hit != null) onPrefectureTap(hit)
            }
        },
    ) {
        canvasSize = size
        // 未選択の県を先に描く。
        for (prefecture in Prefecture.entries) {
            if (prefecture == selected) continue
            // 母集団外（例: 温泉モードで温泉なし）は非常に薄く、対象外は薄く、対象は通常。
            val alpha = when {
                prefecture !in available -> 0.1f
                prefecture !in enabled -> 0.3f
                else -> 1f
            }
            drawPrefecture(prefecture, alpha)
            if (prefecture == Prefecture.OKINAWA) drawOkinawaLine(prefecture)
        }
        // 選択県は最後に前面へ。影付きで強調する。
        selected?.let { drawSelectedPrefecture(it) }
    }
}

/**
 * 点 (px, py) がポリゴン内にあるか（レイキャスティング法）。
 * 座標は 500x500 系。
 */
private fun pointInPolygon(px: Float, py: Float, polygon: List<Offset>): Boolean {
    if (polygon.size < 3) return false
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val xi = polygon[i].x
        val yi = polygon[i].y
        val xj = polygon[j].x
        val yj = polygon[j].y
        if (((yi > py) != (yj > py)) &&
            (px < (xj - xi) * (py - yi) / (yj - yi) + xi)
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

/** 500x500 座標系の点を Canvas の実サイズに正規化する。 */
private fun DrawScope.normalize(point: Offset): Offset = Offset(
    x = point.x / Prefecture.MAP_SIZE * size.width,
    y = point.y / Prefecture.MAP_SIZE * size.height,
)

/** 点列から閉じたパスを作る。 */
private fun DrawScope.buildPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    val first = normalize(points.first())
    path.moveTo(first.x, first.y)
    for (p in points.drop(1)) {
        val n = normalize(p)
        path.lineTo(n.x, n.y)
    }
    path.close()
    return path
}

private fun DrawScope.drawPrefecture(prefecture: Prefecture, alpha: Float) {
    val path = buildPath(prefecture.points)
    drawPath(
        path = path,
        brush = Brush.linearGradient(listOf(PrefectureFillStart, PrefectureFillEnd)),
        alpha = alpha,
    )
    drawPath(
        path = path,
        color = StrokeColor,
        alpha = alpha,
        style = Stroke(width = 0.6.dp.toPx()),
    )
}

private fun DrawScope.drawSelectedPrefecture(prefecture: Prefecture) {
    // 影（少しずらして描く）。
    val shadowPath = Path().apply {
        addPath(buildPath(prefecture.points), Offset(4f, 4f))
    }
    drawPath(path = shadowPath, color = SelectedShadow, alpha = 0.8f)

    // 本体。
    val path = buildPath(prefecture.points)
    drawPath(path = path, color = SelectedColor, alpha = 0.9f)
    drawPath(
        path = path,
        color = Color.White,
        alpha = 0.8f,
        style = Stroke(width = 1f.dp.toPx()),
    )
}

private fun DrawScope.drawOkinawaLine(prefecture: Prefecture) {
    val points = prefecture.okinawaLinePoints
    if (points.size < 2) return
    val path = Path()
    val first = normalize(points.first())
    path.moveTo(first.x, first.y)
    for (p in points.drop(1)) {
        val n = normalize(p)
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

@Composable
private fun ResultOverlay(
    prefecture: Prefecture?,
    mode: RouletteMode,
    onReset: () -> Unit,
    onShowTourism: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3FFFFFF)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = prefecture?.localizedName() ?: "",
                fontSize = 46.sp,
                fontWeight = FontWeight.Black,
                color = SelectedColor,
            )
            prefecture?.let {
                Text(
                    text = it.localizedRegionName(),
                    fontSize = 18.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp),
                )
                // 温泉／自然モードでは件数を表示。
                val countText = when (mode) {
                    RouletteMode.ONSEN -> stringResource(R.string.map_onsen_count, it.onsens.size)
                    RouletteMode.NATURE -> stringResource(R.string.map_nature_count, it.natureSpots.size)
                    else -> null
                }
                if (countText != null) {
                    Text(
                        text = countText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SelectedColor,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            // 詳細ボタン（モードでラベルを切り替え）。
            Row(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(Brush.linearGradient(listOf(Color(0xE6FF9500), Color(0xE6FF3B30))))
                    .clickable(onClick = onShowTourism)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (mode) {
                        RouletteMode.ONSEN -> stringResource(R.string.map_result_onsen)
                        RouletteMode.NATURE -> stringResource(R.string.map_result_nature)
                        else -> stringResource(R.string.map_result_tourism)
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }

        // もう一度ボタン。
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 30.dp, bottom = 40.dp)
                .size(50.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xB34285F4), Color(0xB39C27B0)),
                    ),
                )
                .clickableIf(true, onReset),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.common_retry),
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
