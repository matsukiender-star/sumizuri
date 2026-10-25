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
import com.sumizuri.app.playback.MiniPlayerControl
import java.awt.Dimension
import java.awt.Toolkit

// How wide the small player is, and how far it sits from the corner of the screen.
private const val MiniWidth = 480
private const val MiniMargin = 24

// The small player on a computer: the window shrinks to the picture in the bottom corner of the screen and stays over the other windows, and goes back to its size and place after. The smallest size of the window is lowered while it is small.
@Composable
internal fun rememberMiniWindow(window: ComposeWindow, state: WindowState, usualMinimum: Dimension): MiniPlayerControl {
    var from by remember { mutableStateOf<Pair<DpSize, WindowPosition>?>(null) }
    return remember(from) {
        MiniPlayerControl(
            active = from != null,
            enter = { aspect ->
                if (from == null) {
                    if (state.placement != WindowPlacement.Floating) state.placement = WindowPlacement.Floating
                    from = state.size to state.position
                    val height = (MiniWidth / aspect.coerceIn(0.5f, 3f)).toInt()
                    window.minimumSize = Dimension(240, 135)
                    val config = window.graphicsConfiguration
                    val bounds = config.bounds
                    val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
                    state.size = DpSize(MiniWidth.dp, height.dp)
                    state.position = WindowPosition(
                        (bounds.x + bounds.width - insets.right - MiniWidth - MiniMargin).dp,
                        (bounds.y + bounds.height - insets.bottom - height - MiniMargin).dp,
                    )
                    window.isAlwaysOnTop = true
                }
            },
            exit = {
                from?.let { (size, place) ->
                    window.isAlwaysOnTop = false
                    window.minimumSize = usualMinimum
                    state.size = size
                    state.position = place
                }
                from = null
            },
        )
    }
}
