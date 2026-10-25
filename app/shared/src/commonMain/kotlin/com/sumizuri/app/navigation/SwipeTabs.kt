package com.sumizuri.app.navigation

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

// How far a finger must travel sideways before it changes the tab.
private const val SwipeDp = 72f

// Lets a sideways swipe over the page step to the next or the previous tab. A page that scrolls sideways uses the swipe first, and it is left out when the tab gives a swipe a meaning of its own.
@Composable
internal fun Modifier.swipeTabs(
    enabled: Boolean,
    tabs: List<Destination>,
    selected: Destination,
    onSelect: (Destination) -> Unit,
): Modifier {
    if (!enabled) return this
    val threshold = with(LocalDensity.current) { SwipeDp.dp.toPx() }
    val now by rememberUpdatedState(selected)
    val list by rememberUpdatedState(tabs)
    val pick by rememberUpdatedState(onSelect)
    return pointerInput(Unit) {
        var travelled = 0f
        detectHorizontalDragGestures(
            onDragStart = { travelled = 0f },
            onDragEnd = {
                val at = list.indexOf(now)
                val next = when {
                    travelled <= -threshold -> at + 1
                    travelled >= threshold -> at - 1
                    else -> at
                }
                if (next != at && next in list.indices) pick(list[next])
            },
            onHorizontalDrag = { _, amount -> travelled += amount },
        )
    }
}
