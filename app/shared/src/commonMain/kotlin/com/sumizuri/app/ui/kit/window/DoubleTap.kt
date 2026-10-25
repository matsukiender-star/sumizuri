package com.sumizuri.app.ui.kit.window

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.sumizuri.app.data.text.nowMillis

// Two presses this close together are a double tap.
private const val DoubleTapMs = 350L

// Calls back on a double tap without taking the press from anyone, so the drag area of the window under it still sees every press and the window can still be moved.
internal fun Modifier.onDoubleTap(action: () -> Unit): Modifier = pointerInput(Unit) {
    var last = 0L
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type != PointerEventType.Press) continue
            val now = nowMillis()
            if (now - last <= DoubleTapMs) {
                last = 0L
                action()
            } else {
                last = now
            }
        }
    }
}
