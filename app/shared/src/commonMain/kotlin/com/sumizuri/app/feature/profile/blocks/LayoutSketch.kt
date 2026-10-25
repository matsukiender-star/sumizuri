package com.sumizuri.app.feature.profile.blocks

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// A block in a sketch: its columns, the rows it is drawn with, and whether it is a part of the profile as it came, which is drawn fainter.
internal class SketchPart(val w: Int, val rows: Int, val faint: Boolean)

// The blocks of a page as rounded boxes on four columns, placed the way the page places them and shrunk to fit. The highlighted one is filled stronger and ringed.
@Composable
internal fun LayoutSketch(parts: List<SketchPart>, modifier: Modifier, highlight: Int? = null) {
    val ink = MaterialTheme.colorScheme.primary
    val faint = MaterialTheme.colorScheme.onSurface
    val spots = placeInOrder(parts.map { it.w }, { parts[it].rows })
    val rows = (parts.indices.maxOfOrNull { spots[it].second + parts[it].rows } ?: 1).coerceAtLeast(1)
    Canvas(modifier) {
        val gap = 3.dp.toPx()
        val cell = (size.width - gap * (GridColumns - 1)) / GridColumns
        val row = minOf(cell * 0.55f, (size.height - gap * (rows - 1)) / rows)
        parts.forEachIndexed { index, part ->
            val (x, y) = spots[index]
            val lit = index == highlight
            val color = when {
                lit -> ink
                part.faint -> faint.copy(alpha = 0.18f)
                highlight != null -> ink.copy(alpha = 0.3f)
                else -> ink.copy(alpha = 0.55f)
            }
            val corner = Offset(x * (cell + gap), y * (row + gap))
            val box = Size(cell * part.w + gap * (part.w - 1), row * part.rows + gap * (part.rows - 1))
            drawRoundRect(color, corner, box, CornerRadius(3.dp.toPx()))
            if (lit) drawRoundRect(faint, corner, box, CornerRadius(3.dp.toPx()), style = Stroke(1.5.dp.toPx()))
        }
    }
}
