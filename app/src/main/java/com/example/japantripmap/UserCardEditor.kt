package com.example.japantripmap

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * カードの作成 / 編集画面。iOS 版 UserCardEditor を移植。
 * 詳細画面と同じ構成（表紙のヒーローヘッダー／説明／URL／位置）のまま、その場で直接入力する。
 * [editing] が null なら新規作成。全画面シートとして開く想定。
 */
@Composable
fun UserCardEditor(
    prefecture: Prefecture,
    kind: UserCardKind,
    store: TravelPlanStore,
    editing: UserCard? = null,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = kind.accent

    var name by remember { mutableStateOf(editing?.name.orEmpty()) }
    var description by remember { mutableStateOf(editing?.description.orEmpty()) }
    var urlText by remember { mutableStateOf(editing?.url.orEmpty()) }
    // 選び直した写真。保存するまでは元のファイルに触らない。
    var pickedPhoto by remember { mutableStateOf<Uri?>(null) }
    var photoRemoved by remember { mutableStateOf(false) }
    var latitude by remember { mutableStateOf(editing?.latitude) }
    var longitude by remember { mutableStateOf(editing?.longitude) }
    var placeName by remember { mutableStateOf(editing?.placeName) }
    var address by remember { mutableStateOf(editing?.address) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val coverPhoto: Any? = pickedPhoto ?: editing?.takeUnless { photoRemoved }?.let(UserCardStore::photoFile)
    val trimmedName = name.trim()
    val trimmedUrl = urlText.trim()
    val isUrlValid = trimmedUrl.isEmpty() || normalizedUrl(trimmedUrl) != null
    val canSave = trimmedName.isNotEmpty() && isUrlValid && !saving

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            pickedPhoto = uri
            photoRemoved = false
        }
    }

    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (editing != null) return@LaunchedEffect
        // 画面の表示が落ち着いてからキーボードを出す。
        delay(400)
        nameFocus.requestFocus()
    }

    fun save() {
        if (!canSave) return
        saving = true
        scope.launch {
            val photoFileName = when {
                pickedPhoto != null -> withContext(Dispatchers.IO) { UserCardStore.savePhoto(context, pickedPhoto!!) }
                photoRemoved -> null
                else -> editing?.photoFileName
            }
            val card = (editing ?: UserCard(prefectureName = prefecture.name, kind = kind, name = trimmedName)).copy(
                name = trimmedName,
                description = description.trim(),
                url = trimmedUrl.ifEmpty { null },
                photoFileName = photoFileName,
                latitude = latitude,
                longitude = longitude,
                placeName = placeName,
                address = address,
            )
            if (editing == null) {
                UserCardStore.add(card)
            } else {
                UserCardStore.update(card)?.let { previous -> store.syncUserCard(previous, card) }
            }
            onClose()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppTheme.Background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            // ヒーローヘッダー（表紙写真・名前）。
            Box(modifier = Modifier.fillMaxWidth()) {
                if (coverPhoto != null) {
                    AsyncImage(
                        model = coverPhoto,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.15f), Color.Black.copy(alpha = 0.65f)))),
                    )
                } else {
                    Box(modifier = Modifier.matchParentSize().background(accent))
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 150.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PhotoButtons(
                        hasPhoto = coverPhoto != null,
                        onPick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        onRemove = {
                            pickedPhoto = null
                            photoRemoved = true
                        },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BasicTextField(
                            value = name,
                            onValueChange = { name = it },
                            textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
                            decorationBox = { field ->
                                if (name.isEmpty()) {
                                    Text(
                                        stringResource(R.string.usercard_name_placeholder),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White.copy(alpha = 0.7f),
                                    )
                                }
                                field()
                            },
                        )
                        Text(
                            prefecture.localizedName(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                EditorCard(Icons.AutoMirrored.Filled.Notes, stringResource(R.string.detail_more_info), accent) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text(stringResource(R.string.usercard_description_placeholder)) },
                        colors = editorFieldColors(accent),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
                    )
                }

                EditorCard(Icons.Filled.Link, stringResource(R.string.usercard_url_label), accent) {
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        placeholder = { Text(stringResource(R.string.usercard_url_placeholder)) },
                        singleLine = true,
                        isError = !isUrlValid,
                        supportingText = if (isUrlValid) null else {
                            { Text(stringResource(R.string.usercard_url_invalid)) }
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Uri,
                        ),
                        colors = editorFieldColors(accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                val hasLocation = latitude != null && longitude != null
                EditorCard(Icons.Filled.LocationOn, stringResource(R.string.plan_place), accent) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLocationPicker = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (hasLocation) Icons.Filled.LocationOn else Icons.Filled.LocationOff,
                            contentDescription = null,
                            tint = if (hasLocation) accent else Color.Gray,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when {
                                !hasLocation -> stringResource(R.string.plan_location_add_optional)
                                !placeName.isNullOrBlank() -> placeName!!
                                !address.isNullOrBlank() -> address!!
                                else -> stringResource(R.string.plan_location_set)
                            },
                            fontSize = 14.sp,
                            color = AppTheme.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (hasLocation) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(18.dp))
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    if (hasLocation) {
                        Text(
                            stringResource(R.string.usercard_location_clear),
                            fontSize = 13.sp,
                            color = Color.Red,
                            modifier = Modifier.clickable {
                                latitude = null
                                longitude = null
                                placeName = null
                                address = null
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 上部のボタン（閉じる・保存）。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_cancel), tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .alpha(if (canSave) 1f else 0.5f)
                    .shadow(3.dp, RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .clickable(enabled = canSave) { save() }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (saving) {
                    CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text(stringResource(R.string.common_save), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accent)
                }
            }
        }
    }

    if (showLocationPicker) {
        // 未設定なら、この県の観光地の中心から探し始める。
        val attractions = prefecture.tourismInfo?.attractions.orEmpty()
        FullScreenSheet(onDismiss = { showLocationPicker = false }) {
            LocationPickerScreen(
                initialLat = latitude ?: attractions.map { it.latitude }.takeIf { it.isNotEmpty() }?.average(),
                initialLng = longitude ?: attractions.map { it.longitude }.takeIf { it.isNotEmpty() }?.average(),
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
        }
    }
}

@Composable
private fun PhotoButtons(hasPhoto: Boolean, onPick: () -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.22f))
                .clickable(onClick = onPick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                stringResource(if (hasPhoto) R.string.usercard_photo_change else R.string.usercard_photo_add),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        if (hasPhoto) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.usercard_photo_remove), tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun EditorCard(icon: ImageVector, title: String, accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().appCard().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
        content()
    }
}

@Composable
private fun editorFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    cursorColor = accent,
)
