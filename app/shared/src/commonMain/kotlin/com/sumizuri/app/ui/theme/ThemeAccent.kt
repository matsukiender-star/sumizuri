package com.sumizuri.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.sumizuri.app.ui.theme.spec.AppThemeSpec
import com.sumizuri.app.ui.theme.spec.ThemeColors
import com.sumizuri.app.ui.theme.spec.ThemePresets
import com.sumizuri.app.ui.theme.spec.toColor
import com.sumizuri.app.ui.theme.spec.toLong
import kotlin.math.max
import kotlin.math.min

// The hue from 0 to 360, the saturation and the value from 0 to 1 of a color.
internal fun hsvOf(color: Color): Triple<Float, Float, Float> {
    val top = max(color.red, max(color.green, color.blue))
    val bottom = min(color.red, min(color.green, color.blue))
    val spread = top - bottom
    val hue = when {
        spread == 0f -> 0f
        top == color.red -> (((color.green - color.blue) / spread) % 6f + 6f) % 6f * 60f
        top == color.green -> ((color.blue - color.red) / spread + 2f) * 60f
        else -> ((color.red - color.green) / spread + 4f) * 60f
    }
    return Triple(hue, if (top == 0f) 0f else spread / top, top)
}

// The same color with its hue turned by some degrees.
internal fun Color.turned(degrees: Float): Color {
    val (hue, saturation, value) = hsvOf(this)
    return Color.hsv(((hue + degrees) % 360f + 360f) % 360f, saturation, value)
}

// A color of a role as the theme draws it in a mode, its own or else the one of the first built in theme.
internal fun AppThemeSpec.roleColor(dark: Boolean, role: String): Color {
    val own = if (dark) this.dark else light
    val base = ThemePresets.first().let { if (dark) it.dark else it.light }
    return (own[role] ?: base[role] ?: 0xFF808080).toColor()
}

// The tint behind chosen chips and highlights, worked out from the page and the accent.
internal fun containerOf(page: Color, accent: Color, dark: Boolean): Color =
    lerp(page, accent, if (dark) 0.3f else 0.22f)

// The theme with one accent in both modes: the accent itself made easy to see on each page, its tint, and a third accent a little round the color wheel for the two accent fills.
internal fun AppThemeSpec.withAccent(seed: Color): AppThemeSpec {
    fun mode(dark: Boolean): ThemeColors {
        val page = roleColor(dark, "surface")
        val accent = readableOn(seed, page)
        return (if (dark) this.dark else light)
            .with("primary", accent.toLong())
            .with("primaryContainer", containerOf(page, accent, dark).toLong())
            .with("tertiary", readableOn(seed.turned(40f), page).toLong())
    }
    return copy(light = mode(false), dark = mode(true))
}
