package com.example.japantripmap

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.DownhillSkiing
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Waves
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 温泉・自然の「スポット単位ルーレット」で扱う 1 スポット。
 *
 * iOS 版 FixedOnsenItem / FixedNatureSpotItem を Android 用に統合したもの。
 * id は都道府県名＋スポット名＋タイプで安定させ、有効スポット集合や重み Map のキーに使う。
 */
data class RouletteSpot(
    val id: String,
    val name: String,
    val description: String,
    /** 元データのタイプ生キー（温泉: scenic 等 / 自然: night_view 等）。 */
    val typeKey: String,
    val popularity: Int,
    val prefecture: Prefecture,
    val latitude: Double,
    val longitude: Double,
    /** このスポットの分類（温泉 or 自然）。タイプメタの解決に使う。 */
    val kind: SpotKind,
) {
    /** タイプのメタ情報（アイコン・色・表示名）。 */
    val typeMeta: SpotTypeMeta get() = kind.typeMeta(typeKey)
}

/** スポットの大分類。 */
enum class SpotKind { ONSEN, NATURE }

/** スポットタイプの表示メタ（アイコン・色・名前）。iOS 版 OnsenType / NatureSpotType に対応。 */
data class SpotTypeMeta(
    val key: String,
    val name: String,
    /** グリッド等で使う短い名前（温泉タグ用。無指定なら name）。 */
    val tagName: String,
    val icon: ImageVector,
    val color: Color,
)

// MARK: - 温泉タイプ（iOS OnsenType）

/** 温泉タイプ定義。順序は iOS の OnsenType.allCases に合わせる。 */
val ONSEN_TYPES: List<SpotTypeMeta> = listOf(
    SpotTypeMeta("scenic", "絶景", "絶景", Icons.Filled.Landscape, Color(0xFF2980D9)),
    SpotTypeMeta("historical", "歴史", "歴史", Icons.Filled.Star, Color(0xFF996B47)),
    SpotTypeMeta("therapeutic", "療養", "療養", Icons.Filled.Spa, Color(0xFF3D9E66)),
    SpotTypeMeta("resort", "リゾート", "リゾート", Icons.Filled.BeachAccess, Color(0xFF8C61C7)),
    SpotTypeMeta("mountain", "山あい", "山", Icons.Filled.Terrain, Color(0xFFD9732E)),
    SpotTypeMeta("seaside", "海辺", "海", Icons.Filled.Waves, Color(0xFF2194A8)),
    SpotTypeMeta("ski", "スキー", "スキー", Icons.Filled.DownhillSkiing, Color(0xFF38A394)),
)

private val ONSEN_TYPES_BY_KEY = ONSEN_TYPES.associateBy { it.key }
private val ONSEN_TYPE_FALLBACK =
    SpotTypeMeta("therapeutic", "療養", "療養", Icons.Filled.Thermostat, Color(0xFF2194A8))

// MARK: - 自然タイプ（iOS NatureSpotType）

/** 自然スポットタイプ定義。順序は iOS の NatureSpotType.allCases に合わせる。 */
val NATURE_TYPES: List<SpotTypeMeta> = listOf(
    SpotTypeMeta("night_view", "夜景", "夜景", Icons.Filled.AutoAwesome, Color(0xFFFFCC00)),
    SpotTypeMeta("starry_sky", "星空", "星空", Icons.Filled.NightsStay, Color(0xFF5856D6)),
    SpotTypeMeta("sea", "海", "海", Icons.Filled.Waves, Color(0xFF89DAFF)),
    SpotTypeMeta("camping", "キャンプ", "キャンプ", Icons.Filled.Forest, Color(0xFF34C759)),
)

private val NATURE_TYPES_BY_KEY = NATURE_TYPES.associateBy { it.key }
private val NATURE_TYPE_FALLBACK =
    SpotTypeMeta("night_view", "自然", "自然", Icons.Filled.Photo, Color(0xFF3D9E66))

/** この分類で選べるタイプ一覧。 */
fun SpotKind.allTypes(): List<SpotTypeMeta> = when (this) {
    SpotKind.ONSEN -> ONSEN_TYPES
    SpotKind.NATURE -> NATURE_TYPES
}

/** タイプ生キーからメタ情報を解決する。 */
fun SpotKind.typeMeta(key: String): SpotTypeMeta = when (this) {
    SpotKind.ONSEN -> ONSEN_TYPES_BY_KEY[key] ?: ONSEN_TYPE_FALLBACK
    SpotKind.NATURE -> NATURE_TYPES_BY_KEY[key] ?: NATURE_TYPE_FALLBACK
}

// MARK: - リポジトリ（全スポットの一覧・グルーピング）

/**
 * 温泉／自然の全スポットを一度だけ組み立てて保持する。
 * iOS 版 OnsenDataRepository / NatureSpotDataRepository に対応。
 */
object SpotRepository {

    /** 全温泉スポット（ONSEN_INFO をフラット化）。 */
    val allOnsens: List<RouletteSpot> by lazy {
        PREFECTURES_WITH_ONSEN.flatMap { pref ->
            pref.onsens.map { o ->
                RouletteSpot(
                    id = "onsen-${pref.name}-${o.name}-${o.type}",
                    name = o.name,
                    description = o.description,
                    typeKey = o.type,
                    popularity = o.popularity,
                    prefecture = pref,
                    latitude = o.latitude,
                    longitude = o.longitude,
                    kind = SpotKind.ONSEN,
                )
            }
        }
    }

    /** 全自然スポット（NATURE_INFO をフラット化）。 */
    val allNatureSpots: List<RouletteSpot> by lazy {
        PREFECTURES_WITH_NATURE.flatMap { pref ->
            pref.natureSpots.map { s ->
                RouletteSpot(
                    id = "nature-${pref.name}-${s.name}-${s.type}",
                    name = s.name,
                    description = s.description,
                    typeKey = s.type,
                    popularity = s.popularity,
                    prefecture = pref,
                    latitude = s.latitude,
                    longitude = s.longitude,
                    kind = SpotKind.NATURE,
                )
            }
        }
    }

    /** 分類ごとの全スポット。 */
    fun all(kind: SpotKind): List<RouletteSpot> = when (kind) {
        SpotKind.ONSEN -> allOnsens
        SpotKind.NATURE -> allNatureSpots
    }

    /**
     * 地方ごとにグループ化して REGION_ORDER 順で返す。
     * iOS 版 getOnsensByRegion 相当（各地方内はスポット名でソート）。
     */
    fun groupedByRegion(spots: List<RouletteSpot>): List<Pair<String, List<RouletteSpot>>> {
        val byRegion = spots.groupBy { it.prefecture.regionKey }
        return Prefecture.REGION_ORDER.mapNotNull { key ->
            val list = byRegion[key]?.sortedBy { it.name }
            if (list.isNullOrEmpty()) null else Prefecture.regionNameFor(key) to list
        }
    }

    /**
     * タイプごとにグループ化して定義順で返す。
     * iOS 版 getOnsensByType / spotsByType 相当（各タイプ内はスポット名でソート）。
     */
    fun groupedByType(kind: SpotKind, spots: List<RouletteSpot>): List<Pair<SpotTypeMeta, List<RouletteSpot>>> {
        val byType = spots.groupBy { it.typeKey }
        return kind.allTypes().mapNotNull { meta ->
            val list = byType[meta.key]?.sortedBy { it.name }
            if (list.isNullOrEmpty()) null else meta to list
        }
    }
}
