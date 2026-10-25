package com.sumizuri.app.ui.kit.scroll

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

// A column of rows that are put in order by dragging. Each row is given the modifier for its handle, which starts the drag, and the one for the row itself, which lifts it while it is held. Past half a row the held row takes the place of its neighbour, and the new order is handed over once it is let go.
@Composable
fun <T> DragReorderColumn(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    gap: Dp = 8.dp,
    row: @Composable (item: T, lifted: Boolean, handle: Modifier, rowModifier: Modifier) -> Unit,
) {
    // The order while a row is held, which is the list again when something else changes it.
    var order by remember(items) { mutableStateOf(items) }
    var held by remember { mutableStateOf<Any?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableFloatStateOf(0f) }
    val space = with(LocalDensity.current) { gap.toPx() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        for (item in order) {
            val id = key(item)
            key(id) {
                val lifted = held == id
                val handle = Modifier.pointerInput(id) {
                    detectDragGestures(
                        onDragStart = {
                            held = id
                            offset = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            offset += amount.y
                            val at = order.indexOfFirst { key(it) == id }
                            val step = rowHeight + space
                            if (offset > step / 2 && at < order.lastIndex) {
                                order = order.moved(at, at + 1)
                                offset -= step
                            } else if (offset < -step / 2 && at > 0) {
                                order = order.moved(at, at - 1)
                                offset += step
                            }
                        },
                        onDragEnd = {
                            held = null
                            offset = 0f
                            onReorder(order)
                        },
                        onDragCancel = {
                            held = null
                            offset = 0f
                            onReorder(order)
                        },
                    )
                }
                val rowModifier = Modifier
                    .onSizeChanged {
                        rowHeight = it.height.toFloat()
                    }.zIndex(if (lifted) 1f else 0f)
                    .graphicsLayer {
                        translationY =
                            if (lifted) offset else 0f
                    }
                row(item, lifted, handle, rowModifier)
            }
        }
    }
}

// The list with the item at one place moved to another.
private fun <T> List<T>.moved(from: Int, to: Int): List<T> =
    toMutableList().also { it.add(to.coerceIn(0, size - 1), it.removeAt(from)) }
