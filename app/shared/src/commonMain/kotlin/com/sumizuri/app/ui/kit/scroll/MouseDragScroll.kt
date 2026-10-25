package com.sumizuri.app.ui.kit.scroll

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.min

// Lets a list be moved by pressing the mouse on it and dragging, the way a finger does. A mouse does not scroll a list that way by itself. The drag is watched before the rows see it, and once it goes more up or down than sideways it is the list's and the rows that were pressed are let go. A plain click is left to the rows. Pulling down at the top of the list is told to the page, for the refresh.
@Composable
fun Modifier.mouseDragScroll(state: ScrollableState, horizontal: Boolean = false): Modifier {
    val pull = LocalDragPull.current
    return pointerInput(state, horizontal, pull) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.type != PointerType.Mouse) return@awaitEachGesture
            var moved = Offset.Zero
            var dragging = false
            var pulled = 0f
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val delta = change.position - change.previousPosition
                val along = if (horizontal) delta.x else delta.y
                moved += delta
                val main = if (horizontal) moved.x else moved.y
                val cross = if (horizontal) moved.y else moved.x
                if (!dragging && abs(main) > viewConfiguration.touchSlop && abs(main) > abs(cross)) dragging = true
                if (!dragging) continue
                change.consume()
                // A pull down past the top is kept apart from the scroll, and is given back first when the mouse turns around.
                var rest = -along
                if (pull != null && !horizontal) {
                    if (rest > 0f && pulled > 0f) {
                        val back = min(pulled, rest)
                        pulled -= back
                        rest -= back
                    }
                    val used = if (rest != 0f) state.dispatchRawDelta(rest) else 0f
                    val left = rest - used
                    if (left < 0f) pulled += -left
                    pull.moved(pulled)
                } else {
                    state.dispatchRawDelta(rest)
                }
            }
            if (dragging) pull?.released()
        }
    }
}
