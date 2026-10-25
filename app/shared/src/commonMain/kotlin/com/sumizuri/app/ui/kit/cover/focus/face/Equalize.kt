package com.sumizuri.app.ui.kit.cover.focus.face

import com.sumizuri.app.ui.kit.cover.focus.CoverPixels

// The brightness of every pixel from 0 to 255, spread over the whole range, so a dark cover and a bright one look the same to the cascade.
fun equalizedGray(cover: CoverPixels): IntArray {
    val gray = IntArray(cover.argb.size) {
        val color = cover.argb[it]
        ((color shr 16 and 0xFF) * 299 + (color shr 8 and 0xFF) * 587 + (color and 0xFF) * 114) / 1000
    }
    val histogram = IntArray(256)
    for (value in gray) histogram[value]++
    val running = IntArray(256)
    var sum = 0
    for (level in 0..255) {
        sum += histogram[level]
        running[level] = sum
    }
    val lowest = running.firstOrNull { it > 0 } ?: 0
    val span = (gray.size - lowest).coerceAtLeast(1)
    return IntArray(gray.size) { (running[gray[it]] - lowest) * 255 / span }
}
