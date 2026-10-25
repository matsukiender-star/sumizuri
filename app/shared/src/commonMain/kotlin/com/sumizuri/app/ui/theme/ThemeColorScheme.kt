package com.sumizuri.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.sumizuri.app.ui.theme.spec.AppThemeSpec
import com.sumizuri.app.ui.theme.spec.ThemeColors
import com.sumizuri.app.ui.theme.spec.ThemePresets
import com.sumizuri.app.ui.theme.spec.toColor

// Text on a color: dark with a trace of the color when the color is light, and light when it is dark.
private fun onColor(color: Color): Color = if (color.luminance() >
    0.4f
) {
    lerp(Color.Black, color, 0.12f)
} else {
    lerp(Color.White, color, 0.08f)
}

// The six colors of a role set, each from the theme or else from the first built in theme, which is the one that has them all.
private fun resolved(colors: ThemeColors, fallback: ThemeColors, role: String): Color = (
    colors[role] ?: fallback[role]
        ?: 0xFF808080
    ).toColor()

// The whole color scheme of a theme in the light or dark mode, worked out from its six colors: the surfaces step away from the page color toward the text color, the containers lean toward their color, and every text color is the one that reads on what it sits on.
internal fun AppThemeSpec.colorScheme(dark: Boolean): ColorScheme {
    val own = if (dark) this.dark else light
    val base = ThemePresets.first().let { if (dark) it.dark else it.light }
    val black = dark && options.effects.oledDark
    val page = if (black) Color.Black else resolved(own, base, "surface")
    val primary = resolved(own, base, "primary")
    val primaryContainer = resolved(own, base, "primaryContainer")
    val secondary = resolved(own, base, "secondary")
    val tertiary = resolved(own, base, "tertiary")
    val error = resolved(own, base, "error")
    val ink = if (dark) lerp(Color.White, page, 0.1f) else lerp(Color.Black, page, 0.1f)
    fun step(by: Float): Color = lerp(lerp(page, primary, 0.015f), ink, by)
    fun tint(color: Color, by: Float): Color = lerp(page, color, by)
    val containerBy = if (dark) 0.22f else 0.16f
    val surfaces = if (dark) listOf(0.03f, 0.06f, 0.095f, 0.13f) else listOf(0.025f, 0.05f, 0.075f, 0.1f)
    val secondaryContainer = tint(secondary, containerBy)
    val tertiaryContainer = tint(tertiary, containerBy)
    val errorContainer = tint(error, 0.25f)
    return (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = primary,
        onPrimary = onColor(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = onColor(primaryContainer),
        secondary = secondary,
        onSecondary = onColor(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onColor(secondaryContainer),
        tertiary = tertiary,
        onTertiary = onColor(tertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onColor(tertiaryContainer),
        error = error,
        onError = onColor(error),
        errorContainer = errorContainer,
        onErrorContainer = onColor(errorContainer),
        background = page,
        onBackground = ink,
        surface = page,
        onSurface = ink,
        surfaceVariant = step(surfaces[3]),
        onSurfaceVariant = lerp(ink, page, 0.35f),
        surfaceTint = primary,
        outline = lerp(ink, page, 0.55f),
        outlineVariant = lerp(ink, page, 0.8f),
        surfaceContainerLowest = if (dark) lerp(page, Color.Black, 0.35f) else lerp(page, Color.White, 0.7f),
        surfaceContainerLow = step(surfaces[0]),
        surfaceContainer = step(surfaces[1]),
        surfaceContainerHigh = step(surfaces[2]),
        surfaceContainerHighest = step(surfaces[3]),
    )
}
