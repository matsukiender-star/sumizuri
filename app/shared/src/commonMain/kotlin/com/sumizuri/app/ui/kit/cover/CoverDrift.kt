package com.sumizuri.app.ui.kit.cover

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.sumizuri.app.ui.theme.LocalThemeSpec
import com.sumizuri.app.ui.theme.background.rememberBackdropClock
import kotlin.math.PI
import kotlin.math.sin

// How many frames a second the drift is drawn at, which is smooth for something this slow.
private const val DriftFps = 20

// How long one sweep of the drift takes, in milliseconds.
private const val SweepMs = 26_000.0

// The cover behind a page drifting and zooming slowly when the theme asks for it, as in a film. It stops while the page is out of sight.
@Composable
fun Modifier.coverDrift(): Modifier {
    val on = LocalThemeSpec.current.options.motion.coverDrift
    val clock by rememberBackdropClock(speed = 1f, fps = DriftFps, still = !on)
    if (!on) return this
    return graphicsLayer {
        val turn = (clock / SweepMs) * 2 * PI
        val zoom = 1.12f + 0.06f * sin(turn).toFloat()
        scaleX = zoom
        scaleY = zoom
        translationX = size.width * 0.035f * sin(turn * 0.7).toFloat()
        translationY = size.height * 0.03f * sin(turn * 0.5 + 1.3).toFloat()
    }
}
