package com.sumizuri.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.sumizuri.app.ui.theme.spec.AccentSource
import com.sumizuri.app.ui.theme.spec.AppThemeSpec

// The look of the app from the theme in use: its colors in the light or dark mode, the corners of its pieces and its fonts. Pieces with a look of their own beyond those read the theme from LocalThemeSpec.
@Composable
fun SumizuriTheme(
    mode: ThemeMode,
    themeSpec: AppThemeSpec,
    oled: Boolean = false,
    oledGlow: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    // A theme can take its accent from the system or from the time of day instead of its own colors.
    val seed = when (themeSpec.options.accentFrom) {
        AccentSource.System -> rememberSystemAccent()
        AccentSource.Clock -> rememberClockAccent()
        AccentSource.Theme -> null
    }
    // On a black page the theme is drawn without what would light the screen, unless the person keeps it.
    val spec = remember(themeSpec, dark, oled, oledGlow, seed) {
        val accented = seed?.let { themeSpec.withAccent(it) } ?: themeSpec
        if (dark && oled) accented.onBlack(oledGlow) else accented
    }
    val scheme = remember(spec, dark) { spec.colorScheme(dark) }
    val shapes = remember(spec.shapes) { spec.shapes.toMaterial() }
    CompositionLocalProvider(LocalThemeSpec provides spec) {
        val shown = animatedColorScheme(scheme)
        MaterialTheme(colorScheme = shown, shapes = shapes, typography = themedTypography(spec.options.typography)) {
            // Text that is not on a Material surface takes the color of the page text, since the default would be black.
            CompositionLocalProvider(LocalContentColor provides shown.onBackground, content = content)
        }
    }
}
