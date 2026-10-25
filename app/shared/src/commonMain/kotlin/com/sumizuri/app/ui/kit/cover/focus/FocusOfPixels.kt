package com.sumizuri.app.ui.kit.cover.focus

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// With titles ignored, no face is looked for in the top and the bottom of the cover beyond these shares of its height.
private const val TopZone = 0.06
private const val BottomZone = 0.6

// A color that fills more than this share of the cover is paper, a wall or a logo and not a face.
private const val CommonShare = 0.08

// Finds the likeliest face in a small picture, which is skin colored pixels that sit among fine detail like eyes, hair and a mouth, with a lean to the upper part where faces on covers almost always are. It can also leave out the zones where logos and titles sit.
fun focusOfPixels(cover: CoverPixels, ignoreTitles: Boolean = false): CoverFocus {
    val width = cover.width
    val height = cover.height
    val argb = cover.argb
    val aspect = width.toFloat() / height
    fun red(i: Int) = argb[i] shr 16 and 0xFF
    fun green(i: Int) = argb[i] shr 8 and 0xFF
    fun blue(i: Int) = argb[i] and 0xFF

    val luma = DoubleArray(width * height) { 0.299 * red(it) + 0.587 * green(it) + 0.114 * blue(it) }
    val stride = width + 1
    // Each pixel's detail once, summed so any box of it is four lookups.
    val detailSums = DoubleArray(stride * (height + 1))
    for (y in 0 until height) {
        var row = 0.0
        for (x in 0 until width) {
            val i = y * width + x
            val right = if (x + 1 < width) luma[i + 1] else luma[i]
            val below = if (y + 1 < height) luma[i + width] else luma[i]
            row += (abs(luma[i] - right) + abs(luma[i] - below)) / 255
            detailSums[(y + 1) * stride + x + 1] = detailSums[y * stride + x + 1] + row
        }
    }
    fun box(sums: DoubleArray, x0: Int, y0: Int, x1: Int, y1: Int) =
        sums[y1 * stride + x1] - sums[y0 * stride + x1] - sums[y1 * stride + x0] + sums[y0 * stride + x0]

    // Colors by coarse bin, to tell the colors that fill the cover from the ones that do not.
    val bins = IntArray(512)
    fun binOf(i: Int) = (red(i) shr 5 shl 6) or (green(i) shr 5 shl 3) or (blue(i) shr 5)
    for (i in 0 until width * height) bins[binOf(i)]++
    val commonAt = width * height * CommonShare

    // How face like each pixel is: skin, among detail, high on the cover.
    val weights = DoubleArray(width * height)
    for (y in 0 until height) {
        // Faces sit high on nearly every cover, and the lower half is mostly bodies and titles.
        val lean = 1.25 - 0.9 * (y.toDouble() / height)
        // The zones of logos and badges at the top and of titles at the bottom hold no face to look for.
        if (ignoreTitles && (y < height * TopZone || y > height * BottomZone)) continue
        for (x in 0 until width) {
            val i = y * width + x
            if (!looksLikeSkin(red(i), green(i), blue(i))) continue
            val around = box(detailSums, max(0, x - 2), max(0, y - 2), min(width, x + 3), min(height, y + 3))
            weights[i] = (0.3 + around) * lean * (if (bins[binOf(i)] > commonAt) 0.15 else 1.0)
        }
    }

    val sums = DoubleArray(stride * (height + 1))
    for (y in 0 until height) {
        var row = 0.0
        for (x in 0 until width) {
            row += weights[y * width + x]
            sums[(y + 1) * stride + x + 1] = sums[y * stride + x + 1] + row
        }
    }
    // Too little skin to trust, so the upper part where a face would usually be.
    if (box(sums, 0, 0, width, height) < width * height * 0.01) return CoverFocus(0.5f, 0.3f, aspect)

    // The densest face sized patch, and not the average of all skin, since a face and an arm average out to a place between them that is neither.
    val patchWidth = max(3, (width * 0.34).roundToInt())
    val patchHeight = max(3, (height * 0.2).roundToInt())
    var best = -1.0
    var bestX = 0
    var bestY = 0
    for (y in 0..height - patchHeight) {
        for (x in 0..width - patchWidth) {
            val score = box(sums, x, y, x + patchWidth, y + patchHeight)
            if (score > best) {
                best = score
                bestX = x
                bestY = y
            }
        }
    }
    // The weighted middle of the winning patch, for a point finer than the grid of patches.
    var sum = 0.0
    var sumX = 0.0
    var sumY = 0.0
    for (y in bestY until bestY + patchHeight) {
        for (x in bestX until bestX + patchWidth) {
            val weight = weights[y * width + x]
            sum += weight
            sumX += weight * (x + 0.5)
            sumY += weight * (y + 0.5)
        }
    }
    if (sum <= 0.0) return CoverFocus(0.5f, 0.3f, aspect)
    return CoverFocus((sumX / sum / width).toFloat(), (sumY / sum / height).toFloat(), aspect)
}

// Skin, drawn or photographed, and not the orange, blonde or brown hair that shares its hue, since skin keeps far more blue against its red than hair does.
private fun looksLikeSkin(r: Int, g: Int, b: Int): Boolean {
    if (r < 90 || r <= g || r <= b) return false
    val gr = g.toDouble() / r
    val br = b.toDouble() / r
    return gr in 0.6..0.96 && br in 0.45..0.93 && r - b > 12
}
