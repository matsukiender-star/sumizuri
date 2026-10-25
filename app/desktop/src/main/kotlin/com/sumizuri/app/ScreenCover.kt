package com.sumizuri.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.sumizuri.app.playback.FullscreenControl
import java.awt.GraphicsEnvironment

// Where the window was before it covered the screen, so it goes back there after.
private class Before(val placement: WindowPlacement, val size: DpSize, val position: WindowPosition)

// The whole screen for the player. On Windows the window without a bar is laid over its screen, as the fullscreen mode of Java there leaves the window black for good once another window takes the focus, such as the tool for a screenshot. Other systems keep their own fullscreen.
@Composable
internal fun rememberScreenCover(window: ComposeWindow, state: WindowState, native: Boolean): FullscreenControl {
    var before by remember { mutableStateOf<Before?>(null) }
    if (native) {
        val fullscreen = state.placement == WindowPlacement.Fullscreen
        return remember(fullscreen) {
            FullscreenControl(fullscreen) {
                state.placement = if (fullscreen) WindowPlacement.Floating else WindowPlacement.Fullscreen
            }
        }
    }
    return remember(before) {
        FullscreenControl(before != null) {
            val back = before
            if (back == null) {
                val placement = state.placement
                if (placement != WindowPlacement.Floating) state.placement = WindowPlacement.Floating
                before = Before(placement, state.size, state.position)
                val bounds = window.graphicsConfiguration.bounds
                state.position = WindowPosition(bounds.x.dp, bounds.y.dp)
                state.size = DpSize(bounds.width.dp, bounds.height.dp)
            } else {
                state.size = back.size
                state.position = back.position
                if (back.placement == WindowPlacement.Maximized) {
                    window.maximizedBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
                    state.placement = WindowPlacement.Maximized
                }
                before = null
            }
        }
    }
}
