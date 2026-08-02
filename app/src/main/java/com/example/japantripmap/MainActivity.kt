package com.example.japantripmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

/** ボトムタブの定義。iOS 版 ContentView の 5 タブに対応。 */
private enum class MainTab(val label: String, val icon: ImageVector) {
    PREFECTURE("都道府県", Icons.Filled.Map),
    ONSEN("温泉", Icons.Filled.Hotel),
    NATURE("自然", Icons.Filled.Park),
    FESTIVAL("祭り", Icons.Filled.Celebration),
    PLAN("マイプラン", Icons.Filled.Luggage),
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

    // タブごとに別インスタンス（対象県・重みを独立管理）。
    val tourismViewModel: RouletteViewModel = viewModel(
        key = "tourism",
        factory = RouletteViewModel.factory(RouletteMode.TOURISM),
    )
    val onsenViewModel: RouletteViewModel = viewModel(
        key = "onsen",
        factory = RouletteViewModel.factory(RouletteMode.ONSEN),
    )
    val natureViewModel: RouletteViewModel = viewModel(
        key = "nature",
        factory = RouletteViewModel.factory(RouletteMode.NATURE),
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

    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TabAccent,
                            selectedTextColor = TabAccent,
                            indicatorColor = TabAccent.copy(alpha = 0.12f),
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selectedTab) {
                MainTab.PREFECTURE -> JapanMapScreen(
                    viewModel = tourismViewModel,
                    onOpenTourism = { detail = Detail.Tourism(it) },
                )
                MainTab.ONSEN -> JapanMapScreen(
                    viewModel = onsenViewModel,
                    onOpenTourism = { detail = Detail.Onsen(it) },
                )
                MainTab.NATURE -> JapanMapScreen(
                    viewModel = natureViewModel,
                    onOpenTourism = { detail = Detail.Nature(it) },
                )
                MainTab.FESTIVAL -> FestivalScreen(onOpenSpot = { spotDetail = it })
                MainTab.PLAN -> PlanScreen(store = planStore)
            }
        }
    }
}

/** 未実装タブの仮表示。 */
@Composable
private fun PlaceholderTab(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text("$name（準備中）", color = Color.Gray)
    }
}
