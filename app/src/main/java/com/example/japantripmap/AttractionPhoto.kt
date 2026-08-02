package com.example.japantripmap

import android.content.Context
import androidx.annotation.DrawableRes

/**
 * 観光スポットの実写写真（CC0）へのアクセサ。
 * iOS 版 AttractionPhoto.swift を移植。画像は res/drawable/attraction_*.jpg。
 * スポットの日本語名から drawable リソース ID を解決する。
 */
object AttractionPhoto {

    /** スポット名に対応する drawable リソース ID。無ければ 0。 */
    @DrawableRes
    fun resId(context: Context, spotName: String): Int {
        val resName = ATTRACTION_PHOTOS[spotName] ?: return 0
        return context.resources.getIdentifier(resName, "drawable", context.packageName)
    }

    /** そのスポットに写真があるか。 */
    fun hasPhoto(context: Context, spotName: String): Boolean = resId(context, spotName) != 0

    /**
     * 県内で「写真を持つ観光スポット」だけを、元の並び順で返す。
     * 県詳細のフォトカルーセル用。
     */
    fun photographedAttractions(context: Context, prefecture: Prefecture): List<Attraction> {
        val info = prefecture.tourismInfo ?: return emptyList()
        return info.attractions.filter { hasPhoto(context, it.name) }
    }
}
