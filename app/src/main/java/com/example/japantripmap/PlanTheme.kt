package com.example.japantripmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * マイプラン機能の共通デザインテーマ。iOS 版 PlanTheme を移植。
 * ブランドカラー（オレンジ→コーラル）と共通スタイルを一元管理し、
 * プラン一覧・詳細・「プランに追加」シートの見た目を揃える。
 */
internal object PlanTheme {
    /** メインのオレンジ（iOS: selectedColor2 #FF5100 相当）。 */
    val Primary = Color(0xFFFF5100)

    /** アクセントのコーラル（iOS: selectedColor #FF5F73 相当）。 */
    val Accent = Color(0xFFFF5F73)

    /** ブランドの基調グラデーション（オレンジ→コーラル）。 */
    val brandGradient = Brush.linearGradient(colors = listOf(Primary, Accent))

    /** やわらかい背景グラデーション。 */
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Primary.copy(alpha = 0.06f),
            Accent.copy(alpha = 0.04f),
            Color(0xFFF7F7FA),
        ),
    )

    /** シート・カード内で二次的に使う淡い面色（入力欄・行の地）。 */
    val Surface2 = Color(0xFFF3F3F6)

    val cardCorner = 20.dp
}

/** カテゴリ -> 色。iOS 版 PlanTheme.color(for:) を移植。 */
internal fun planCategoryColor(c: PlanItemCategory): Color = when (c) {
    PlanItemCategory.ATTRACTION -> Color(0xFF4C8CFA)
    PlanItemCategory.GOURMET -> Color(0xFFF27326)
    PlanItemCategory.ONSEN -> Color(0xFFF25973)
    PlanItemCategory.FESTIVAL -> Color(0xFFB366F2)
    PlanItemCategory.NATURE -> Color(0xFF40B373)
    PlanItemCategory.SOUVENIR -> Color(0xFFE68C33)
    PlanItemCategory.HOTEL -> Color(0xFF7380D9)
    PlanItemCategory.TRANSPORT -> Color(0xFF4CA6B3)
    PlanItemCategory.OTHER -> Color(0xFF8C8C99)
}

/** カテゴリ -> アイコン。iOS 版 PlanItemCategory.icon（SF Symbol）を Material で近似。 */
internal fun planCategoryIcon(c: PlanItemCategory): ImageVector = when (c) {
    PlanItemCategory.ATTRACTION -> Icons.Filled.Place
    PlanItemCategory.GOURMET -> Icons.Filled.Restaurant
    PlanItemCategory.ONSEN -> Icons.Filled.Spa
    PlanItemCategory.FESTIVAL -> Icons.Filled.Celebration
    PlanItemCategory.NATURE -> Icons.Filled.Forest
    PlanItemCategory.SOUVENIR -> Icons.Filled.CardGiftcard
    PlanItemCategory.HOTEL -> Icons.Filled.Hotel
    PlanItemCategory.TRANSPORT -> Icons.Filled.DirectionsTransit
    PlanItemCategory.OTHER -> Icons.Filled.EditNote
}

/**
 * ブランドグラデーションの主要ボタン。iOS 版 PlanPrimaryButtonStyle を移植。
 * enabled=false のときは淡くして押下を無効化する。
 */
@Composable
internal fun PlanPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .then(if (enabled) Modifier.alpha(1f) else Modifier.alpha(0.5f))
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = PlanTheme.Primary.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(16.dp))
            .background(PlanTheme.brandGradient)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

/** 円形のカテゴリアイコンアバター（プラン項目の頭に付ける丸）。 */
@Composable
internal fun CategoryAvatar(category: PlanItemCategory, size: Int = 40, iconSize: Int = 20) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(planCategoryColor(category), androidx.compose.foundation.shape.CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            planCategoryIcon(category),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize.dp),
        )
    }
}
