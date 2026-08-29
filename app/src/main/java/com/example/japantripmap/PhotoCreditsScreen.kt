package com.example.japantripmap

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler

/**
 * 観光スポット写真のフォトクレジット画面。
 *
 * iOS 版は OS の設定アプリ（Settings.bundle/PhotoCredits.plist）にクレジットを
 * 表示するが、Android には相当する仕組みが無いため、アプリ内で全文表示する。
 * CC BY ライセンス作品の帰属表示（法的義務）を満たすためのもの。
 *
 * 文言は [PHOTO_CREDITS_TEXT]（iOS の plist をそのまま移植）を等幅風に流し込むだけ。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCreditsScreen(onBack: () -> Unit) {
    // 端末の戻るジェスチャ／ボタンでも一覧へ戻す。
    BackHandler(onBack = onBack)

    Scaffold(
        containerColor = AppTheme.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.photo_credits_title), fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.TopBar,
                    scrolledContainerColor = AppTheme.TopBar,
                ),
                modifier = Modifier.drawBottomHairline(),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 見出しカード（用途の説明）。
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appCard(corner = 16.dp)
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = AppTheme.TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.photo_credits_attribution), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppTheme.TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.photo_credits_desc),
                        fontSize = 12.sp,
                        color = AppTheme.TextSecondary,
                    )
                }
            }

            // クレジット本文（iOS の plist をそのまま流し込む）。
            item {
                Text(
                    text = PHOTO_CREDITS_TEXT,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = AppTheme.TextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
