package com.example.japantripmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 施設名で YouTube / Instagram / 食べログ / Google マップ を検索して開くボタン群。
 * iOS 版 SocialSearchButtons.swift を移植。
 *
 * @param query 検索キーワード（基本は施設名）。
 * @param tabelogArea 食べログ検索のエリアスラッグ（Prefecture.slug）。null なら食べログボタン非表示。
 * @param tabelogKeyword 食べログ検索の日本語キーワード（グルメ名）。
 * @param showMap Google マップボタンを出すか。
 */
@Composable
fun SocialSearchButtons(
    query: String,
    tabelogArea: String? = null,
    tabelogKeyword: String = query,
    showMap: Boolean = true,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (showMap) {
            SocialButton(stringResource(R.string.social_open_google_maps), Icons.Filled.Map, Color(0xFF34A853)) {
                ExternalLinks.openMap(context, query)
            }
        }
        if (tabelogArea != null) {
            SocialButton(stringResource(R.string.social_tabelog), Icons.Filled.Restaurant, Color(0xFF2E9E8C)) {
                ExternalLinks.openTabelog(context, tabelogArea, tabelogKeyword)
            }
        }
        SocialButton(stringResource(R.string.social_youtube), Icons.Filled.PlayArrow, Color(0xFFFF0000)) {
            ExternalLinks.openYouTube(context, query)
        }
        SocialButton(stringResource(R.string.social_instagram), Icons.Filled.CameraAlt, Color(0xFFE1306C)) {
            ExternalLinks.openInstagram(context, query)
        }
    }
}

@Composable
private fun SocialButton(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color)
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
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp),
        )
    }
}
