package com.example.japantripmap

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.location.Geocoder
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * プラン項目 → 詳細画面のルーティング。iOS 版 PlanItemResolver / PlanItemDetailRouter を移植。
 *
 * - アプリ項目（観光/グルメ/温泉/自然/祭/お土産）は元データを name+県で引き当て、
 *   既存の共通詳細 SpotDetailScreen に流し込む SpotDetail を再構築する。
 * - カスタム項目（宿泊/交通/メモ）は座標・メモを保持する専用の CustomPlanItemScreen を開く。
 */

/** displayName から Prefecture を引き当てる（保存時の県名は displayName）。 */
private fun prefectureByName(name: String): Prefecture? =
    Prefecture.entries.firstOrNull { it.displayName == name }

/**
 * PlanItem から、対応する既存詳細画面用の SpotDetail を再構築する。
 * 元データが見つからない場合は name/detail だけの簡易 SpotDetail を返す（iOS の fallback 相当）。
 * カスタム項目（宿泊/交通/メモ）は SpotDetail を持たないため null を返す（呼び出し側で別画面へ）。
 */
internal fun spotDetailForPlanItem(item: PlanItem): SpotDetail? {
    if (item.category in CUSTOM_PLAN_CATEGORIES) return null
    val pref = prefectureByName(item.prefectureName) ?: return fallbackSpotDetail(item)

    return when (item.category) {
        PlanItemCategory.ATTRACTION -> {
            val a = pref.tourismInfo?.attractions?.firstOrNull { it.name == item.name }
                ?: return fallbackSpotDetail(item)
            SpotDetail(
                title = a.name,
                prefecture = pref,
                accent = AttractionAccentColor,
                icon = Icons.Filled.LocationOn,
                description = a.description,
                planCategory = PlanItemCategory.ATTRACTION,
                latitude = a.latitude,
                longitude = a.longitude,
                photoResName = ATTRACTION_PHOTOS[a.name],
            )
        }

        PlanItemCategory.GOURMET -> {
            val g = pref.gourmets.firstOrNull { it.name == item.name }
                ?: return fallbackSpotDetail(item)
            SpotDetail(
                title = g.name,
                prefecture = pref,
                accent = categoryColor(g.category),
                icon = Icons.Filled.Restaurant,
                description = g.description,
                planCategory = PlanItemCategory.GOURMET,
                badge = categoryLabel(g.category),
                popularity = g.popularity,
                infoRows = listOf(
                    "価格帯" to g.price,
                    "おすすめ時期" to g.bestSeason,
                    "カテゴリ" to categoryLabel(g.category),
                ),
                tabelogKeyword = g.name,
            )
        }

        PlanItemCategory.SOUVENIR -> {
            val s = pref.souvenirs.firstOrNull { it.name == item.name }
                ?: return fallbackSpotDetail(item)
            SpotDetail(
                title = s.name,
                prefecture = pref,
                accent = categoryColor(s.category),
                icon = Icons.Filled.CardGiftcard,
                description = s.description,
                planCategory = PlanItemCategory.SOUVENIR,
                badge = categoryLabel(s.category),
                popularity = s.popularity,
                infoRows = listOf(
                    "価格帯" to s.price,
                    "おすすめ時期" to s.bestSeason,
                    "カテゴリ" to categoryLabel(s.category),
                ),
            )
        }

        // 温泉・自然は RouletteSpot に既存の toSpotDetail() があるので再利用する。
        PlanItemCategory.ONSEN ->
            SpotRepository.all(SpotKind.ONSEN)
                .firstOrNull { it.name == item.name && it.prefecture == pref }
                ?.toSpotDetail()
                ?: fallbackSpotDetail(item)

        PlanItemCategory.NATURE ->
            SpotRepository.all(SpotKind.NATURE)
                .firstOrNull { it.name == item.name && it.prefecture == pref }
                ?.toSpotDetail()
                ?: fallbackSpotDetail(item)

        PlanItemCategory.FESTIVAL -> {
            val f = pref.festivals.firstOrNull { it.name == item.name }
                ?: return fallbackSpotDetail(item)
            SpotDetail(
                title = f.name,
                prefecture = pref,
                accent = festColor(f.category),
                icon = festIcon(f.category),
                description = f.description,
                planCategory = PlanItemCategory.FESTIVAL,
                latitude = f.latitude,
                longitude = f.longitude,
                badge = festLabel(f.category),
                popularity = f.scale,
                infoRows = listOf(
                    "開催地" to "${pref.localizedName()}・${localizeData(f.location)}",
                    "時期" to localizeData(f.month),
                    "期間" to localizeData(f.duration),
                ),
                features = f.features,
            )
        }

        // カスタムは上で早期 return 済み。網羅性のため。
        PlanItemCategory.HOTEL, PlanItemCategory.TRANSPORT, PlanItemCategory.OTHER -> null
    }
}

/** 元データが引き当てられなかったときの簡易 SpotDetail（保存済みの name/detail のみ）。 */
private fun fallbackSpotDetail(item: PlanItem): SpotDetail = SpotDetail(
    title = item.name,
    prefecture = prefectureByName(item.prefectureName) ?: Prefecture.entries.first(),
    accent = planCategoryColor(item.category),
    icon = planCategoryIcon(item.category),
    description = item.detail,
    planCategory = item.category,
)

/** 手動追加カスタム項目のカテゴリ集合（座標・メモを保持する専用詳細を開く）。 */
internal val CUSTOM_PLAN_CATEGORIES = setOf(
    PlanItemCategory.HOTEL, PlanItemCategory.TRANSPORT, PlanItemCategory.OTHER,
)

/**
 * プラン項目の地図表示用座標を解決する（iOS 版 PlanItemResolver.coordinate 相当）。
 * - カスタム項目（宿泊/交通/メモ）: 自身が保持する座標（未設定なら null）。
 * - グルメ/お土産: 元データに座標が無いため、ユーザーが手動で設定した座標（未設定なら null）。
 * - 観光/温泉/自然/祭: 元データから引き当て（祭は座標を持たない項目もある）。
 */
internal fun coordinateForPlanItem(item: PlanItem): LatLng? {
    // カスタム項目・グルメ・お土産は、自身が保持するユーザー設定座標を使う。
    if (item.category in CUSTOM_PLAN_CATEGORIES ||
        item.category == PlanItemCategory.GOURMET ||
        item.category == PlanItemCategory.SOUVENIR
    ) {
        val lat = item.latitude ?: return null
        val lng = item.longitude ?: return null
        return LatLng(lat, lng)
    }
    val pref = prefectureByName(item.prefectureName) ?: return null
    return when (item.category) {
        PlanItemCategory.ATTRACTION ->
            pref.tourismInfo?.attractions?.firstOrNull { it.name == item.name }
                ?.let { LatLng(it.latitude, it.longitude) }
        PlanItemCategory.ONSEN ->
            pref.onsens.firstOrNull { it.name == item.name }
                ?.let { LatLng(it.latitude, it.longitude) }
        PlanItemCategory.NATURE ->
            pref.natureSpots.firstOrNull { it.name == item.name }
                ?.let { LatLng(it.latitude, it.longitude) }
        PlanItemCategory.FESTIVAL ->
            pref.festivals.firstOrNull { it.name == item.name }
                ?.let { f -> f.latitude?.let { la -> f.longitude?.let { lo -> LatLng(la, lo) } } }
        else -> null // グルメ・お土産・カスタムは上で処理済み
    }
}

/** 観光アクセント（TourismDetailScreen の private 値と同一。ルーターでのみ使う）。 */
private val AttractionAccentColor = Color(0xFF4C8CFA)

// MARK: - カスタム項目の詳細画面（iOS 版 CustomPlanItemView 相当）

/**
 * 宿泊/交通/メモの詳細画面。カテゴリ色のヒーロー → メモ → 位置カード（地図＋住所コピー＋マップ起動）。
 * 右上に編集ボタン。編集すると onEdit 経由でエディタを開く。
 */
@Composable
fun CustomPlanItemScreen(
    item: PlanItem,
    onEdit: () -> Unit,
    onClose: () -> Unit,
) {
    val accent = planCategoryColor(item.category)

    Box(modifier = Modifier.fillMaxSize().background(accent)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // ヒーローヘッダー（カテゴリ色のグラデーション＋アイコン丸＋タイトル）。
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.85f)))),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 90.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            planCategoryIcon(item.category),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                    Text(item.name, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(
                        stringResource(item.category.labelRes),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // 本文。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppTheme.Background)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // メモカード。
                if (item.detail.isNotBlank()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().appCard().padding(16.dp),
                    ) {
                        CustomSectionHeader(Icons.AutoMirrored.Filled.Notes, stringResource(R.string.plan_memo), accent)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(item.detail, fontSize = 14.sp, color = Color(0xFF444444), lineHeight = 22.sp)
                    }
                }

                // 位置カード（座標があるとき）。
                if (item.hasCoordinate) {
                    PlanItemLocationCard(item = item, accent = accent)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // 右上：編集・閉じる。
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 44.dp, end = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircleIconButton(Icons.Filled.Edit, accent, onEdit)
            CircleIconButton(Icons.Filled.Close, accent, onClose)
        }
    }
}

/**
 * プラン項目に設定した位置を表示するカード（iOS 版 PlanPlaceMapCard 相当）。
 * 施設名・住所（コピー）・地図プレビュー・「マップで開く」をまとめる。
 * カスタム項目の詳細と、グルメ・お土産の詳細（プラン経由）で共用する。
 * 呼び出し側で座標の有無を保証すること（latitude/longitude は非 null 前提）。
 */
@Composable
fun PlanItemLocationCard(item: PlanItem, accent: Color) {
    val context = LocalContext.current
    var addressCopied by remember(item.id) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().appCard().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CustomSectionHeader(Icons.Filled.LocationOn, stringResource(R.string.plan_place), accent)

        if (!item.placeName.isNullOrBlank()) {
            Text(item.placeName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accent)
        }
        if (!item.address.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.address,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    copyToClipboard(context, item.address)
                    addressCopied = true
                    Toast.makeText(context, context.getString(R.string.plan_address_copied), Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        if (addressCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                        contentDescription = stringResource(R.string.plan_copy_address),
                        tint = if (addressCopied) Color(0xFF34C759) else accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        PlacesMapSection(
            showSelectedPlace = false,
            places = listOf(
                MapPlace(
                    id = item.id,
                    title = item.placeName ?: item.name,
                    subtitle = item.address ?: "",
                    latitude = item.latitude!!,
                    longitude = item.longitude!!,
                ),
            ),
        )

        // 「マップで開く」。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(accent)
                .clickable {
                    openInMaps(context, item.latitude!!, item.longitude!!, item.placeName ?: item.name)
                }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.plan_open_in_map), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun CircleIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CustomSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .background(accent, RoundedCornerShape(2.dp)),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

// MARK: - カスタム項目の追加/編集シート（iOS 版 CustomPlanItemEditor 相当）

/**
 * 宿泊/交通/メモの追加・編集画面。iOS 版 CustomPlanItemEditor を移植（全画面フォーム）。
 * 背景グラデーションの上にカード（種類 / タイトル / メモ / 位置）を積み、下に保存ボタン。
 * 位置カードをタップすると LocationPickerScreen をこの画面の上に全画面表示する。
 * editing が null なら新規追加、非 null ならその項目を編集する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPlanItemSheet(
    editing: PlanItem? = null,
    onDismiss: () -> Unit,
    onSave: (PlanItem) -> Unit,
) {
    var category by remember { mutableStateOf(editing?.category ?: PlanItemCategory.HOTEL) }
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var detail by remember { mutableStateOf(editing?.detail ?: "") }
    // 位置（緯度経度・施設名・住所）。
    var latitude by remember { mutableStateOf(editing?.latitude) }
    var longitude by remember { mutableStateOf(editing?.longitude) }
    var placeName by remember { mutableStateOf(editing?.placeName) }
    var address by remember { mutableStateOf(editing?.address) }
    var showLocationPicker by remember { mutableStateOf(false) }

    val customCategories = listOf(PlanItemCategory.HOTEL, PlanItemCategory.TRANSPORT, PlanItemCategory.OTHER)

    // 位置ピッカー表示中は最前面にそれを出す（ボトムシートと違い全画面なので確実に前面へ）。
    if (showLocationPicker) {
        LocationPickerScreen(
            initialLat = latitude,
            initialLng = longitude,
            onCancel = { showLocationPicker = false },
            onPick = { lat, lng, resolvedAddress ->
                latitude = lat
                longitude = lng
                address = resolvedAddress
                // 施設名は手動ピンでは持たない（住所を主表示に使う）。
                placeName = null
                showLocationPicker = false
            },
        )
        return
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (editing == null) R.string.plan_item_add else R.string.plan_item_edit), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_cancel))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
            )
        },
        // 下地に不透明色を敷いてから背景グラデーションを重ねる（下の画面が透けないように）。
        modifier = Modifier
            .background(AppTheme.Background)
            .background(PlanTheme.backgroundGradient),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 種類カード（アイコン付きのタイル 3 種）。
            Column(
                modifier = Modifier.fillMaxWidth().appCard().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(R.string.plan_item_kind), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    customCategories.forEach { c ->
                        val selected = category == c
                        val color = planCategoryColor(c)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) color.copy(alpha = 0.18f) else PlanTheme.Surface2)
                                .then(
                                    if (selected) {
                                        Modifier.border(1.5.dp, color, RoundedCornerShape(12.dp))
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { category = c }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                planCategoryIcon(c),
                                contentDescription = null,
                                tint = if (selected) color else Color.Gray,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                stringResource(c.labelRes),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) color else Color.Gray,
                            )
                        }
                    }
                }
            }

            // タイトルカード。
            Column(
                modifier = Modifier.fillMaxWidth().appCard().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.plan_item_title), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(stringResource(R.string.plan_item_title_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // メモカード。
            Column(
                modifier = Modifier.fillMaxWidth().appCard().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.plan_memo), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlanTheme.Primary)
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    placeholder = { Text(stringResource(R.string.plan_memo_optional)) },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                )
            }

            // 位置カード（タップで地図ピッカーを開く）。
            val hasLoc = latitude != null && longitude != null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .appCard()
                    .clickable { showLocationPicker = true }
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (hasLoc) Icons.Filled.LocationOn else Icons.Filled.LocationOff,
                    contentDescription = null,
                    tint = if (hasLoc) PlanTheme.Primary else Color.Gray,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = when {
                        !placeName.isNullOrBlank() -> placeName!!
                        !address.isNullOrBlank() -> address!!
                        hasLoc -> stringResource(R.string.plan_location_set)
                        else -> stringResource(R.string.plan_location_add_optional)
                    },
                    fontSize = 14.sp,
                    color = if (hasLoc) AppTheme.TextPrimary else Color.Gray,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (hasLoc) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }

            PlanPrimaryButton(
                text = stringResource(if (editing == null) R.string.plan_add else R.string.common_save),
                icon = if (editing == null) Icons.Filled.Add else null,
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (name.isNotBlank()) {
                        val base = editing ?: PlanItem(category = category, prefectureName = "", name = name)
                        onSave(
                            base.copy(
                                category = category,
                                name = name,
                                detail = detail,
                                latitude = latitude,
                                longitude = longitude,
                                placeName = if (latitude == null) null else placeName,
                                address = if (latitude == null) null else address,
                            ),
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// MARK: - 位置ピッカー（iOS 版 LocationPickerView 相当・地図中心固定ピン方式）

/**
 * 地図を動かして中央のピンで位置を決める全画面ピッカー。保存時に Geocoder で住所を補完する。
 * 上部のテキスト検索（Android 標準 Geocoder・無料）で地名/住所/駅名を検索して地図を移動できる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    initialLat: Double?,
    initialLng: Double?,
    onCancel: () -> Unit,
    onPick: (lat: Double, lng: Double, address: String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var resolving by remember { mutableStateOf(false) }

    // テキスト検索の状態。
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<GeocodeResult>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }

    // 初期中心（既存座標 → 東京駅）。
    val start = LatLng(initialLat ?: 35.681236, initialLng ?: 139.767125)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(start, 14f)
    }
    val properties = remember { MapProperties() }
    val uiSettings = remember {
        MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false, mapToolbarEnabled = false)
    }

    // 検索を実行してカメラを先頭ヒットへ移動する（複数ヒット時は候補リストも出す）。
    fun runSearch() {
        val q = query.trim()
        if (q.isBlank() || searching) return
        keyboard?.hide()
        searching = true
        scope.launch {
            val found = geocodeAddress(context, q)
            searching = false
            searched = true
            results = found
            found.firstOrNull()?.let {
                cameraPositionState.position =
                    CameraPosition.fromLatLngZoom(LatLng(it.latitude, it.longitude), 15f)
            }
        }
    }

    Scaffold(
        containerColor = AppTheme.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.location_picker_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.TopBar),
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = properties,
                uiSettings = uiSettings,
            )

            // 中央固定ピン。
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = PlanTheme.Primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .padding(bottom = 22.dp), // ピン先端を中心に合わせる
            )

            // 上部：テキスト検索バー＋候補リスト。
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        if (it.isBlank()) {
                            results = emptyList()
                            searched = false
                        }
                    },
                    placeholder = { Text(stringResource(R.string.location_search_hint)) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = {
                                query = ""
                                results = emptyList()
                                searched = false
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_clear))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(14.dp)),
                )

                // 検索結果（複数ヒット時に候補として提示。タップでその地点へ移動）。
                if (searching) {
                    LocationSearchStatus(stringResource(R.string.location_searching))
                } else if (searched && results.isEmpty()) {
                    LocationSearchStatus(stringResource(R.string.location_not_found))
                } else if (results.size > 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White),
                    ) {
                        results.forEach { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                            LatLng(r.latitude, r.longitude), 15f,
                                        )
                                        results = listOf(r) // 選択を確定して候補を畳む
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = PlanTheme.Primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    r.label,
                                    fontSize = 14.sp,
                                    color = AppTheme.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            // 下部の保存ボタン。
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                PlanPrimaryButton(
                    text = stringResource(if (resolving) R.string.location_resolving else R.string.location_save_point),
                    enabled = !resolving,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val center = cameraPositionState.position.target
                        resolving = true
                        scope.launch {
                            val addr = reverseGeocode(context, center.latitude, center.longitude)
                            resolving = false
                            onPick(center.latitude, center.longitude, addr)
                        }
                    },
                )
            }
        }
    }
}

/** 検索中/該当なしを地図上に淡いカードで示す。 */
@Composable
private fun LocationSearchStatus(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, fontSize = 13.sp, color = Color.Gray)
    }
}

// MARK: - ヘルパー

/** 検索結果の 1 地点（テキスト検索用）。 */
internal data class GeocodeResult(
    val label: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * テキスト（地名・住所・駅名など）→ 座標の順ジオコーディング（Android 標準 Geocoder・無料）。
 * Places API は使わないため個別の施設名には弱いが、地名/住所/駅名は十分ヒットする。
 * 取得失敗・0 件時は空リストを返す。
 */
private suspend fun geocodeAddress(context: Context, query: String): List<GeocodeResult> =
    withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching {
            @Suppress("DEPRECATION")
            val results = Geocoder(context).getFromLocationName(query, 5) ?: emptyList()
            results.mapNotNull { a ->
                // 表示名：feature/住所行 → 市区町村 の順で人間可読なラベルを組む。
                val label = listOfNotNull(
                    a.featureName?.takeIf { it.isNotBlank() && it != a.thoroughfare },
                    listOfNotNull(a.adminArea, a.locality, a.subLocality, a.thoroughfare)
                        .filter { it.isNotBlank() }
                        .joinToString("")
                        .ifBlank { null },
                ).distinct().joinToString(" ").ifBlank { query }
                GeocodeResult(label, a.latitude, a.longitude)
            }
        }.getOrDefault(emptyList())
    }

/** 逆ジオコーディングで 1 行の住所を得る（取得失敗時は null）。 */
private suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            @Suppress("DEPRECATION")
            val results = Geocoder(context).getFromLocation(lat, lng, 1)
            val a = results?.firstOrNull() ?: return@runCatching null
            // 都道府県→市区町村→番地の順で連結（国・郵便番号は省く）。
            listOfNotNull(
                a.adminArea,
                a.locality,
                a.subLocality,
                a.thoroughfare,
                a.subThoroughfare,
            ).filter { it.isNotBlank() }.joinToString("").ifBlank { null }
        }.getOrNull()
    }

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText("address", text))
}

/** 座標を地図アプリで開く（施設名があればラベル付き）。 */
private fun openInMaps(context: Context, lat: Double, lng: Double, label: String) {
    val encoded = Uri.encode(label)
    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($encoded)")
    val intent = Intent(Intent.ACTION_VIEW, uri)
    runCatching { context.startActivity(intent) }.onFailure {
        // 地図アプリが無ければ Google Maps の Web にフォールバック。
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
        runCatching { context.startActivity(web) }
    }
}
