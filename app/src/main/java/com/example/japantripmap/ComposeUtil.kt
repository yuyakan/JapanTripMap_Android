package com.example.japantripmap

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier

/** enabled のときだけクリック可能にする Modifier ヘルパー。 */
fun Modifier.clickableIf(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) this.clickable(onClick = onClick) else this
