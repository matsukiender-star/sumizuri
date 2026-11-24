package com.sumizuri.app.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

// Corner zone size where parking the mouse hides controls and keeps them hidden.
private val CornerHideZone = 48.dp

// Tracks visible control bar rectangles in root coordinates so corner-hiding never traps buttons inside the bars.
internal class PlayerControlZones {
    var topBar: Rect? = null
    var bottomBar: Rect? = null

    fun contains(rootPosition: Offset): Boolean =
        topBar?.contains(rootPosition) == true || bottomBar?.contains(rootPosition) == true

    fun clear() {
        topBar = null
        bottomBar = null
    }
}

// Checks whether a pointer position lies inside any 48dp corner of the player viewport.
internal fun isInCorner(position: Offset, size: IntSize, cornerPx: Float): Boolean {
    if (size.width <= 0 || size.height <= 0) return false
    val nearX = position.x <= cornerPx || position.x >= size.width - cornerPx
    val nearY = position.y <= cornerPx || position.y >= size.height - cornerPx
    return nearX && nearY
}

// Mouse-only player controls: wheel changes volume, leaving the window or entering a corner outside control bars hides controls immediately while playing.
@Composable
internal fun Modifier.playerMouseControls(
    enabled: Boolean,
    canHide: Boolean,
    controlsVisible: Boolean,
    controlZones: PlayerControlZones,
    allowVolume: Boolean,
    onVolumeBy: (Int) -> Unit,
    onShow: () -> Unit,
    onHide: () -> Unit,
): Modifier {
    if (!enabled) return this
    val latestCanHide by rememberUpdatedState(canHide)
    val latestControlsVisible by rememberUpdatedState(controlsVisible)
    val latestAllowVolume by rememberUpdatedState(allowVolume)
    val latestVolumeBy by rememberUpdatedState(onVolumeBy)
    val latestShow by rememberUpdatedState(onShow)
    val latestHide by rememberUpdatedState(onHide)
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    return onGloballyPositioned { hostOrigin = it.positionInRoot() }.pointerInput(controlZones) {
        val cornerPx = CornerHideZone.toPx()
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val mouse = event.changes.firstOrNull { it.type == PointerType.Mouse } ?: continue
                when (event.type) {
                    PointerEventType.Scroll -> {
                        if (latestAllowVolume) latestVolumeBy(if (mouse.scrollDelta.y < 0f) 5 else -5)
                    }

                    PointerEventType.Exit -> {
                        if (latestCanHide) latestHide()
                    }

                    PointerEventType.Move, PointerEventType.Enter -> {
                        val overControls = latestControlsVisible && controlZones.contains(hostOrigin + mouse.position)
                        if (isInCorner(mouse.position, size, cornerPx) && !overControls) {
                            if (latestCanHide) latestHide()
                        } else if (event.type == PointerEventType.Move) {
                            latestShow()
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}
