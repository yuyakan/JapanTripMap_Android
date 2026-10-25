package com.example.japantripmap

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

// MARK: - 接続状態

/**
 * 端末がネットワークに繋がっているかの監視。iOS 版 NetworkMonitor。
 * 埋め込みプレイヤーはオフラインでも黙って黒のままになるため、開く前・開いている最中に案内を出す。
 * 初期値は true（最初の評価前に「オフライン」と誤表示しないため）。
 */
@Composable
fun rememberIsConnected(): Boolean {
    val context = LocalContext.current
    var connected by remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connected = cm.activeNetwork != null
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { connected = true }
            override fun onLost(network: Network) { connected = cm.activeNetwork != null && cm.activeNetwork != network }
        }
        runCatching { cm.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }
    return connected
}

// MARK: - カテゴリ名

/** トレンドのカテゴリ名。Firestore の日本語固定値ではなく categoryKey からローカライズする。 */
@Composable
fun trendCategoryName(group: TrendGroup): String = when (group.categoryKey) {
    "cafe" -> stringResource(R.string.home_trend_category_cafe)
    "spot" -> stringResource(R.string.home_trend_category_spot)
    "gourmet" -> stringResource(R.string.home_trend_category_gourmet)
    else -> group.category
}

/** 「◯◯県の人気スポット」形式の見出し。 */
@Composable
fun trendGroupTitle(group: TrendGroup): String =
    stringResource(R.string.home_trend_group_title, group.prefecture?.localizedName() ?: group.prefName, trendCategoryName(group))

/** サムネを先読みしておく（スクロールで主役の県が変わった瞬間に読み込み待ちを出さない）。 */
fun prefetchThumbnails(context: Context, urls: List<String>) {
    val loader = context.imageLoader
    urls.forEach { loader.enqueue(ImageRequest.Builder(context).data(it).build()) }
}

// MARK: - トレンドカード

/**
 * トレンド 1 本の縦型サムネカード（9:16）。iOS 版 TrendCard。
 * 要件D: チャンネル名を必ず表示して出典を明示する（画面全体の出典は Developed with YouTube ロゴ）。
 */
@Composable
fun TrendCard(video: TrendVideo, onClick: () -> Unit) {
    val width = 132.dp
    Column(
        modifier = Modifier
            .width(width)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF2F2F7)),
        ) {
            Icon(
                Icons.Outlined.SmartDisplay,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.align(Alignment.Center).size(28.dp),
            )
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                video.durationText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
        Text(
            video.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp,
            modifier = Modifier.height(32.dp),
        )
        Text(
            video.channelTitle,
            fontSize = 10.sp,
            color = AppTheme.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * トレンド動画の横スクロール行。タップでその行の動画を頭から連続再生する（[ShortsFeedScreen]）。
 * @param horizontalPadding 行の左右端インセット（見出しと縦を揃える）。
 */
@Composable
fun TrendScrollRow(
    group: TrendGroup,
    horizontalPadding: Int = 20,
    modifier: Modifier = Modifier,
    onOpen: (List<TrendVideo>, Int) -> Unit,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = horizontalPadding.dp, vertical = 8.dp),
    ) {
        itemsIndexed(group.videos, key = { _, v -> v.videoId }) { index, video ->
            TrendCard(video = video, onClick = { onOpen(group.videos, index) })
        }
    }
}

/**
 * 公式「Developed with YouTube」ロゴ（要件D）。タップで YouTube へ（リンク必須要件）。
 * 単色ロゴなので前景色を場面に合わせて着色する。
 */
@Composable
fun DevelopedWithYouTubeBadge(height: Int = 22, tint: Color = AppTheme.TextSecondary) {
    val context = LocalContext.current
    Image(
        painter = painterResource(R.drawable.developed_with_youtube),
        contentDescription = stringResource(R.string.home_trend_source_developed),
        colorFilter = ColorFilter.tint(tint),
        modifier = Modifier
            .height(height.dp)
            .aspectRatio(512f / 77f)
            .clickable {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")))
                }
            },
    )
}

/**
 * 親の左右余白（[amount]）を打ち消して画面端まで広げる。県詳細の動画行を
 * 「見出しは内側・スクロールは端いっぱい」にするため（iOS の負の padding と同じ）。
 */
fun Modifier.bleedHorizontally(amount: androidx.compose.ui.unit.Dp): Modifier = layout { measurable, constraints ->
    val extra = amount.roundToPx() * 2
    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra, minWidth = constraints.minWidth + extra))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-amount.roundToPx(), 0) }
}

/** Shorts フィードを開くための指定（動画配列＋開始位置）。 */
data class ShortsFeedRequest(val videos: List<TrendVideo>, val startIndex: Int)

// MARK: - Shorts フィード（全画面）

/**
 * Shorts を縦スワイプで次々に再生する全画面フィード。iOS 版 ShortsFeedView。
 *
 * 規約対応:
 * - 要件B: 再生は公式 IFrame Player の埋め込みのみ。DL 手段は作らない。プレイヤー UI は隠さない。
 * - 要件C: この再生画面には広告を置かない。
 * - 要件D: 各ページにタイトル・チャンネル名を表示し、出典はプレイヤー自体の YouTube ブランドに委ねる。
 */
@Composable
fun ShortsFeedScreen(request: ShortsFeedRequest, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val videos = request.videos
    val pagerState = rememberPagerState(initialPage = request.startIndex) { videos.size }
    val connected = rememberIsConnected()
    var showOffline by remember { mutableStateOf(false) }
    LaunchedEffect(connected) { if (!connected) showOffline = true }

    // プレイヤーを実体化しておくページ（前後 PRELOAD_RADIUS 本）。一度作ったページは
    // EVICT_RADIUS の外に出るまで生かし、往復スワイプで作り直さない（iOS と同じヒステリシス）。
    var materialized by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { current ->
            val next = materialized.toMutableSet()
            videos.forEachIndexed { i, v ->
                val d = kotlin.math.abs(i - current)
                if (d <= PRELOAD_RADIUS) next.add(v.videoId) else if (d > EVICT_RADIUS) next.remove(v.videoId)
            }
            materialized = next
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            key = { it },
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val video = videos[page]
            val active = page == pagerState.currentPage && !pagerState.isScrollInProgress ||
                page == pagerState.settledPage && page == pagerState.currentPage
            ShortsPage(
                video = video,
                isActive = active,
                isPreloaded = video.videoId in materialized ||
                    kotlin.math.abs(page - pagerState.currentPage) <= PRELOAD_RADIUS,
            )
        }

        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_close), tint = Color.White)
        }
    }

    if (showOffline) {
        AlertDialog(
            onDismissRequest = { showOffline = false; onClose() },
            title = { Text(stringResource(R.string.shorts_offline_title)) },
            text = { Text(stringResource(R.string.shorts_offline_message)) },
            confirmButton = {
                TextButton(onClick = { showOffline = false; onClose() }) { Text(stringResource(R.string.common_ok)) }
            },
        )
    }
}

private const val PRELOAD_RADIUS = 2
private const val EVICT_RADIUS = 4

/** 1 本分のページ。プレイヤー（または未生成時のサムネ）＋下部の出典オーバーレイ。 */
@Composable
private fun ShortsPage(video: TrendVideo, isActive: Boolean, isPreloaded: Boolean) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (isPreloaded) {
            ShortPlayer(
                videoId = video.videoId,
                isActive = isActive,
                thumbnailUrl = video.thumbnailUrl,
                modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f),
            )
        } else {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))))
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                video.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(video.channelTitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 1)
            }
        }
    }
}

/**
 * 公式 IFrame Player を 9:16 で表示するラッパー。iOS 版 YouTubeShortPlayerView。
 * - 表示中（isActive）のページだけ再生し、外れたら一時停止する（音が残らないように）。
 * - ready 前の play は捨てられるので、ready・状態変化のたびに「前面なのに止まっていないか」を照合して打ち直す。
 * - 実際に再生が始まるまではサムネを重ね、黒画面待ちを見せない。
 * - 失敗したページはサムネ止まりにする（プレイヤーの上に自前のエラー表示は重ねない＝要件B）。
 */
@Composable
private fun ShortPlayer(videoId: String, isActive: Boolean, thumbnailUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    var started by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    // リスナー内から最新の isActive を読むための箱。
    val activeRef = remember { booleanArrayOf(isActive) }
    activeRef[0] = isActive

    val playerView = remember {
        YouTubePlayerView(context).apply {
            enableAutomaticInitialization = false
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            matchParent()
            val options = IFramePlayerOptions.Builder(context)
                .controls(1) // 要件B: プレイヤー UI を隠さない
                .rel(0) // 関連動画を同じチャンネルに限定（iOS restrictRelatedVideosToSameChannel）
                .ccLoadPolicy(0) // 字幕を既定で強制表示しない
                .ivLoadPolicy(3)
                .build()
            initialize(
                object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        player = youTubePlayer
                        failed = false
                        if (activeRef[0]) youTubePlayer.loadVideo(videoId, 0f) else youTubePlayer.cueVideo(videoId, 0f)
                    }

                    override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                        when (state) {
                            PlayerConstants.PlayerState.PLAYING, PlayerConstants.PlayerState.BUFFERING -> started = true
                            PlayerConstants.PlayerState.ENDED -> {
                                // iOS の loopEnabled 相当。単一動画を先頭から繰り返す。
                                if (activeRef[0]) {
                                    youTubePlayer.seekTo(0f)
                                    youTubePlayer.play()
                                }
                            }
                            PlayerConstants.PlayerState.VIDEO_CUED, PlayerConstants.PlayerState.PAUSED,
                            PlayerConstants.PlayerState.UNSTARTED -> {
                                // 前面なのに止まっていたら打ち直す（自動再生ブロック・スワイプ直後の取りこぼし対策）。
                                if (activeRef[0] && !failed) youTubePlayer.play()
                            }
                            else -> Unit
                        }
                    }

                    override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                        failed = true
                        started = false
                    }
                },
                options,
            )
        }
    }

    DisposableEffect(playerView) {
        lifecycleOwner.lifecycle.addObserver(playerView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(playerView)
            playerView.release()
        }
    }

    LaunchedEffect(isActive, player) {
        val p = player ?: return@LaunchedEffect
        if (isActive) {
            if (!failed) p.play()
        } else {
            p.pause()
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { playerView }, modifier = Modifier.fillMaxSize())
        if (!started) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().background(Color.Black),
            )
        }
    }
}
