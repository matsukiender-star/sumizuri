package com.sumizuri.app.ui.kit.art

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// The bars and rings that show work going on, in the look of the first run of the app. They take the same words as the bars of Material, so they stand in for them anywhere: a color or a track given is kept, and without one the fill runs through the two accents of the theme. The ends are always round.

// A bar that fills as the work goes. It stands still between changes, so a bar on a page does not draw the window again on every frame.
@Composable
fun InkLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified,
) {
    val fill = inkFill(color)
    val track = inkTrack(trackColor)
    Canvas(modifier.size(240.dp, 4.dp)) {
        val corner = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = corner)
        val reach = size.width * progress().coerceIn(0f, 1f)
        if (reach >
            0f
        ) {
            drawRoundRect(fill, size = Size(reach.coerceAtLeast(size.height), size.height), cornerRadius = corner)
        }
    }
}

// A bar for work of no known length: a lit stretch gliding along the track.
@Composable
fun InkLinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    trackColor: Color = Color.Unspecified,
) {
    val fill = inkFill(color)
    val track = inkTrack(trackColor)
    val transition = rememberInfiniteTransition(label = "bar")
    val glide by transition.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(1_400, easing = FastOutSlowInEasing)),
        label = "glide",
    )
    Canvas(modifier.size(240.dp, 4.dp)) {
        val corner = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = corner)
        val length = size.width * 0.35f
        val start = -length + (size.width + length) * glide
        clipRect {
            drawRoundRect(fill, topLeft = Offset(start, 0f), size = Size(length, size.height), cornerRadius = corner)
        }
    }
}

// A ring that fills as the work goes, from the top. Like the bar, it stands still between changes.
@Composable
fun InkCircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    strokeWidth: Dp = 4.dp,
    trackColor: Color = Color.Unspecified,
) {
    val scheme = MaterialTheme.colorScheme
    val sweep = if (color ==
        Color.Unspecified
    ) {
        Brush.sweepGradient(listOf(scheme.tertiary, scheme.primary, scheme.tertiary))
    } else {
        Brush.linearGradient(listOf(color, color))
    }
    val track = inkTrack(trackColor)
    Canvas(modifier.size(40.dp)) {
        val stroke = strokeWidth.toPx()
        val topLeft = Offset(stroke / 2, stroke / 2)
        val arc = Size(size.width - stroke, size.height - stroke)
        drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
        val share = progress().coerceIn(0f, 1f)
        if (share >
            0.001f
        ) {
            drawArc(sweep, -90f, 360f * share, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

// A ring of light turning round, its tail fading out, as round the icon of the app, for work of no known length.
@Composable
fun InkCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    strokeWidth: Dp = 4.dp,
) {
    val ring = if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color
    val transition = rememberInfiniteTransition(label = "spin")
    val turn by transition.animateFloat(
        0f,
        360f,
        infiniteRepeatable(tween(1_100, easing = LinearEasing)),
        label = "turn",
    )
    Canvas(modifier.size(40.dp)) {
        val stroke = strokeWidth.toPx()
        rotate(turn) {
            drawArc(
                Brush.sweepGradient(listOf(ring.copy(alpha = 0f), ring)),
                0f,
                300f,
                false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun inkFill(color: Color): Brush {
    val scheme = MaterialTheme.colorScheme
    return if (color ==
        Color.Unspecified
    ) {
        Brush.horizontalGradient(listOf(scheme.tertiary, scheme.primary))
    } else {
        Brush.horizontalGradient(listOf(color, color))
    }
}

@Composable
private fun inkTrack(color: Color): Color = if (color ==
    Color.Unspecified
) {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
} else {
    color
}
