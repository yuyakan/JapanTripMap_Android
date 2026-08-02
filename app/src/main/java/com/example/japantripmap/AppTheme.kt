package com.example.japantripmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * アプリ全体で共有するデザイントークン。
 *
 * 「背景は白・カードは白＋細い枠線＋弱い影」というモダンで統一感のあるトーンを
 * 一元管理する。各画面はここを参照して見た目を揃える。
 */
object AppTheme {
    /** 画面全体の背景（白）。 */
    val Background = Color.White

    /** トップバー背景（白で統一）。 */
    val TopBar = Color.White

    /** カード地の色（白）。 */
    val CardSurface = Color.White

    /** カード・区切りに使う細い枠線色（ごく薄いグレー）。 */
    val Hairline = Color(0xFFECECEF)

    /** 本文・見出しの基本文字色。 */
    val TextPrimary = Color(0xFF1C1C1E)

    /** 補助テキスト（サブタイトル等）。 */
    val TextSecondary = Color(0xFF8A8A8E)

    /** カードの角丸。 */
    val CardCorner = 16.dp
}

/**
 * 統一カード装飾。白地＋細い枠線＋やわらかい影。角丸でクリップする。
 * `clickable` などを重ねたい場合は、この後にチェーンする。
 */
fun Modifier.appCard(corner: androidx.compose.ui.unit.Dp = AppTheme.CardCorner): Modifier =
    this
        .shadow(
            elevation = 3.dp,
            shape = RoundedCornerShape(corner),
            spotColor = Color(0x14000000),
            ambientColor = Color(0x0F000000),
        )
        .clip(RoundedCornerShape(corner))
        .background(AppTheme.CardSurface)
        .border(1.dp, AppTheme.Hairline, RoundedCornerShape(corner))

/** 上端に細い区切り線を描く（白いトップバー／ナビバーとコンテンツの境界に）。 */
fun Modifier.drawTopHairline(): Modifier =
    this.drawBehind {
        val h = 1.dp.toPx()
        drawRect(
            color = AppTheme.Hairline,
            topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(size.width, h),
        )
    }

/** 下端に細い区切り線を描く（白いトップバーの下境界に）。 */
fun Modifier.drawBottomHairline(): Modifier =
    this.drawBehind {
        val h = 1.dp.toPx()
        drawRect(
            color = AppTheme.Hairline,
            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - h),
            size = androidx.compose.ui.geometry.Size(size.width, h),
        )
    }
