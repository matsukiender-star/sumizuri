package com.sumizuri.app.ui.kit.input

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

// The slider of the app: a thin rounded track with the chosen part filled, small dots for the steps when there are steps, and a round thumb with a ring that grows while it is held. It works like the slider of Material, keys and screen readers included, and only looks lighter.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InkSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val dragged by source.collectIsDraggedAsState()
    val pressed by source.collectIsPressedAsState()
    val held = dragged || pressed
    val knob by animateDpAsState(if (held) 22.dp else 16.dp)
    val thickness by animateDpAsState(if (held) 6.dp else 4.dp)
    val colors = MaterialTheme.colorScheme
    val fill = if (enabled) colors.primary else colors.onSurface.copy(alpha = 0.38f)
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = source,
        thumb = {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(knob).background(fill, CircleShape).border(2.dp, colors.surface, CircleShape))
            }
        },
        track = { state ->
            val span = state.valueRange.endInclusive - state.valueRange.start
            val share = if (span <= 0f) 0f else ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f)
            Canvas(Modifier.fillMaxWidth().height(24.dp)) {
                val middle = size.height / 2
                val width = thickness.toPx()
                drawLine(
                    colors.surfaceContainerHighest,
                    Offset(0f, middle),
                    Offset(size.width, middle),
                    width,
                    StrokeCap.Round,
                )
                drawLine(fill, Offset(0f, middle), Offset(size.width * share, middle), width, StrokeCap.Round)
                if (state.steps in 1..40) {
                    for (each in 0..state.steps + 1) {
                        val at = each.toFloat() / (state.steps + 1)
                        val dot = if (at <=
                            share
                        ) {
                            colors.onPrimary.copy(alpha = 0.55f)
                        } else {
                            colors.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                        drawCircle(dot, width / 4, Offset(size.width * at, middle))
                    }
                }
            }
        },
    )
}
