package com.sumizuri.app.ui.kit.cover.focus

import kotlin.math.max

// The same picture at a smaller width, each new pixel the average of the pixels it covers.
fun CoverPixels.scaledToWidth(target: Int): CoverPixels {
    if (width <= target) return this
    val newHeight = max(1, height * target / width)
    val out = IntArray(target * newHeight)
    for (y in 0 until newHeight) {
        val y0 = y * height / newHeight
        val y1 = max(y0 + 1, (y + 1) * height / newHeight)
        for (x in 0 until target) {
            val x0 = x * width / target
            val x1 = max(x0 + 1, (x + 1) * width / target)
            var r = 0
            var g = 0
            var b = 0
            for (yy in y0 until y1) {
                for (xx in x0 until x1) {
                    val color = argb[yy * width + xx]
                    r += color shr 16 and 0xFF
                    g += color shr 8 and 0xFF
                    b += color and 0xFF
                }
            }
            val count = (y1 - y0) * (x1 - x0)
            out[y * target + x] = (0xFF shl 24) or (r / count shl 16) or (g / count shl 8) or (b / count)
        }
    }
    return CoverPixels(target, newHeight, out)
}
