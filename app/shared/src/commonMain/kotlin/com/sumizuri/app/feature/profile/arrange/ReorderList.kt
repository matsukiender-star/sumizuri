package com.sumizuri.app.feature.profile.arrange

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sumizuri.app.feature.profile.BlockCalls
import com.sumizuri.app.feature.profile.ShownBlock

private val Gap = 8.dp

// The blocks one under the other, with their actions at the end of each line when there is room. A block is dragged by its handle, and takes the place of the next one once it has gone past half of it, so it stays under the finger the whole way. The new order is kept when it is let go.
@Composable
internal fun ReorderList(
    blocks: List<ShownBlock>,
    picked: String?,
    onPick: (String) -> Unit,
    calls: BlockCalls,
    inlineActions: Boolean,
) {
    val ids = blocks.map { it.block.id }
    var order by remember { mutableStateOf(ids) }
    var dragging by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<String, Int>() }
    val gap = with(LocalDensity.current) { Gap.toPx() }
    LaunchedEffect(ids) { if (dragging == null) order = ids }
    val byId = blocks.associateBy { it.block.id }
    // The drag runs on while the list changes under it, so it reads the saved order and the calls as they are now.
    val savedIds by rememberUpdatedState(ids)
    val currentCalls by rememberUpdatedState(calls)

    // Swaps the dragged block with the one it has gone past half of, and moves its offset by that block, so it does not jump.
    fun follow() {
        val id = dragging ?: return
        val index = order.indexOf(id)
        if (offset > 0 && index < order.lastIndex) {
            val next = order[index + 1]
            val step = (heights[next] ?: 0) + gap
            if (offset > step / 2) {
                order = order.toMutableList().apply { add(index + 1, removeAt(index)) }
                offset -= step
            }
        } else if (offset < 0 && index > 0) {
            val before = order[index - 1]
            val step = (heights[before] ?: 0) + gap
            if (-offset > step / 2) {
                order = order.toMutableList().apply { add(index - 1, removeAt(index)) }
                offset += step
            }
        }
    }

    fun drop() {
        val id = dragging ?: return
        val to = order.indexOf(id)
        if (to != savedIds.indexOf(id)) currentCalls.onMove(id, to)
        dragging = null
        offset = 0f
    }

    Column(verticalArrangement = Arrangement.spacedBy(Gap)) {
        for (id in order) {
            val item = byId[id] ?: continue
            key(id) {
                val lifted = dragging == id
                Box(
                    Modifier.onSizeChanged { heights[id] = it.height }.zIndex(if (lifted) 1f else 0f).graphicsLayer {
                        translationY = if (lifted) offset else 0f
                        if (lifted) {
                            scaleX = 1.02f
                            scaleY = 1.02f
                            shadowElevation = 12f
                        }
                    },
                ) {
                    BlockRow(
                        item,
                        picked = picked == id,
                        lifted = lifted,
                        onPick = { onPick(id) },
                        handle = Modifier.pointerInput(id) {
                            detectDragGestures(
                                onDragStart = {
                                    dragging = id
                                    offset = 0f
                                },
                                onDragEnd = { drop() },
                                onDragCancel = { drop() },
                            ) { change, amount ->
                                change.consume()
                                offset += amount.y
                                follow()
                            }
                        },
                        trailing = { if (inlineActions) BlockActions(item, calls) },
                    )
                }
            }
        }
    }
}
