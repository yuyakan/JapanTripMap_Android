package com.example.japantripmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.navigationBarsPadding

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 起動時の白い中間画面を消す。super.onCreate より前に呼ぶ必要がある。
        // iOS 版と同じく、システムスプラッシュ（ティール地＋アイコン）を約 2 秒
        // 保持してから本体へ遷移する。
        val splash = installSplashScreen()
        val start = System.currentTimeMillis()
        splash.setKeepOnScreenCondition {
            System.currentTimeMillis() - start < SPLASH_DURATION_MS
        }
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = AppTheme.Background) {
                    AppRoot()
                }
            }
        }
    }

    private companion object {
        /** スプラッシュを表示し続ける時間（iOS 版の 2 秒に合わせる）。 */
        const val SPLASH_DURATION_MS = 2000L
    }
}

/** ボトムタブの定義。iOS 版 ContentView の 5 タブに対応。 */
private enum class MainTab(val labelRes: Int, val icon: ImageVector) {
    PREFECTURE(R.string.tab_prefecture, Icons.Filled.Map),
    ONSEN(R.string.tab_onsen, Icons.Filled.Hotel),
    NATURE(R.string.tab_nature, Icons.Filled.Park),
    FESTIVAL(R.string.tab_festival, Icons.Filled.Celebration),
    PLAN(R.string.tab_plan, Icons.Filled.Luggage),
}

private val TabAccent = Color(0xFFFF9500)

/**
 * アプリのルート。5 タブ構成。
 * 観光詳細はタブの上に重ねて表示する（全画面）。
 */
/** 全画面で開く詳細のたぐい。 */
private sealed interface Detail {
    data class Tourism(val prefecture: Prefecture) : Detail
    data class Onsen(val prefecture: Prefecture) : Detail
    data class Nature(val prefecture: Prefecture) : Detail
}

@Composable
private fun AppRoot() {
    var selectedTab by remember { mutableStateOf(MainTab.PREFECTURE) }
    // 表示中の全画面詳細（県別の一覧詳細）。null ならタブ画面。
    var detail by remember { mutableStateOf<Detail?>(null) }
    // 個別スポット詳細（一覧詳細のさらに上に重ねる）。null なら非表示。
    var spotDetail by remember { mutableStateOf<SpotDetail?>(null) }

    // 都道府県ルーレット（全国モード）。
    val tourismViewModel: RouletteViewModel = viewModel(
        key = "tourism",
        factory = RouletteViewModel.factory(RouletteMode.TOURISM),
    )
    // 温泉・自然はスポット単位ルーレット（設定・重みを端末に永続化）。
    val application = LocalContext.current.applicationContext as android.app.Application
    val onsenViewModel: SpotRouletteViewModel = viewModel(
        key = "onsen_spot",
        factory = SpotRouletteViewModel.factory(application, SpotKind.ONSEN),
    )
    val natureViewModel: SpotRouletteViewModel = viewModel(
        key = "nature_spot",
        factory = SpotRouletteViewModel.factory(application, SpotKind.NATURE),
    )
    val planStore: TravelPlanStore = viewModel()

    // 最前面：個別スポット詳細。
    spotDetail?.let { sd ->
        SpotDetailScreen(detail = sd, store = planStore, onClose = { spotDetail = null })
        return
    }

    // 県別の一覧詳細。各カードタップで spotDetail を開く。
    when (val d = detail) {
        is Detail.Tourism -> {
            TourismDetailScreen(
                prefecture = d.prefecture, store = planStore,
                onOpenSpot = { spotDetail = it }, onBack = { detail = null },
            )
            return
        }
        is Detail.Onsen -> {
            OnsenDetailScreen(
                prefecture = d.prefecture, store = planStore,
                onOpenSpot = { spotDetail = it }, onBack = { detail = null },
            )
            return
        }
        is Detail.Nature -> {
            NatureDetailScreen(
                prefecture = d.prefecture, store = planStore,
                onOpenSpot = { spotDetail = it }, onBack = { detail = null },
            )
            return
        }
        null -> Unit
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                MainTab.PREFECTURE -> JapanMapScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = tourismViewModel,
                    onOpenTourism = { detail = Detail.Tourism(it) },
                )
                MainTab.ONSEN -> SpotRouletteScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = onsenViewModel,
                    onOpenSpot = { spotDetail = it.toSpotDetail() },
                )
                MainTab.NATURE -> SpotRouletteScreen(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = natureViewModel,
                    onOpenSpot = { spotDetail = it.toSpotDetail() },
                )
                MainTab.FESTIVAL -> FestivalScreen(onOpenSpot = { spotDetail = it })
                MainTab.PLAN -> PlanScreen(store = planStore)
            }
        }

        NavigationBar(
            modifier = Modifier
                .drawTopHairline()
                .navigationBarsPadding(),
            containerColor = AppTheme.Background,
        ) {
            MainTab.entries.forEach { tab ->
                val tabLabel = stringResource(tab.labelRes)
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    icon = { Icon(tab.icon, contentDescription = tabLabel) },
                    label = {
                        // ラベルは常に 1 行。英語などで長くなっても折り返して崩れないようにする。
                        Text(
                            tabLabel,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TabAccent,
                        selectedTextColor = TabAccent,
                        indicatorColor = TabAccent.copy(alpha = 0.12f),
                    ),
                )
            }
        }
    }
}

/** 未実装タブの仮表示。 */
@Composable
private fun PlaceholderTab(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(stringResource(R.string.placeholder_preparing, name), color = Color.Gray)
    }
}
