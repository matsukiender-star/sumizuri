package com.sumizuri.app.ui.kit.card

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

// Where a row stands among the rows of one card: its place and how many there are.
class CardPlace(val index: Int, val count: Int) {
    val first: Boolean get() = index == 0
    val last: Boolean get() = index == count - 1
}

// The shape of a row in a card of many: round at the top for the first, at the bottom for the last, and square in between, so the rows together look like one card.
@Composable
fun cardRowShape(place: CardPlace): Shape {
    val card = CardShape
    val none = CornerSize(0)
    return when {
        place.first && place.last -> card
        place.first -> card.copy(bottomStart = none, bottomEnd = none)
        place.last -> card.copy(topStart = none, topEnd = none)
        else -> RectangleShape
    }
}

// Draws a row as its part of one card that holds a long list, as the sections of the settings look: the flat fill of a card, its border around the outside only, and a thin line between rows that stops short of the sides. A list too long to put in one card, such as the chapters of a title, keeps its rows lazy this way. A tint, such as for a picked row, is laid over the card.
@Composable
fun Modifier.cardRow(place: CardPlace, tint: Color? = null): Modifier {
    val colors = MaterialTheme.colorScheme
    val fill = cardFill()
    val border = cardEdge()
    val divider = colors.outlineVariant.copy(alpha = 0.6f)
    val shape = cardRowShape(place)
    val corner = CardShape
    val borderWidth = 1.dp
    return drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(outline, fill)
        if (tint != null) drawOutline(outline, tint)
        if (!place.first) {
            val inset = 14.dp.toPx()
            drawLine(divider, Offset(inset, 0f), Offset(size.width - inset, 0f), strokeWidth = 1.dp.toPx())
        }
        val width = borderWidth.toPx()
        drawPath(edgePath(place, corner, width), border, style = Stroke(width))
    }
}

// The part of the border of a card that one row draws: both sides, and the rounded top or bottom when the row is at an end. Drawn half a line in from the edge so it stays inside the row.
private fun DrawScope.edgePath(place: CardPlace, corner: CornerBasedShape, width: Float): Path {
    val half = width / 2
    val left = half
    val right = size.width - half
    val top = half
    val bottom = size.height - half
    val radius = corner.topStart.toPx(size, this).coerceAtMost(size.height / 2)
    return Path().apply {
        when {
            place.first && place.last -> addRoundRect(RoundRect(left, top, right, bottom, radius, radius))

            place.first -> {
                moveTo(left, size.height)
                lineTo(left, top + radius)
                arcTo(Rect(left, top, left + radius * 2, top + radius * 2), 180f, 90f, false)
                lineTo(right - radius, top)
                arcTo(Rect(right - radius * 2, top, right, top + radius * 2), 270f, 90f, false)
                lineTo(right, size.height)
            }

            place.last -> {
                moveTo(left, 0f)
                lineTo(left, bottom - radius)
                arcTo(Rect(left, bottom - radius * 2, left + radius * 2, bottom), 180f, -90f, false)
                lineTo(right - radius, bottom)
                arcTo(Rect(right - radius * 2, bottom - radius * 2, right, bottom), 90f, -90f, false)
                lineTo(right, 0f)
            }

            else -> {
                moveTo(left, 0f)
                lineTo(left, size.height)
                moveTo(right, 0f)
                lineTo(right, size.height)
            }
        }
    }
}
