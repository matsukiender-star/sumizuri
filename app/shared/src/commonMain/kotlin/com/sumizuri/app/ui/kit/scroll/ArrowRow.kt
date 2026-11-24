package com.sumizuri.app.ui.kit.scroll

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// How much of the visible width one arrow moves the row, so a cover cut at the edge shows whole after the move.
private const val StepShare = 0.8f

// A row that scrolls sideways with arrows at its two ends for a mouse, which cannot swipe it. They show while the pointer is over the row, each only while there is more that way. A finger never sees them.
@Composable
fun ArrowRow(state: LazyListState, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var hovered by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val step: (Float) -> Unit = { sign ->
        scope.launch { state.animateScrollBy(sign * state.layoutInfo.viewportSize.width * StepShare) }
    }
    Box(
        modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.none { it.type == PointerType.Mouse }) continue
                    when (event.type) {
                        PointerEventType.Enter, PointerEventType.Move -> hovered = true
                        PointerEventType.Exit -> hovered = false
                        else -> Unit
                    }
                }
            }
        },
    ) {
        content()
        Arrow(hovered && state.canScrollBackward, Alignment.CenterStart, Icons.AutoMirrored.Filled.KeyboardArrowLeft) {
            step(-1f)
        }
        Arrow(
            hovered && state.canScrollForward,
            Alignment.CenterEnd,
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
        ) { step(1f) }
    }
}

@Composable
private fun BoxScope.Arrow(shown: Boolean, place: Alignment, icon: ImageVector, onClick: () -> Unit) {
    AnimatedVisibility(shown, Modifier.align(place), enter = fadeIn(), exit = fadeOut()) {
        Surface(
            onClick = onClick,
            modifier = Modifier.padding(4.dp).size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.94f),
            shadowElevation = 3.dp,
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(26.dp)) }
        }
    }
}
