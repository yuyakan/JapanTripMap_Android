package com.example.japantripmap

import android.content.Intent

/**
 * スクリーンショット撮影モードのリリース版（常に無効の空実装）。
 * 本体は src/debug/.../ScreenshotMode.kt。リリース版にはモック画像もダミー文言も含めない。
 */
object ScreenshotMode {
    val isEnabled: Boolean get() = false

    @Suppress("UNUSED_PARAMETER")
    fun init(intent: Intent?) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun mockThumbnail(url: String, shelf: String? = null, position: Int? = null): Int = 0

    fun title(video: TrendVideo): String = video.title

    fun channel(video: TrendVideo): String = video.channelTitle
}
