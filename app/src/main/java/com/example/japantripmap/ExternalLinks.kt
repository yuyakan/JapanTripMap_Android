package com.example.japantripmap

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/**
 * 施設名で外部アプリ／ブラウザを開くユーティリティ。
 * iOS 版 SocialSearchButtons.swift / AttractionDetailView.openInMaps を移植。
 * URL を開くだけなので地図 SDK は不要。
 */
object ExternalLinks {

    private fun enc(s: String): String =
        URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun open(context: Context, url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }

    /** Google マップを施設名で検索して開く。 */
    fun openMap(context: Context, query: String) {
        open(context, "https://www.google.com/maps/search/?api=1&query=${enc(query)}")
    }

    /** YouTube を施設名で検索して開く。 */
    fun openYouTube(context: Context, query: String) {
        open(context, "https://www.youtube.com/results?search_query=${enc(query)}")
    }

    /** Instagram のハッシュタグ検索を開く（スペース除去でタグ化）。 */
    fun openInstagram(context: Context, query: String) {
        val tag = query.replace(" ", "").replace("　", "")
        open(context, "https://www.instagram.com/explore/tags/${enc(tag)}/")
    }

    /**
     * 食べログをエリア＋キーワードで検索して開く。
     * areaSlug は Prefecture.slug（例: "mie"）。keyword は日本語のグルメ名。
     */
    fun openTabelog(context: Context, areaSlug: String, keyword: String) {
        open(context, "https://tabelog.com/$areaSlug/rstLst/?sw=${enc(keyword)}")
    }
}
