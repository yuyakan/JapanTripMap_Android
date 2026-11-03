package com.example.japantripmap

import androidx.activity.compose.BackHandler
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import kotlin.concurrent.thread
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NearMe
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
        // EEA 等では UMP の同意フォームを表示し、同意が済んで（または不要で）広告リクエストが
        // 許可されてから AdMob を初期化する。対象外の地域では即座に許可され従来どおり広告が出る。
        ConsentManager.gatherConsentThen(this) { initializeAds() }
        // お気に入り・最近見た・訪問済みの県、トレンド動画キャッシュを使えるようにする。
        // ストア用スクショの撮影モード（デバッグ版で --ez screenshot_mode true のときだけ有効）。
        ScreenshotMode.init(intent)
        PrefectureStores.init(this)
        UserCardStore.init(this)
        TrendRepository.init(this)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = AppTheme.Background) {
                    AppRoot()
                }
            }
        }
    }

    /**
     * AdMob（GMA Next-Gen SDK）を初期化し、最初のインタースティシャルを先読みする。
     * UMP の同意フロー（[ConsentManager]）で広告リクエストが許可された後に呼ばれる。
     * Next-Gen SDK は初期化を必ずバックグラウンドスレッドで行うよう求めている（ANR 回避）。
     * アプリ ID は AndroidManifest の APPLICATION_ID メタデータから読み、単一の情報源に揃える。
     */
    private fun initializeAds() {
        if (MobileAds.isInitialized) return
        val appId = admobAppIdFromManifest() ?: run {
            Log.w(TAG, "AdMob の APPLICATION_ID がマニフェストに無いため初期化をスキップ")
            return
        }
        thread(name = "MobileAdsInit") {
            MobileAds.initialize(
                applicationContext,
                InitializationConfig.Builder(appId).build(),
            ) {
                // アダプタ初期化完了。最初のインタースティシャルを先読みしておく。
                InterstitialAdManager.loadAd(applicationContext)
            }
        }
    }

    /** AndroidManifest の `com.google.android.gms.ads.APPLICATION_ID` メタデータ値を取得する。 */
    private fun admobAppIdFromManifest(): String? = try {
        val appInfo = packageManager.getApplicationInfo(
            packageName, PackageManager.GET_META_DATA,
        )
        appInfo.metaData?.getString("com.google.android.gms.ads.APPLICATION_ID")
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private companion object {
        private const val TAG = "MainActivity"

        /** スプラッシュを表示し続ける時間（iOS 版の 2 秒に合わせる）。 */
        const val SPLASH_DURATION_MS = 2000L
    }
}

/** 下タブのラベル・アイコン。並びは iOS 版 ContentView（0:マップ 1:近く 2:ホーム 3:祭 4:マイプラン）。 */
private val MainTab.labelRes: Int
    get() = when (this) {
        MainTab.MAP -> R.string.tab_map
        MainTab.NEARBY -> R.string.tab_nearby
        MainTab.HOME -> R.string.tab_home
        MainTab.FESTIVAL -> R.string.tab_festival
        MainTab.PLAN -> R.string.tab_plan
    }

private val MainTab.icon: ImageVector
    get() = when (this) {
        MainTab.MAP -> Icons.Outlined.Map
        MainTab.NEARBY -> Icons.Outlined.NearMe
        MainTab.HOME -> Icons.Outlined.Home
        MainTab.FESTIVAL -> Icons.Outlined.AutoAwesome
        MainTab.PLAN -> Icons.Outlined.Luggage
    }

/** 全画面で開く県詳細。spot があれば県詳細の上にその詳細まで続けて開く（ホームのテーマカード）。 */
private data class TourismTarget(val prefecture: Prefecture)

/**
 * アプリのルート。iOS 版 ContentView（3.0.0）に合わせた 5 タブ構成。
 * 詳細（県詳細・単体詳細・県検索・ホームから開いたプラン）はタブの上に全画面で重ねる。
 */
@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    // 表示中の県詳細。null ならタブ画面。
    var tourism by remember { mutableStateOf<TourismTarget?>(null) }
    // 個別スポット詳細（県詳細のさらに上に重ねる）。
    var spotDetail by remember { mutableStateOf<SpotDetail?>(null) }
    // 県一覧・検索（マップタブ右上の🔍）。
    var showPrefectureSearch by remember { mutableStateOf(false) }
    // ホームの「次の旅」から開いたプラン詳細。
    var homePlanId by remember { mutableStateOf<String?>(null) }
    // ホームから開いた Shorts フィード（下タブも覆う全画面）。
    var shortsFeed by remember { mutableStateOf<ShortsFeedRequest?>(null) }
    // 統合マップタブで表示中のマップ（タブを離れても保持する）。
    var mapMode by remember { mutableStateOf(MapMode.PREFECTURE) }

    // 都道府県ルーレット（全国モード）。有効県・重みは端末に保存する。
    val tourismViewModel: RouletteViewModel = viewModel(
        key = "tourism",
        factory = RouletteViewModel.factory(
            RouletteMode.TOURISM,
            application.getSharedPreferences("roulette_settings", android.content.Context.MODE_PRIVATE),
        ),
    )
    // 温泉・自然はスポット単位ルーレット（設定・重みを端末に永続化）。
    val onsenViewModel: SpotRouletteViewModel = viewModel(
        key = "onsen_spot",
        factory = SpotRouletteViewModel.factory(application, SpotKind.ONSEN),
    )
    val natureViewModel: SpotRouletteViewModel = viewModel(
        key = "nature_spot",
        factory = SpotRouletteViewModel.factory(application, SpotKind.NATURE),
    )
    val planStore: TravelPlanStore = viewModel()

    val openSpot: (SpotDetail) -> Unit = { spotDetail = it }

    shortsFeed?.let { feed ->
        ShortsFeedScreen(request = feed, onClose = { shortsFeed = null })
        return
    }

    // 最前面：個別スポット詳細。
    spotDetail?.let { sd ->
        SpotDetailScreen(
            detail = sd, store = planStore,
            // 広告カウントの加算は SpotDetailScreen 側で行う。
            onClose = { spotDetail = null },
        )
        return
    }

    // 県詳細。各カードタップで spotDetail を開く。
    tourism?.let { t ->
        TourismDetailScreen(
            prefecture = t.prefecture, store = planStore,
            onOpenSpot = openSpot,
            onBack = { tourism = null },
        )
        return
    }

    if (showPrefectureSearch) {
        PrefectureSearchScreen(
            onSelect = { tourism = TourismTarget(it) },
            onClose = { showPrefectureSearch = false },
        )
        return
    }

    val selectedTab = AppRouter.selectedTab
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                MainTab.MAP -> MapTabScreen(
                    mapMode = mapMode,
                    onModeChange = { mapMode = it },
                ) { mode ->
                    when (mode) {
                        MapMode.PREFECTURE -> JapanMapScreen(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = tourismViewModel,
                            onOpenTourism = { tourism = TourismTarget(it) },
                            onOpenSearch = { showPrefectureSearch = true },
                        )
                        MapMode.ONSEN -> SpotRouletteScreen(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = onsenViewModel,
                            onOpenSpot = { spotDetail = it.toSpotDetail() },
                        )
                        MapMode.NATURE -> SpotRouletteScreen(
                            modifier = Modifier.fillMaxSize(),
                            viewModel = natureViewModel,
                            onOpenSpot = { spotDetail = it.toSpotDetail() },
                        )
                    }
                }
                MainTab.NEARBY -> NearbyScreen(onOpenSpot = openSpot)
                // ホームの「次の旅」から開いたプランは、iOS の NavigationLink と同じくホームタブ内に重ねる。
                MainTab.HOME -> homePlanId?.let { id ->
                    PlanScreen(store = planStore, fixedPlanId = id, onExitFixedPlan = { homePlanId = null })
                } ?: HomeScreen(
                    store = planStore,
                    onOpenPrefecture = { pref, spot ->
                        tourism = TourismTarget(pref)
                        spotDetail = spot
                    },
                    onOpenPlan = { homePlanId = it },
                    onOpenShorts = { shortsFeed = it },
                )
                MainTab.FESTIVAL -> FestivalScreen(onOpenSpot = openSpot)
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
                    onClick = { AppRouter.selectedTab = tab },
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
                        selectedIconColor = PlanTheme.Primary,
                        selectedTextColor = PlanTheme.Primary,
                        indicatorColor = PlanTheme.Primary.copy(alpha = 0.12f),
                    ),
                )
            }
        }
    }
}
