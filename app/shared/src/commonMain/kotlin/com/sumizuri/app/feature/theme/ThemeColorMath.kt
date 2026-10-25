package com.sumizuri.app.feature.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.sumizuri.app.ui.theme.containerOf
import com.sumizuri.app.ui.theme.hsvOf
import com.sumizuri.app.ui.theme.readableOn
import com.sumizuri.app.ui.theme.roleColor
import com.sumizuri.app.ui.theme.spec.AppThemeSpec
import com.sumizuri.app.ui.theme.spec.ThemeColors
import com.sumizuri.app.ui.theme.spec.ThemePresets
import com.sumizuri.app.ui.theme.spec.toColor
import com.sumizuri.app.ui.theme.spec.toLong
import kotlin.math.max
import kotlin.math.min

// The colors of one mode made from those of the other: a page of the same hue at the other end of brightness, and every accent moved just enough to stand out on it.
internal fun AppThemeSpec.otherModeFrom(fromDark: Boolean): AppThemeSpec {
    val toDark = !fromDark
    val (hue, saturation, _) = hsvOf(roleColor(fromDark, "surface"))
    val page = if (toDark) {
        Color.hsv(
            hue,
            min(saturation, 0.3f),
            0.07f,
        )
    } else {
        Color.hsv(hue, min(saturation, 0.1f), 0.965f)
    }
    fun moved(role: String): Color = readableOn(roleColor(fromDark, role), page)
    val primary = moved("primary")
    val made = ThemeColors(
        primary = primary.toLong(),
        primaryContainer = containerOf(page, primary, toDark).toLong(),
        secondary = moved("secondary").toLong(),
        tertiary = moved("tertiary").toLong(),
        error = moved("error").toLong(),
        surface = page.toLong(),
    )
    return if (toDark) copy(dark = made) else copy(light = made)
}

// Accents that look good in most themes, offered as one tap choices.
internal val AccentSwatches: List<Color> = listOf(
    0xFF4F46E5, 0xFF7C4DFF, 0xFFC2187A, 0xFFE53950, 0xFFE8702A, 0xFFD4A017,
    0xFF4E7A2E, 0xFF14967F, 0xFF0288D1, 0xFF1F4E8C, 0xFF6B6A80, 0xFF1A1A1A,
).map { Color(it) }

// How the other accents of a palette sit on the color wheel from the one color it is built from.
enum class ColorHarmony(val second: Float, val third: Float) {
    Complementary(180f, 150f),
    Analogous(30f, -30f),
    Triadic(120f, 240f),
    SplitComplementary(150f, 210f),
    Monochrome(0f, 0f),
}

// Both modes built from one color: a page of its hue, the color itself as the main accent, the other accents placed by the harmony, and every accent moved just enough to stand out. Vibrance turns the saturation of the accents down or up.
internal fun AppThemeSpec.paletteFrom(seed: Color, harmony: ColorHarmony, vibrance: Float): AppThemeSpec {
    val (hue, saturation, value) = hsvOf(seed)
    val rich = (saturation * vibrance).coerceIn(0f, 1f)
    fun mode(dark: Boolean): ThemeColors {
        val page = if (dark) Color.hsv(hue, min(rich, 0.3f), 0.07f) else Color.hsv(hue, min(rich, 0.08f), 0.97f)
        fun accent(turn: Float, by: Float, light: Float = value): Color =
            readableOn(Color.hsv(((hue + turn) % 360f + 360f) % 360f, (rich * by).coerceIn(0f, 1f), light), page)
        val primary = accent(0f, 1f)
        val mono = harmony == ColorHarmony.Monochrome
        return ThemeColors(
            primary = primary.toLong(),
            primaryContainer = containerOf(page, primary, dark).toLong(),
            secondary = (
                if (mono) {
                    accent(
                        0f,
                        0.45f,
                        max(value - 0.2f, 0.3f),
                    )
                } else {
                    accent(harmony.second, 0.85f)
                }
                ).toLong(),
            tertiary = (if (mono) accent(0f, 0.7f, min(value + 0.2f, 1f)) else accent(harmony.third, 0.9f)).toLong(),
            error = readableOn(roleColor(dark, "error"), page).toLong(),
            surface = page.toLong(),
        )
    }
    return copy(light = mode(false), dark = mode(true))
}
