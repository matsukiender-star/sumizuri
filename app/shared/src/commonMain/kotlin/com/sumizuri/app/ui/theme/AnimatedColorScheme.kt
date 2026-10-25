package com.sumizuri.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.sumizuri.app.ui.motion.AppMotion
import com.sumizuri.app.ui.motion.motionMs

// The color scheme that moves toward a new one over a moment when the theme or the mode changes, so a change of look is a slow turn from one to the other and not a jump. The first scheme shows at once.
@Composable
internal fun animatedColorScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(motionMs(AppMotion.Medium + 140), easing = AppMotion.Exit)

    @Composable
    fun move(color: Color): Color = animateColorAsState(color, spec, label = "scheme").value
    return target.copy(
        primary = move(target.primary), onPrimary = move(target.onPrimary),
        primaryContainer = move(target.primaryContainer), onPrimaryContainer = move(target.onPrimaryContainer),
        secondary = move(target.secondary), onSecondary = move(target.onSecondary),
        secondaryContainer = move(target.secondaryContainer), onSecondaryContainer = move(target.onSecondaryContainer),
        tertiary = move(target.tertiary), onTertiary = move(target.onTertiary),
        tertiaryContainer = move(target.tertiaryContainer), onTertiaryContainer = move(target.onTertiaryContainer),
        error = move(target.error), onError = move(target.onError),
        errorContainer = move(target.errorContainer), onErrorContainer = move(target.onErrorContainer),
        background = move(target.background), onBackground = move(target.onBackground),
        surface = move(target.surface), onSurface = move(target.onSurface),
        surfaceVariant = move(target.surfaceVariant), onSurfaceVariant = move(target.onSurfaceVariant),
        outline = move(target.outline), outlineVariant = move(target.outlineVariant),
        surfaceContainerLowest = move(
            target.surfaceContainerLowest,
        ),
        surfaceContainerLow = move(target.surfaceContainerLow),
        surfaceContainer = move(target.surfaceContainer), surfaceContainerHigh = move(target.surfaceContainerHigh),
        surfaceContainerHighest = move(target.surfaceContainerHighest),
    )
}
