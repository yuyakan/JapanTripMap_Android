package com.example.japantripmap

import androidx.annotation.StringRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 統合マップタブ内で切り替えるマップの種類。iOS 版 MapMode。 */
enum class MapMode(@param:StringRes val titleRes: Int) {
    PREFECTURE(R.string.tab_band_prefecture),
    ONSEN(R.string.tab_onsen_place),
    NATURE(R.string.tab_band_nature),
}

/** 切替帯の高さ（iOS mapModeBandHeight）。 */
private val MapModeBandHeight = 44.dp

/** 下線の左右インセット（セル幅からこの分だけ内側に縮める）。 */
private val UnderlineInset = 12.dp

/**
 * 都道府県・温泉・自然の 3 マップを 1 タブに統合するコンテナ。iOS 版 IntegratedMapView。
 * 上部の切替帯（MapModeBand）はこのコンテナが 1 つだけ持ち、マップを切り替えても作り直さないので
 * 下線がスライドアニメーションする。
 */
@Composable
fun MapTabScreen(
    mapMode: MapMode,
    onModeChange: (MapMode) -> Unit,
    content: @Composable (MapMode) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        MapModeBand(
            selected = mapMode,
            onSelect = onModeChange,
            modifier = Modifier.statusBarsPadding(),
        )
        Box(modifier = Modifier.weight(1f)) {
            content(mapMode)
        }
    }
}

/** 文字のみ・選択中はセル幅いっぱいのブランドグラデーション下線（常に 1 本をスライドさせる）。 */
@Composable
private fun MapModeBand(selected: MapMode, onSelect: (MapMode) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(MapModeBandHeight)
            .background(Color.White),
    ) {
        val count = MapMode.entries.size
        val cellWidth = maxWidth / count
        val underlineOffset by animateDpAsState(
            targetValue = cellWidth * selected.ordinal + UnderlineInset,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "mapModeUnderline",
        )
        Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp, bottom = 4.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                MapMode.entries.forEach { mode ->
                    val isSelected = mode == selected
                    Text(
                        stringResource(mode.titleRes),
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) PlanTheme.Primary else Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { if (!isSelected) onSelect(mode) },
                    )
                }
            }
            Box(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .offset(x = underlineOffset)
                    .width(cellWidth - UnderlineInset * 2)
                    .height(2.dp)
                    .background(PlanTheme.brandGradient)
                    .align(Alignment.Start),
            )
        }
    }
}
