package com.sumizuri.app.feature.reader.view

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import kotlin.math.abs
import kotlin.math.max

// About how many points across a page is looked at, which is enough to see the gutters between panels and cheap to read.
private const val SampleWidth = 300

// A row or column is a gutter only when all but a speck of it is the color of the page around the panels, so a row that only crosses the thin side borders of a panel still counts as inside it.
private const val BlankShare = 0.996f

// How near the color of a point must be to that of the gutters to count as gutter.
private const val BlankReach = 0.1f

// Pieces smaller than these parts of the page are specks, not panels.
private const val SmallestWidth = 0.08f
private const val SmallestHeight = 0.05f

// More pieces than this is a page of pictures or text, not of panels.
private const val MostPanels = 16

// The panels found on each page by its address, as bands from the top, each band in columns from the left, each column in pieces from the top. Kept for the pages read lately.
object PagePanels {
    private const val Keep = 64
    private val found = LinkedHashMap<String, List<List<List<Rect>>>>()

    // Whether pages are looked at for panels as they are read, which only the panel by panel reading needs.
    @kotlin.concurrent.Volatile
    var wanted = false

    @Synchronized
    fun get(url: String): List<List<List<Rect>>>? = found[url]

    @Synchronized
    fun put(url: String, panels: List<List<List<Rect>>>) {
        found.remove(url)
        found[url] = panels
        while (found.size > Keep) found.remove(found.keys.first())
    }
}

// The panels in reading order: bands from the top, and within a band the columns from the left, or from the right in a book that reads right to left.
fun List<List<List<Rect>>>.inOrder(rtl: Boolean): List<Rect> =
    flatMap { band -> (if (rtl) band.reversed() else band).flatten() }

// Finds the panels of a comic page by its gutters: the page is cut into bands across at the rows that are all gutter, each band into columns, and each column once more into pieces. The rectangles are parts of the page, from 0 to 1. Gives nothing for a page that is not cut into panels.
fun findPanels(image: ImageBitmap): List<List<List<Rect>>> {
    val width = image.width
    val height = image.height
    if (width < 200 || height < 200) return emptyList()
    val step = max(1, width / SampleWidth)
    val cols = width / step
    val rows = height / step
    val light = FloatArray(cols * rows)
    val line = IntArray(width)
    for (row in 0 until rows) {
        image.readPixels(line, 0, row * step, width, 1)
        for (col in 0 until cols) light[row * cols + col] = brightness(line[col * step])
    }
    // The gutters take the color most of the edge of the page has.
    val edge = buildList {
        for (col in 0 until cols) {
            add(light[col])
            add(light[(rows - 1) * cols + col])
        }
        for (row in 0 until rows) {
            add(light[row * cols])
            add(light[row * cols + cols - 1])
        }
    }.sorted()[cols + rows]
    fun blank(row: Int, col: Int) = abs(light[row * cols + col] - edge) < BlankReach
    fun rowFull(row: Int, from: Int, to: Int) =
        (from..to).count { !blank(row, it) } > (to - from + 1) * (1 - BlankShare)
    fun colFull(col: Int, from: Int, to: Int) =
        (from..to).count { !blank(it, col) } > (to - from + 1) * (1 - BlankShare)
    fun part(left: Int, top: Int, right: Int, bottom: Int) =
        Rect(left.toFloat() / cols, top.toFloat() / rows, (right + 1).toFloat() / cols, (bottom + 1).toFloat() / rows)
    val bands = runs(rows) { rowFull(it, 0, cols - 1) }
        .map { across ->
            runs(cols) { colFull(it, across.first, across.last) }
                .map { down ->
                    runs(rows, from = across.first, to = across.last) { rowFull(it, down.first, down.last) }
                        .map { piece -> part(down.first, piece.first, down.last, piece.last) }
                        .filter { it.width >= SmallestWidth && it.height >= SmallestHeight }
                }.filter { it.isNotEmpty() }
        }.filter { it.isNotEmpty() }
    val count = bands.sumOf { band -> band.sumOf { it.size } }
    return if (count < 2 || count > MostPanels) emptyList() else bands
}

// The stretches of a line where there is something, with gaps of a point or two closed and slivers left out.
private fun runs(size: Int, from: Int = 0, to: Int = size - 1, full: (Int) -> Boolean): List<IntRange> {
    val shortest = max(2, (to - from + 1) / 25)
    val found = mutableListOf<IntRange>()
    var start = -1
    var gap = 0
    for (at in from..to) {
        if (full(at)) {
            if (start < 0) start = at
            gap = 0
        } else if (start >= 0) {
            gap++
            if (gap > 2) {
                val end = at - gap
                if (end - start + 1 >= shortest) found += start..end
                start = -1
                gap = 0
            }
        }
    }
    if (start >= 0 && to - gap - start + 1 >= shortest) found += start..(to - gap)
    return found
}

private fun brightness(argb: Int): Float {
    val red = (argb shr 16) and 0xFF
    val green = (argb shr 8) and 0xFF
    val blue = argb and 0xFF
    return (red * 0.299f + green * 0.587f + blue * 0.114f) / 255f
}

// The panels of a page are kept by its address and whether its borders were cut, since the cut changes where they sit.
fun panelKey(url: String, crop: Boolean): String = if (crop) "$url|crop" else url
