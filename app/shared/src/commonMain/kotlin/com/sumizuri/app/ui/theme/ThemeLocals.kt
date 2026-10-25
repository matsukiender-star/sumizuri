package com.sumizuri.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.sumizuri.app.ui.theme.spec.ThemePresets

// The theme in use, for every piece that has a look of its own beyond the Material colors and shapes.
val LocalThemeSpec = compositionLocalOf { ThemePresets.first() }

// Whether list rows are drawn thin, with less room around them, so more fit on the screen.
val LocalCompactLists = compositionLocalOf { false }

// Whether the app changes between pages at once, with no fades.
val LocalReduceMotion = compositionLocalOf { false }
