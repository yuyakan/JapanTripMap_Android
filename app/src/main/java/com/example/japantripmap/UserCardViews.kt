package com.example.japantripmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/** セクション見出しの右側に置く「カードを追加」ボタン。iOS 版 AddUserCardButton を移植。 */
@Composable
fun AddUserCardButton(accent: Color, onClick: () -> Unit) {
    val description = stringResource(R.string.usercard_add)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(accent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(stringResource(R.string.usercard_add_short), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
    }
}

/**
 * セクションのグリッドに並べるカード。iOS 版 UserCardTile を移植。
 * 表紙写真があれば上半分に敷き、無ければセクションのアイコンを出す。
 * [height] は同じセクションの既存カードに合わせて渡す（既存カードと同じく最小の高さとして扱う）。
 */
@Composable
fun UserCardTile(card: UserCard, height: Int, onClick: () -> Unit) {
    val accent = card.kind.accent
    val photo = UserCardStore.photoFile(card)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = height.dp)
            .fillMaxHeight()
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height((height * 0.55f).dp),
            )
        }
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (photo == null) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .size(40.dp)
                        .background(accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(card.kind.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            Text(card.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(card.description, fontSize = 12.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
