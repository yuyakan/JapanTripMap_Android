package com.example.japantripmap

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 近くのスポットの種類。iOS 版 NearbySpotKind。 */
enum class NearbySpotKind(val color: Color, val icon: ImageVector) {
    TOURISM(PlanTheme.Primary, Icons.Filled.CameraAlt),
    ONSEN(Color(0xFFE65959), Icons.Filled.WaterDrop),
    NATURE(Color(0xFF33996B), Icons.Filled.Eco),
}

@Composable
private fun NearbySpotKind.title(): String = when (this) {
    NearbySpotKind.TOURISM -> stringResource(R.string.nearby_filter_tourism)
    NearbySpotKind.ONSEN -> stringResource(R.string.tab_onsen_place)
    NearbySpotKind.NATURE -> stringResource(R.string.tab_nature)
}

/** 観光・温泉・自然を 1 つに畳んだスポット。iOS 版 NearbySpot。 */
data class NearbySpot(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val kind: NearbySpotKind,
    /** 詳細画面（種類ごとの既存詳細）を組み立てる。 */
    val detail: () -> SpotDetail,
    val distanceMeters: Float? = null,
)

/** アプリ内の全スポット（観光・温泉・自然）。iOS 版 NearbySpotRepository.allSpots。 */
private val ALL_NEARBY_SPOTS: List<NearbySpot> by lazy {
    val result = mutableListOf<NearbySpot>()
    Prefecture.entries.forEach { pref ->
        pref.tourismInfo?.attractions?.forEachIndexed { i, a ->
            result += NearbySpot("tourism_${pref.name}_$i", a.name, a.latitude, a.longitude, NearbySpotKind.TOURISM, { attractionSpotDetail(pref, a) })
        }
    }
    SpotRepository.all(SpotKind.ONSEN).forEach { s ->
        result += NearbySpot("onsen_${s.id}", s.name, s.latitude, s.longitude, NearbySpotKind.ONSEN, { s.toSpotDetail() })
    }
    SpotRepository.all(SpotKind.NATURE).forEach { s ->
        result += NearbySpot("nature_${s.id}", s.name, s.latitude, s.longitude, NearbySpotKind.NATURE, { s.toSpotDetail() })
    }
    result
}

private fun sortedByDistance(origin: LatLng): List<NearbySpot> {
    val out = FloatArray(1)
    return ALL_NEARBY_SPOTS.map { s ->
        Location.distanceBetween(origin.latitude, origin.longitude, s.latitude, s.longitude, out)
        s.copy(distanceMeters = out[0])
    }.sortedBy { it.distanceMeters }
}

/** 東京駅（現在地なし・国外時の初期位置）。 */
private val TOKYO_CENTER = LatLng(35.6812, 139.7671)
/** 地図の初期・検索後のズーム（iOS の span 0.5° 相当）。 */
private const val DEFAULT_ZOOM = 9.5f
/** 地図に出すピンの上限件数と距離しきい値（100km 以内・近い順に最大 20 件）。 */
private const val MAX_MAPPED_PINS = 20
private const val MAPPED_RADIUS_METERS = 100_000f

/** 現在地が日本の範囲内か（国外なら採用せず東京にフォールバック）。 */
private fun isInJapan(p: LatLng) = p.latitude in 24.0..46.0 && p.longitude in 122.0..154.0

/**
 * 「近く」タブ。iOS 版 NearbyView。
 * 現在地（または地図で選んだ地点）を中心に、周辺の観光地・温泉・自然を地図のピンと横スクロールカードで出す。
 * 位置情報が未許可・取得前・国外のときは東京で暫定表示し、日本国内の現在地が取れたら一度だけ追従する。
 */
@SuppressLint("MissingPermission")
@Composable
fun NearbyScreen(onOpenSpot: (SpotDetail) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf<NearbySpotKind?>(null) }
    var sortedSpots by remember { mutableStateOf(sortedByDistance(TOKYO_CENTER)) }
    var hasPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    // 確定検索（現在地採用・ユーザー操作）が済んだか。済んだ後は現在地に自動追従しない。
    var committed by remember { mutableStateOf(false) }
    var showNoResults by remember { mutableStateOf(false) }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(TOKYO_CENTER, DEFAULT_ZOOM)
    }

    fun search(center: LatLng, isCommitted: Boolean = true) {
        if (isCommitted) committed = true
        sortedSpots = sortedByDistance(center)
        scope.launch { cameraState.animate(CameraUpdateFactory.newLatLngZoom(center, DEFAULT_ZOOM), 400) }
        if (isCommitted && sortedSpots.none { (it.distanceMeters ?: Float.MAX_VALUE) <= MAPPED_RADIUS_METERS }) {
            showNoResults = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        requestCurrentLocation(context) { loc -> currentLocation = loc }
    }
    LaunchedEffect(currentLocation) {
        val loc = currentLocation ?: return@LaunchedEffect
        if (!committed && isInJapan(loc)) search(loc)
    }
    LaunchedEffect(showNoResults) {
        if (showNoResults) {
            delay(3000)
            showNoResults = false
        }
    }

    val visible = sortedSpots.filter { filter == null || it.kind == filter }
    val mapped = visible.filter { (it.distanceMeters ?: Float.MAX_VALUE) <= MAPPED_RADIUS_METERS }.take(MAX_MAPPED_PINS)

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(isMyLocationEnabled = hasPermission && currentLocation != null),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false, mapToolbarEnabled = false),
        ) {
            mapped.forEach { spot ->
                key(spot.id) {
                    MarkerComposable(
                        keys = arrayOf(spot.id),
                        state = rememberUpdatedMarkerState(position = LatLng(spot.latitude, spot.longitude)),
                        onClick = {
                            onOpenSpot(spot.detail())
                            true
                        },
                    ) {
                        NearbyPin(spot)
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            // 種類切替（すべて/観光/温泉/自然）。地図の上でも読めるよう非選択も不透明カプセル。
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                val options: List<NearbySpotKind?> = listOf(null) + NearbySpotKind.entries
                items(options) { option ->
                    val selected = filter == option
                    Text(
                        option?.title() ?: stringResource(R.string.nearby_filter_all),
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) Color.White else PlanTheme.Primary,
                        modifier = Modifier
                            .shadow(3.dp, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .then(if (selected) Modifier.background(PlanTheme.brandGradient) else Modifier.background(Color.White))
                            .clickable { filter = option }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = showNoResults,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.LocationOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.nearby_no_results), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 2)
                }
            }
        }

        // 下部: 「このエリアを探す」＋距離順のカード。
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .shadow(6.dp, RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .clickable { search(cameraState.position.target) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Sync, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.nearby_search_this_area), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PlanTheme.Primary)
            }
            if (mapped.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    items(mapped, key = { it.id }) { spot ->
                        NearbyCard(spot) { onOpenSpot(spot.detail()) }
                    }
                }
            }
        }
    }
}

/** 地図ピン（種類色の丸アイコン＋白カプセルの名前ラベル）。 */
@Composable
private fun NearbyPin(spot: NearbySpot) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(spot.kind.color)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(spot.kind.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            localizeData(spot.name),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.TextPrimary,
            maxLines = 1,
            modifier = Modifier
                .widthIn(max = 160.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

/** 地図の上に重ねる横スクロールカード（種類・名前・距離）。 */
@Composable
private fun NearbyCard(spot: NearbySpot, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .height(120.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(spot.kind.color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(spot.kind.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(spot.kind.title(), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AppTheme.TextSecondary)
        }
        Text(
            localizeData(spot.name),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.weight(1f))
        spot.distanceMeters?.let { d ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = PlanTheme.Primary, modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (d >= 1000f) stringResource(R.string.nearby_distance_km, d / 1000f)
                    else stringResource(R.string.nearby_distance_m, d),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlanTheme.Primary,
                )
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/**
 * 現在地を 1 回だけ取得する（おおよその位置で十分なのでネットワーク測位を優先）。
 * 直近の既知位置があれば先に返し、続けて最新の測位結果でも呼ぶ。
 */
@SuppressLint("MissingPermission")
private fun requestCurrentLocation(context: Context, onLocation: (LatLng) -> Unit) {
    if (!hasLocationPermission(context)) return
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
    providers.firstNotNullOfOrNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
        ?.let { onLocation(LatLng(it.latitude, it.longitude)) }
    // 端末によって測位できるプロバイダが違うので、有効なものすべてに 1 回ずつ問い合わせる
    // （最初に返った位置で初回検索され、以降は確定済みなので追従しない）。
    providers.filter { it != LocationManager.PASSIVE_PROVIDER }.forEach { provider ->
        runCatching {
            LocationManagerCompat.getCurrentLocation(lm, provider, android.os.CancellationSignal(), ContextCompat.getMainExecutor(context)) { loc ->
                loc?.let { onLocation(LatLng(it.latitude, it.longitude)) }
            }
        }
    }
}
