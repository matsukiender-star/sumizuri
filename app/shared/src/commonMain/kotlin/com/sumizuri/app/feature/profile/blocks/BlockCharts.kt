package com.sumizuri.app.feature.profile.blocks

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.blocks.BlockView
import com.sumizuri.app.data.blocks.CellShape
import com.sumizuri.app.data.blocks.ChartKind
import com.sumizuri.app.data.blocks.ChartStyle
import com.sumizuri.app.data.blocks.Point
import com.sumizuri.app.data.blocks.numberText
import com.sumizuri.app.data.blocks.weekdayIndex
import org.jetbrains.compose.resources.stringResource
import kotlin.math.sqrt

// The most points a chart draws, so a long series stays readable. top() keeps the biggest on purpose.
private const val MostPoints = 40

// How tall the area of a chart that goes up is when its script does not say.
private const val ChartHeight = 110

// The label of a point in the language of the app.
@Composable
internal fun Point.text(): String = name?.let { stringResource(it) } ?: label

// A short form of a label under a narrow column: the first three letters, or the first character of a word written in kanji or kana.
private fun short(label: String): String = if (label.firstOrNull()?.let { it.code > 0x2E80 } == true) label.take(1) else label.take(3)

// The top of the scale: what the script set, or the biggest value.
internal fun ChartStyle.top(points: List<Point>): Double = (max ?: points.maxOf { it.value }).coerceAtLeast(1e-9)

@Composable
internal fun ChartView(view: BlockView.Chart, modifier: Modifier) {
    val points = view.points.take(MostPoints)
    val style = view.style
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ViewLabel(view.label)
        if (points.isEmpty()) {
            Text("—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            return@Column
        }
        when (style.kind) {
            ChartKind.Bars -> Bars(points, style)
            ChartKind.Columns -> Columns(points, style)
            ChartKind.Line, ChartKind.Area -> Line(points, style, filled = style.kind == ChartKind.Area)
            ChartKind.Dots -> Dots(points, style)
            ChartKind.Donut, ChartKind.Pie -> Slices(points, style, whole = style.kind == ChartKind.Pie)
            ChartKind.Radial -> Radial(points, style)
            ChartKind.Share -> Share(points, style)
        }
    }
}

// A bar across for each point, longest for the biggest, with its label before and its value after.
@Composable
private fun Bars(points: List<Point>, style: ChartStyle) {
    val top = style.top(points)
    val palette = chartPalette(style.color, style.colors, points.size)
    val oneColor = style.colors.isEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        points.forEachIndexed { index, point ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (style.labels) Text(point.text(), Modifier.weight(0.38f), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box(Modifier.weight(0.62f).height(12.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth((point.value / top).toFloat().coerceIn(0.02f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (oneColor) palette[0] else palette[index]),
                    )
                }
                if (style.values != false) Text(numberText(point.value), Modifier.width(36.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, maxLines = 1)
            }
        }
    }
}

// A column up for each point, with its label under it and its value over it when asked. Many columns leave out the labels but the first and the last.
@Composable
private fun Columns(points: List<Point>, style: ChartStyle) {
    val top = style.top(points)
    val palette = chartPalette(style.color, style.colors, points.size)
    val oneColor = style.colors.isEmpty()
    val every = points.size <= 12
    Column {
        Row(Modifier.fillMaxWidth().height((style.height ?: ChartHeight).dp), horizontalArrangement = Arrangement.spacedBy(if (every) 6.dp else 2.dp), verticalAlignment = Alignment.Bottom) {
            points.forEachIndexed { index, point ->
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (style.values == true) Text(numberText(point.value), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight((point.value / top).toFloat().coerceIn(0.02f, 1f) * 0.9f)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (oneColor) palette[0] else palette[index]),
                    )
                }
            }
        }
        if (style.labels) AxisLabels(points, every)
    }
}

@Composable
private fun AxisLabels(points: List<Point>, every: Boolean) {
    val style = MaterialTheme.typography.labelSmall
    val color = MaterialTheme.colorScheme.outline
    if (every) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (point in points) Text(short(point.text()), Modifier.weight(1f), style = style, color = color, textAlign = TextAlign.Center, maxLines = 1)
        }
    } else {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(points.first().text(), style = style, color = color)
            Box(Modifier.weight(1f))
            Text(points.last().text(), style = style, color = color)
        }
    }
}

// A line through the points from left to right, straight or curved, with dots on it, and the space under it filled lightly, or fully for an area.
@Composable
private fun Line(points: List<Point>, style: ChartStyle, filled: Boolean) {
    val ink = viewColor(style.color)
    val top = style.top(points)
    val dots = points.size <= 24
    Column {
        Canvas(Modifier.fillMaxWidth().height((style.height ?: ChartHeight).dp)) {
            val inset = 4.dp.toPx()
            val step = if (points.size > 1) (size.width - inset * 2) / (points.size - 1) else 0f
            fun at(index: Int) = Offset(inset + index * step, size.height - inset - (points[index].value / top).toFloat().coerceIn(0f, 1f) * (size.height - inset * 2))
            val line = Path().apply {
                moveTo(at(0).x, at(0).y)
                for (index in 1 until points.size) {
                    val from = at(index - 1)
                    val to = at(index)
                    if (style.smooth) cubicTo((from.x + to.x) / 2, from.y, (from.x + to.x) / 2, to.y, to.x, to.y) else lineTo(to.x, to.y)
                }
            }
            val under = Path().apply {
                addPath(line)
                lineTo(at(points.lastIndex).x, size.height)
                lineTo(at(0).x, size.height)
                close()
            }
            drawPath(under, Brush.verticalGradient(if (filled) listOf(ink.copy(alpha = 0.55f), ink.copy(alpha = 0.05f)) else listOf(ink.copy(alpha = 0.16f), Color.Transparent)))
            drawPath(line, ink, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            if (dots && !filled) points.indices.forEach { drawCircle(ink, 3.dp.toPx(), at(it)) }
        }
        if (style.labels) AxisLabels(points, every = false)
    }
}

// A thin line across for each point with a dot where its value is, and the value after it.
@Composable
private fun Dots(points: List<Point>, style: ChartStyle) {
    val top = style.top(points)
    val palette = chartPalette(style.color, style.colors, points.size)
    val oneColor = style.colors.isEmpty()
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        points.forEachIndexed { index, point ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (style.labels) Text(point.text(), Modifier.weight(0.38f), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val ink = if (oneColor) palette[0] else palette[index]
                Canvas(Modifier.weight(0.62f).height(14.dp)) {
                    val y = size.height / 2
                    val x = (point.value / top).toFloat().coerceIn(0f, 1f) * (size.width - size.height) + size.height / 2
                    drawLine(track, Offset(0f, y), Offset(size.width, y), 2.dp.toPx(), StrokeCap.Round)
                    drawLine(ink, Offset(0f, y), Offset(x, y), 2.dp.toPx(), StrokeCap.Round)
                    drawCircle(ink, size.height / 2.6f, Offset(x, y))
                }
                if (style.values != false) Text(numberText(point.value), Modifier.width(36.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, maxLines = 1)
            }
        }
    }
}

// The days of the last weeks as cells, a column for each week from Monday down, darker for a heavier day. A cell is a square, a rounded square or a circle.
@Composable
internal fun HeatmapView(view: BlockView.Heatmap, modifier: Modifier) {
    val empty = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val ink = viewColor(view.color)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ViewLabel(view.label)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val weeks = view.weeks.coerceIn(4, 53)
            val start = view.today - ((weeks - 1) * 7 + weekdayIndex(view.today))
            val counts = (start..view.today).map { view.daily[it] ?: 0 }
            val peak = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
            Canvas(Modifier.fillMaxWidth().aspectRatio(weeks / 7f)) {
                val cell = size.width / weeks
                val gap = cell * 0.18f
                counts.forEachIndexed { index, count ->
                    val color = if (count == 0) empty else ink.copy(alpha = 0.25f + 0.75f * sqrt(count.toFloat() / peak))
                    val corner = Offset((index / 7) * cell + gap / 2, (index % 7) * cell + gap / 2)
                    val side = cell - gap
                    when (view.shape) {
                        CellShape.Circle -> drawCircle(color, side / 2, Offset(corner.x + side / 2, corner.y + side / 2))
                        else -> drawRoundRect(color, corner, Size(side, side), CornerRadius(if (view.shape == CellShape.Round) cell * 0.2f else 0f))
                    }
                }
            }
        }
    }
}
