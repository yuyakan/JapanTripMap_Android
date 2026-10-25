package com.example.japantripmap

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.window.DialogProperties
import java.text.NumberFormat
import java.util.Locale

/**
 * iOS のシート／fullScreenCover のように、下タブも含めた画面全体を覆って表示する。
 * 下にある画面はそのまま残る（状態を保ったまま上に重ねる）。戻る操作で [onDismiss]。
 */
@Composable
fun FullScreenSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(PlanTheme.pageBackground)) { content() }
    }
}

/**
 * 子を左詰めで並べ、幅が足りなくなったら折り返す簡易レイアウト。iOS 版 FlowLayout。
 * （Compose の FlowRow は実験的 API で依存ライブラリ間のバイナリ互換が崩れることがあるため自前で持つ）
 */
@Composable
fun WrapRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val h = horizontalSpacing.roundToPx()
        val v = verticalSpacing.roundToPx()
        val maxWidth = constraints.maxWidth
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val positions = ArrayList<IntArray>()
        var x = 0
        var y = 0
        var rowHeight = 0
        placeables.forEach { p ->
            if (x > 0 && x + p.width > maxWidth) {
                x = 0
                y += rowHeight + v
                rowHeight = 0
            }
            positions += intArrayOf(x, y)
            x += p.width + h
            rowHeight = maxOf(rowHeight, p.height)
        }
        val width = if (constraints.hasBoundedWidth) maxWidth else (placeables.maxOfOrNull { it.width } ?: 0)
        layout(width.coerceAtLeast(constraints.minWidth), (y + rowHeight).coerceAtLeast(constraints.minHeight)) {
            placeables.forEachIndexed { i, p -> p.place(positions[i][0], positions[i][1]) }
        }
    }
}

/** 破線の角丸枠（iOS の StrokeStyle(dash:) 相当）。追加ボタン・空ブロックの受け皿に使う。 */
fun Modifier.dashedBorder(color: Color, corner: Dp, width: Dp = 1.dp, dash: Dp = 4.dp): Modifier = drawBehind {
    val stroke = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(corner.toPx()),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx()))),
    )
}

/** マイプラン系のカード（iOS planCard: 白地・角丸 20・やわらかい影）。 */
fun Modifier.planCard(padding: Dp = 16.dp): Modifier = this
    .shadow(6.dp, RoundedCornerShape(PlanTheme.cardCorner), spotColor = PlanTheme.cardShadow, ambientColor = PlanTheme.cardShadow)
    .clip(RoundedCornerShape(PlanTheme.cardCorner))
    .background(PlanTheme.groupedSurface)
    .padding(padding)

/**
 * 文字＋ブランドグラデ下線の切替帯（マイプランの一覧/訪問済み、プラン詳細の旅程/費用）。
 * iOS 版 PlanSectionBand / PlanDetailSectionBand。統合マップの切替帯と同じデザイン言語。
 */
@Composable
fun PlanSectionBand(titles: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(44.dp)) {
        val inset = 24.dp
        val cellWidth = maxWidth / titles.size
        val offset by animateDpAsState(
            targetValue = cellWidth * selectedIndex + inset,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
            label = "planBandUnderline",
        )
        Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp, bottom = 4.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                titles.forEachIndexed { i, title ->
                    val selected = i == selectedIndex
                    Text(
                        title,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) PlanTheme.Primary else Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                    )
                }
            }
            Box(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .offset(x = offset)
                    .width(cellWidth - inset * 2)
                    .height(2.dp)
                    .background(PlanTheme.brandGradient),
            )
        }
    }
}

/** 右下に積むブランドの円形 FAB（地図・スポット追加・地方別集計）。 */
@Composable
fun PlanFab(icon: ImageVector, contentDescription: String, size: Int = 56, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .shadow(10.dp, CircleShape, spotColor = PlanTheme.Primary.copy(alpha = 0.4f))
            .clip(CircleShape)
            .background(PlanTheme.brandGradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size((size * 0.4f).dp))
    }
}

/** 破線枠のテキストボタン（「＋ ホテル・駅を追加」「＋ 新規作成」など）。 */
@Composable
fun DashedAddButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, corner: Dp = 12.dp, verticalPadding: Dp = 12.dp) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .dashedBorder(PlanTheme.Primary.copy(alpha = 0.4f), corner)
            .clickable(onClick = onClick)
            .padding(vertical = verticalPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
    }
}

/**
 * 金額（円・整数）を端末ロケールの通貨で整形する。iOS 版 CurrencyFormat と同じく
 * 保存は素の整数で、表示だけ通貨記号を付ける（小数は出さない）。
 */
object CurrencyFormat {
    private val formatter: NumberFormat
        get() = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }

    fun string(value: Int): String = runCatching { formatter.format(value) }.getOrDefault("$symbol$value")

    val symbol: String get() = runCatching { formatter.currency?.getSymbol(Locale.getDefault()) }.getOrNull() ?: "¥"
}
