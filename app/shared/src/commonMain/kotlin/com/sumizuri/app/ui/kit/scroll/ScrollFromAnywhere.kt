package com.sumizuri.app.ui.kit.scroll

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// How far one notch of the mouse wheel moves the list.
private val WheelStep = 80.dp

// Lets a list be scrolled from the empty space around it. The mouse wheel glides the list the way it does on the list itself, and a finger or a pen dragging over that space moves it as if it were on it. Whatever the list or its rows already use is left to them.
fun Modifier.scrollFrom(target: () -> ScrollableState?): Modifier = this
    .pointerInput(Unit) {
        coroutineScope {
            // The distance still to go, which each notch adds to, played out over a short glide.
            var owed = 0f
            val glide = Animatable(0f)
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type != PointerEventType.Scroll) continue
                    val change = event.changes.firstOrNull() ?: continue
                    if (change.isConsumed) continue
                    val state = target() ?: continue
                    change.consume()
                    owed += change.scrollDelta.y * WheelStep.toPx()
                    val goal = owed
                    launch {
                        var done = glide.value
                        glide.animateTo(goal, tween(160)) {
                            state.dispatchRawDelta(value - done)
                            done = value
                        }
                    }
                }
            }
        }
    }.pointerInput(Unit) {
        detectVerticalDragGestures { change, amount ->
            target()?.dispatchRawDelta(-amount)
            change.consume()
        }
    }
