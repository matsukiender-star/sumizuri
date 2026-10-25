package com.sumizuri.app.feature.profile.blocks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.blocks.BlockFit
import com.sumizuri.app.data.blocks.fit
import com.sumizuri.app.feature.profile.ShownBlock

// The page is four columns wide, and a block with a set height is a number of rows of this height.
internal const val GridColumns = 4

// The most rows a block can be given.
internal const val MostRows = 6
private val Gap = 12.dp

// The gap between blocks, for the places that draw a block at the size it has on the page.
internal val GridGap = Gap
internal val RowHeight = 76.dp

// How tall a block of a set number of rows is, with the gaps between its rows.
internal fun heightOfRows(rows: Int): Dp = RowHeight * rows + Gap * (rows - 1)

// The smallest a block can be: what its view needs, and a row more for a title over a view of one row.
internal val ShownBlock.fit: BlockFit
    get() {
        val base = view.fit()
        return if (title.isNotBlank() && !base.fitOnly && base.minH < 2) BlockFit(base.minW, base.minH + 1) else base
    }

// The size a block is drawn at: its own, made big enough for what it shows.
internal val ShownBlock.size: Pair<Int, Int> get() = fit.clamp(block.w, block.h)

// Where a block goes, in order: at the lowest place its width fits, but never above the block before it, and beside that block only to its right. So the blocks read in their order, from left to right and down, the way they are listed.
internal fun placeInOrder(
    widths: List<Int>,
    heights: (index: Int) -> Int,
    columns: Int = GridColumns,
): List<Pair<Int, Int>> {
    val skyline = IntArray(columns)
    var lastTop = 0
    var lastEnd = 0
    return widths.mapIndexed { index, wide ->
        val w = wide.coerceIn(1, columns)
        var best: Pair<Int, Int>? = null
        for (x in 0..columns - w) {
            val top = maxOf((x until x + w).maxOf { skyline[it] }, lastTop)
            if (top == lastTop && index > 0 && x < lastEnd) continue
            if (best == null || top < best.second) best = x to top
        }
        val (x, top) = best ?: (0 to (skyline.maxOrNull() ?: 0))
        val bottom = top + heights(index)
        for (column in x until x + w) skyline[column] = bottom
        lastTop = top
        lastEnd = x + w
        x to top
    }
}

// The blocks of the profile on four columns, in order. No block is drawn smaller than what it shows needs. With no blocks, a tile adds one.
@Composable
internal fun BlockGrid(blocks: List<ShownBlock>, env: BlockEnv, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val sizes = blocks.map { it.size }
    Layout(
        content = {
            blocks.forEachIndexed { index, item ->
                key(item.block.id) {
                    BlockTile(
                        item,
                        env,
                        fixedHeight =
                        sizes[index].second > 0,
                    )
                }
            }
            if (blocks.isEmpty()) AddBlockTile(onAdd)
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val gap = Gap.roundToPx()
        val row = RowHeight.roundToPx()
        val cell = ((width - gap * (GridColumns - 1)) / GridColumns).coerceAtLeast(1)
        val placeables = measurables.mapIndexed { index, measurable ->
            val (w, h) = sizes.getOrNull(index) ?: (GridColumns to 0)
            val span = cell * w + gap * (w - 1)
            if (h >
                0
            ) {
                measurable.measure(Constraints.fixed(span, row * h + gap * (h - 1)))
            } else {
                measurable.measure(Constraints(minWidth = span, maxWidth = span))
            }
        }
        val spots =
            placeInOrder(placeables.indices.map { sizes.getOrNull(it)?.first ?: GridColumns }, {
                placeables[it].height +
                    gap
            })
        val height = placeables.indices.maxOfOrNull { spots[it].second + placeables[it].height } ?: 0
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                placeable.place(
                    spots[index].first * (cell + gap),
                    spots[index].second,
                )
            }
        }
    }
}
