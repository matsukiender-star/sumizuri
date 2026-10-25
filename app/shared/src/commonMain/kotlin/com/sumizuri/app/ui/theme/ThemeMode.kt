package com.sumizuri.app.ui.theme

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.theme_dark
import com.sumizuri.app.shared.resources.theme_light
import com.sumizuri.app.shared.resources.theme_system
import com.sumizuri.app.ui.theme.spec.AppThemeSpec
import org.jetbrains.compose.resources.StringResource

enum class ThemeMode(val label: StringResource) {
    System(Res.string.theme_system),
    Light(Res.string.theme_light),
    Dark(Res.string.theme_dark),
}

// The theme as it is drawn on a black page. The page is true black, and unless the glow is kept, the ink behind every page is left out so the pixels stay off.
internal fun AppThemeSpec.onBlack(keepGlow: Boolean): AppThemeSpec {
    val effects = options.effects.copy(oledDark = true, bloom = if (keepGlow) options.effects.bloom else 0f)
    return copy(options = options.copy(effects = effects))
}
