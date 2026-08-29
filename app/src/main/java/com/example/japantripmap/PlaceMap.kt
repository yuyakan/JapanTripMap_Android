package com.example.japantripmap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/** 地図に載せるスポット。 */
data class MapPlace(
    val id: String,
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
)

fun SpotDetail.mapPlace(): MapPlace? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    return MapPlace(
        id = title,
        title = title,
        subtitle = prefecture.displayName,
        latitude = lat,
        longitude = lng,
    )
}

private fun centerOf(places: List<MapPlace>): LatLng {
    if (places.isEmpty()) return LatLng(35.681236, 139.767125)
    val lat = places.map { it.latitude }.average()
    val lng = places.map { it.longitude }.average()
    return LatLng(lat, lng)
}

private fun zoomFor(places: List<MapPlace>): Float {
    if (places.size <= 1) return 12f
    val latSpan = places.maxOf { it.latitude } - places.minOf { it.latitude }
    val lngSpan = places.maxOf { it.longitude } - places.minOf { it.longitude }
    val span = maxOf(latSpan, lngSpan)
    return when {
        span < 0.05 -> 13.5f
        span < 0.15 -> 11.5f
        span < 0.4 -> 10f
        span < 0.9 -> 8.3f
        span < 2.0 -> 7.2f
        else -> 6f
    }
}

@Composable
fun PlacesMapSection(
    places: List<MapPlace>,
    modifier: Modifier = Modifier,
    /** 地図上部のタイトル。null なら非表示。 */
    title: String? = null,
    /** 地図下部に選択中スポット名を出すか。 */
    showSelectedPlace: Boolean = true,
    /**
     * 地図に指が触れている間 true を通知する。詳細画面は外側が verticalScroll / LazyColumn の
     * ため、これを受けて地図ドラッグ中だけ親スクロールを止め、地図のパンを親に横取りされないようにする。
     */
    onMapTouch: (Boolean) -> Unit = {},
) {
    if (places.isEmpty()) return

    var selectedId by remember(places) { mutableStateOf(places.first().id) }
    val selectedPlace = places.firstOrNull { it.id == selectedId } ?: places.first()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerOf(places), zoomFor(places))
    }
    LaunchedEffect(places) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(centerOf(places), zoomFor(places))
    }
    val properties = remember { MapProperties() }
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
        )
    }

    // 外枠（カード）は付けず、地図は左右16dpの均等スペース＋角丸で大きめに表示する。
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color(0xFF1F1F1F),
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp),
            )
        }

        // 地図は左右8dpの均等マージン・高さ 280dp・角丸14dp。見切れないよう親内に収める。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(280.dp)
                .clip(RoundedCornerShape(14.dp))
                // Initial パスで down/up を「消費せず」監視し、地図に触れている間だけ親へ通知する。
                // 消費しないので地図（AndroidView）にもイベントが届き、パン・ズームはそのまま機能する。
                // 親側はこの通知でスクロールを一時停止し、地図ドラッグの横取りを防ぐ。
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitPointerEvent(PointerEventPass.Initial)
                            if (down.changes.any { it.pressed }) onMapTouch(true)
                            // 全ポインタが離れるまで待ってから解除する。
                            do {
                                val e = awaitPointerEvent(PointerEventPass.Initial)
                            } while (e.changes.any { it.pressed })
                            onMapTouch(false)
                        }
                    }
                },
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings,
            ) {
                places.forEach { place ->
                    Marker(
                        state = MarkerState(position = LatLng(place.latitude, place.longitude)),
                        title = localizeData(place.title),
                        snippet = localizeData(place.subtitle),
                        onClick = {
                            selectedId = place.id
                            true
                        },
                    )
                }
            }
        }

        if (showSelectedPlace) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = localizeData(selectedPlace.title),
                    fontSize = 15.sp,
                    color = Color(0xFF1F1F1F),
                )
                if (selectedPlace.subtitle.isNotBlank()) {
                    Text(
                        text = localizeData(selectedPlace.subtitle),
                        fontSize = 12.sp,
                        color = Color.Gray,
                    )
                }
            }
        }
    }
}
