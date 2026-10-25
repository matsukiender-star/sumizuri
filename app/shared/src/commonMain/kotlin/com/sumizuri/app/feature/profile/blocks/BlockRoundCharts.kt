package com.sumizuri.app.feature.profile.blocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.blocks.ChartStyle
import com.sumizuri.app.data.blocks.Point
import com.sumizuri.app.data.blocks.numberText

// How many slices or rings a round chart names, so its key fits beside it. The rest are put together into one.
private const val MostNamed = 6
private const val MostRings = 5

// A ring cut into a slice for each point, or a whole pie, with the biggest few named beside it.
@Composable
internal fun Slices(points: List<Point>, style: ChartStyle, whole: Boolean) {
    val shown = points.take(MostNamed)
    val rest = points.drop(shown.size).sumOf { it.value }
    val values = shown.map { it.value } + listOfNotNull(rest.takeIf { it > 0 })
    val palette = chartPalette(style.color, style.colors, values.size).let {
        if (rest >
            0
        ) {
            it.dropLast(1) + MaterialTheme.colorScheme.outline
        } else {
            it
        }
    }
    val total = values.sum().coerceAtLeast(1e-9)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Canvas(Modifier.size((style.height ?: 96).coerceAtMost(160).dp)) {
            val stroke = size.minDimension * 0.2f
            var start = -90f
            values.forEachIndexed { index, value ->
                val sweep = (value / total * 360).toFloat()
                if (whole) {
                    drawArc(palette[index], start, sweep, useCenter = true)
                    drawArc(
                        Color.Black.copy(alpha = 0.12f),
                        start,
                        sweep,
                        useCenter = true,
                        style = Stroke(1.dp.toPx()),
                    )
                } else {
                    drawArc(
                        palette[index],
                        start,
                        (sweep - 1.5f).coerceAtLeast(0.5f),
                        false,
                        Offset(
                            stroke / 2,
                            stroke / 2,
                        ),
                        Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke),
                    )
                }
                start += sweep
            }
        }
        if (style.labels) Key(shown, palette, style.values != false)
    }
}

// Rings inside each other, one for each point, each going round as far as its share of the top of the scale, from the biggest outside in.
@Composable
internal fun Radial(points: List<Point>, style: ChartStyle) {
    val shown = points.take(MostRings)
    val top = style.top(shown)
    val palette = chartPalette(style.color, style.colors, shown.size)
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Canvas(Modifier.size((style.height ?: 110).coerceAtMost(180).dp)) {
            val ring = size.minDimension / 2 / (shown.size + 1)
            val stroke = ring * 0.72f
            shown.forEachIndexed { index, point ->
                val inset = index * ring + stroke / 2
                val box = Size(size.width - inset * 2, size.height - inset * 2)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), box, style = Stroke(stroke))
                drawArc(
                    palette[index],
                    -90f,
                    300f * (point.value / top).toFloat().coerceIn(0f, 1f),
                    false,
                    Offset(inset, inset),
                    box,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        if (style.labels) Key(shown, palette, style.values != false)
    }
}

// One bar split into a share for each point, with a key under it.
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Share(points: List<Point>, style: ChartStyle) {
    val shown = points.take(MostNamed)
    val rest = points.drop(shown.size).sumOf { it.value }
    val values = shown.map { it.value } + listOfNotNull(rest.takeIf { it > 0 })
    val palette = chartPalette(style.color, style.colors, values.size).let {
        if (rest >
            0
        ) {
            it.dropLast(1) + MaterialTheme.colorScheme.outline
        } else {
            it
        }
    }
    val total = values.sum().coerceAtLeast(1e-9)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().height((style.height ?: 16).coerceIn(8, 48).dp).clip(RoundedCornerShape(6.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            values.forEachIndexed { index, value ->
                if (value >
                    0
                ) {
                    Box(Modifier.weight((value / total).toFloat()).fillMaxHeight().background(palette[index]))
                }
            }
        }
        if (style.labels) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                shown.forEachIndexed { index, point ->
                    KeyLine(
                        point,
                        palette[index],
                        if (style.values !=
                            false
                        ) {
                            "${numberText(point.value / total * 100, 0)}%"
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

// The names of the points of a round chart, each with its colour and, when asked, its value.
@Composable
private fun Key(points: List<Point>, palette: List<Color>, values: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        points.forEachIndexed { index, point ->
            KeyLine(
                point,
                palette[index],
                if (values) numberText(point.value) else null,
            )
        }
    }
}

@Composable
private fun KeyLine(point: Point, color: Color, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(point.text(), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (value !=
            null
        ) {
            Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        }
    }
}
