package com.sumizuri.app.feature.library.top

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

// How far a swipe must go, in pixels, to change the shelf.
private const val Reach = 140f

// A swipe to the left opens the next category or shelf and a swipe to the right the one before, going round at the ends. It does nothing while titles are picked or when there is only one tab.
internal fun <T> Modifier.swipeShelves(order: List<T>, current: T, enabled: Boolean, onSelect: (T) -> Unit): Modifier =
    if (!enabled ||
        order.size < 2
    ) {
        this
    } else {
        pointerInput(order, current) {
            var drag = 0f
            detectHorizontalDragGestures(
                onDragStart = { drag = 0f },
                onDragEnd = {
                    if (abs(drag) >= Reach) {
                        val at = order.indexOf(current).coerceAtLeast(0)
                        onSelect(order[(at + (if (drag < 0) 1 else order.size - 1)) % order.size])
                    }
                },
            ) { _, amount -> drag += amount }
        }
    }
