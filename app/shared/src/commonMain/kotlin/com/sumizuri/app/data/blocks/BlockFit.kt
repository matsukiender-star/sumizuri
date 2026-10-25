package com.sumizuri.app.data.blocks

// The smallest a block can be for what it shows to fit: columns across and rows down. A block that fits its content only, as a part of the profile as it came does, takes no set height at all. A height of 0, which fits the content, is always allowed.
class BlockFit(val minW: Int, val minH: Int, val fitOnly: Boolean = false) {
    // A size made big enough: at least the smallest width, and at least the smallest height when the height is set.
    fun clamp(w: Int, h: Int): Pair<Int, Int> {
        val width = w.coerceIn(minW.coerceAtMost(4), 4)
        val height = if (fitOnly || h <= 0) 0 else h.coerceAtLeast(minH)
        return width to height
    }

    fun allows(w: Int, h: Int): Boolean = w in minW..4 && (h == 0 || (!fitOnly && h >= minH))
}

// How small each kind of view can go. A number needs two columns for its label, a ring needs two rows for the ring, a chart three columns for its bars or days, covers three columns and three rows for a few covers and their names, and a part of the profile as it came the whole width.
fun BlockView.fit(): BlockFit = when (this) {
    is BlockView.Figure, is BlockView.Bar -> BlockFit(2, 1)

    is BlockView.Ring, is BlockView.Items -> BlockFit(2, 2)

    is BlockView.Chart, is BlockView.Heatmap -> BlockFit(3, 2)

    is BlockView.Covers -> BlockFit(3, 3)

    is BlockView.Words -> BlockFit(1, 1)

    is BlockView.Classic -> BlockFit(4, 0, fitOnly = true)

    is BlockView.Problem -> BlockFit(2, 1)

    BlockView.Empty -> BlockFit(1, 1)

    is BlockView.Stack -> {
        val parts = this.parts.map { it.fit() }
        if (parts.isEmpty()) {
            BlockFit(1, 1)
        } else if (row) {
            BlockFit(parts.sumOf { it.minW }.coerceAtMost(4), parts.maxOf { it.minH }, parts.any { it.fitOnly })
        } else {
            BlockFit(parts.maxOf { it.minW }, parts.sumOf { it.minH }, parts.any { it.fitOnly })
        }
    }
}
