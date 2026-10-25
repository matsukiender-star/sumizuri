package com.sumizuri.app.ui.kit.page

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

// The page margin, the same as in the Flutter app.
val PageGutter = 16.dp

// The widest a list may grow before it is centered, the same as in the Flutter app.
val ReadableWidth = 720.dp

// How much room a tab page leaves at its bottom for the floating bar. Pages that open over the tabs have none.
val LocalBottomClearance = compositionLocalOf { 0.dp }

// Asks the frame of the tabs to hide its bar at the bottom, for a page that puts a bar of its own there for a while, such as the actions on picked titles.
val LocalHideBottomBar = compositionLocalOf<(Boolean) -> Unit> { {} }

// As many columns as it takes for no cell to be wider than the maximum, so the cells grow to fill the row, as the Flutter grid does.
class MaxCellWidth(private val max: Dp) : GridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val widest = max.roundToPx()
        val columns = ceil(availableSize.toFloat() / (widest + spacing)).toInt().coerceAtLeast(1)
        val cell = (availableSize - spacing * (columns - 1)) / columns
        // The pixels left over by the division go to the first cells, one each.
        val extra = availableSize - spacing * (columns - 1) - cell * columns
        return List(columns) { index -> if (index < extra) cell + 1 else cell }
    }
}

// One item that takes the whole row of a grid.
fun LazyGridScope.fullSpan(content: @Composable LazyGridItemScope.() -> Unit) =
    item(span = { GridItemSpan(maxLineSpan) }, content = content)
