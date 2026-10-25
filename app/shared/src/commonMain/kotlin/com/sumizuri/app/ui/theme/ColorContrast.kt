package com.sumizuri.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

// How far apart in brightness an accent and the page must be for the accent to be easy to see, as the web guidelines ask of large text and icons.
internal const val SeenContrast = 3f

// How far apart two colors are in brightness, from 1 for the same to 21 for black on white.
internal fun contrastOf(a: Color, b: Color): Float {
    val one = a.luminance() + 0.05f
    val two = b.luminance() + 0.05f
    return max(one, two) / min(one, two)
}

// The color moved toward white on a dark page or toward black on a light one, only as far as it needs to stand out from the page.
internal fun readableOn(color: Color, page: Color, wanted: Float = SeenContrast): Color {
    if (contrastOf(color, page) >= wanted) return color
    val away = if (page.luminance() < 0.4f) Color.White else Color.Black
    for (step in 1..20) {
        val moved = lerp(color, away, step / 20f)
        if (contrastOf(moved, page) >= wanted) return moved
    }
    return away
}
