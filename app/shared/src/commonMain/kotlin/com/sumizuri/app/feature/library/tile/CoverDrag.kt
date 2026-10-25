package com.sumizuri.app.feature.library.tile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.data.model.library.Category
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.library_drop_on_shelf
import com.sumizuri.app.ui.kit.cover.CoverArt
import org.jetbrains.compose.resources.stringResource

// Covers being dragged onto a shelf: the titles they carry, where the pointer is on the window, and where each shelf they can be dropped on sits.
@Stable
class CoverDrag {
    var titles by mutableStateOf<List<Title>>(emptyList())
    var at by mutableStateOf(Offset.Zero)
    val shelves = mutableStateMapOf<Long, Rect>()

    val active: Boolean get() = titles.isNotEmpty()

    // The shelf under the pointer, if any.
    fun over(): Long? = shelves.entries.firstOrNull { it.value.contains(at) }?.key
}

// Lets a cover be picked up with a long press and moved onto a shelf without lifting the finger or the button. It carries the titles that are picked when there are any, and itself otherwise.
@Composable
internal fun Modifier.coverDrag(drag: CoverDrag, title: Title, carried: () -> List<Title>, onDrop: (Set<Long>, Long) -> Unit): Modifier {
    var origin by remember { mutableStateOf(Offset.Zero) }
    return onGloballyPositioned { origin = it.positionInRoot() }.pointerInput(title.id) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset ->
                drag.shelves.clear()
                drag.titles = carried().ifEmpty { listOf(title) }
                drag.at = origin + offset
            },
            onDrag = { change, amount ->
                change.consume()
                drag.at += amount
            },
            onDragEnd = {
                drag.over()?.let { shelf -> onDrop(drag.titles.map { it.id }.toSet(), shelf) }
                drag.titles = emptyList()
            },
            onDragCancel = { drag.titles = emptyList() },
        )
    }
}

// What shows while covers are dragged: the first cover under the pointer, and the shelves to drop them on rising from the bottom, with the one under the pointer lit. The place of the box this sits in is taken away, since the pointer is measured on the window.
@Composable
internal fun CoverDragLayer(drag: CoverDrag, shelves: List<Category>, modifier: Modifier = Modifier) {
    if (!drag.active) return
    var corner by remember { mutableStateOf(Offset.Zero) }
    Box(modifier.onGloballyPositioned { corner = it.positionInRoot() }) {
        val first = drag.titles.first()
        Box(
            Modifier
                .width(72.dp)
                .graphicsLayer {
                    val local = drag.at - corner
                    translationX = local.x - size.width / 2
                    translationY = local.y - size.height / 2
                    rotationZ = -4f
                }.shadow(12.dp, MaterialTheme.shapes.small),
        ) {
            CoverArt(first, Modifier.width(72.dp), letterSize = 18)
            if (drag.titles.size > 1) {
                Text(
                    drag.titles.size.toString(),
                    Modifier
                        .padding(4.dp)
                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        FlowRow(
            Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(Res.string.library_drop_on_shelf),
                Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            for (shelf in shelves) {
                val lit = drag.over() == shelf.id
                val grow by animateFloatAsState(if (lit) 1.08f else 1f)
                Text(
                    shelf.name,
                    Modifier
                        .onGloballyPositioned { drag.shelves[shelf.id] = it.boundsInRoot() }
                        .graphicsLayer {
                            scaleX = grow
                            scaleY = grow
                        }.clip(MaterialTheme.shapes.small)
                        .background(if (lit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    color = if (lit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
