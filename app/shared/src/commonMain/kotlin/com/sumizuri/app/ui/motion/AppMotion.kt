package com.sumizuri.app.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.runtime.Composable
import com.sumizuri.app.ui.theme.LocalReduceMotion
import com.sumizuri.app.ui.theme.LocalThemeSpec

// The lengths of the movements of the app in milliseconds and the curves they follow, the same as the Flutter app has.
object AppMotion {
    const val Micro = 100
    const val Fast = 160
    const val Medium = 260
    const val Page = 320

    // A quick start that settles slowly, for what comes in.
    val Entrance = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val Exit = EaseOutCubic
    val Leave = EaseInCubic
}

// A length of time changed by the speed the theme sets, and none at all when the person asked for less motion.
@Composable
fun motionMs(base: Int): Int {
    if (LocalReduceMotion.current) return 0
    return (
        base / LocalThemeSpec.current.options.motion.transitionSpeed
            .coerceIn(0.5f, 2f)
        ).toInt()
}
