package com.sumizuri.app.ui.kit.scroll

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf

// Where a centered page sends a scroll that lands in its empty margins. The list inside tells the page about itself by registering.
internal val LocalScrollTarget = compositionLocalOf<((ScrollableState?) -> Unit)?> { null }

// Makes a list the one that scrolls when the wheel or a finger moves in the empty space next to it.
@Composable
fun RegisterScrollTarget(state: ScrollableState) {
    val register = LocalScrollTarget.current
    DisposableEffect(state, register) {
        register?.invoke(state)
        ScrollTops.add(state)
        onDispose {
            register?.invoke(null)
            ScrollTops.remove(state)
        }
    }
}
