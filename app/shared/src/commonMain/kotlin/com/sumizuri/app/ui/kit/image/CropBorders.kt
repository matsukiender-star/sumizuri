package com.sumizuri.app.ui.kit.image

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min

// A pixel at least this bright in every channel is white, and one at most this dark is black.
private const val WhiteFrom = 235
private const val BlackUpTo = 20

// A row or a column with fewer picture pixels than this is a speck of noise, not a part of the picture.
private const val MinHits = 3

// Margins smaller than this many pixels on every side are left as they are, since cutting them gains nothing.
private const val MinGain = 8

// Never cut away more than this share of the width or the height, so a page that is nearly blank stays whole.
private const val MostCut = 0.4f

// Cuts the blank white or black margin off the sides of a page, and gives the slices of what is left. A page whose edges are neither white nor black, or with nothing to cut, comes back as it was.
fun cropBorders(slices: List<ImageBitmap>, maxHeight: Int): List<ImageBitmap> {
    if (slices.isEmpty()) return slices
    val width = slices.first().width
    val height = slices.sumOf { it.height }
    val first = IntArray(1).also { slices.first().readPixels(it, 0, 0, 1, 1) }[0]
    val white = isWhite(first)
    if (!white && !isBlack(first)) return slices
    fun blank(pixel: Int): Boolean = pixel ushr 24 < 16 || if (white) isWhite(pixel) else isBlack(pixel)

    val rowHits = IntArray(height)
    val columnHits = IntArray(width)
    var top = 0
    for (slice in slices) {
        val buffer = IntArray(slice.width * slice.height)
        slice.readPixels(buffer, 0, 0, slice.width, slice.height, 0, slice.width)
        for (y in 0 until slice.height) {
            val line = y * slice.width
            for (x in 0 until min(slice.width, width)) {
                if (!blank(buffer[line + x])) {
                    rowHits[top + y]++
                    columnHits[x]++
                }
            }
        }
        top += slice.height
    }
    val up = rowHits.indexOfFirst { it >= MinHits }
    val down = rowHits.indexOfLast { it >= MinHits }
    val left = columnHits.indexOfFirst { it >= MinHits }
    val right = columnHits.indexOfLast { it >= MinHits }
    if (up < 0 || left < 0) return slices
    val cutTop = min(up, (height * MostCut).toInt())
    val cutBottom = min(height - 1 - down, (height * MostCut).toInt())
    val cutLeft = min(left, (width * MostCut).toInt())
    val cutRight = min(width - 1 - right, (width * MostCut).toInt())
    if (max(max(cutTop, cutBottom), max(cutLeft, cutRight)) < MinGain) return slices

    val outWidth = width - cutLeft - cutRight
    val outHeight = height - cutTop - cutBottom
    val out = ArrayList<ImageBitmap>()
    var from = cutTop
    while (from < cutTop + outHeight) {
        val rows = min(maxHeight, cutTop + outHeight - from)
        val piece = ImageBitmap(outWidth, rows)
        val canvas = Canvas(piece)
        val paint = Paint()
        var start = 0
        for (slice in slices) {
            val end = start + slice.height
            val overlapFrom = max(from, start)
            val overlapTo = min(from + rows, end)
            if (overlapTo > overlapFrom) {
                val size = IntSize(outWidth, overlapTo - overlapFrom)
                canvas.drawImageRect(
                    slice,
                    IntOffset(cutLeft, overlapFrom - start),
                    size,
                    IntOffset(
                        0,
                        overlapFrom - from,
                    ),
                    size,
                    paint,
                )
            }
            start = end
        }
        out += piece
        from += rows
    }
    return out
}

private fun isWhite(pixel: Int): Boolean =
    (pixel shr 16 and 0xFF) >= WhiteFrom && (pixel shr 8 and 0xFF) >= WhiteFrom && (pixel and 0xFF) >= WhiteFrom

private fun isBlack(pixel: Int): Boolean =
    (pixel shr 16 and 0xFF) <= BlackUpTo && (pixel shr 8 and 0xFF) <= BlackUpTo && (pixel and 0xFF) <= BlackUpTo
