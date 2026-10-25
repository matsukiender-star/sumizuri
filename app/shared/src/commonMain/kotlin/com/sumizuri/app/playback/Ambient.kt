package com.sumizuri.app.playback

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// No colors, for an engine that cannot tell those of its picture.
val NoAmbient: StateFlow<List<Color>> = MutableStateFlow(emptyList())

// How deep into the picture each edge is looked at, as a part of its width or height.
private const val EdgeDepth = 0.12f

// How many points are looked at along and into each edge.
private const val Along = 12
private const val Into = 4

// The colors at the four edges of a picture: left, right, top and bottom. The color of a point is read by its place, as 0xAARRGGBB.
fun edgeColors(width: Int, height: Int, at: (x: Int, y: Int) -> Int): List<Color> {
    if (width < 2 || height < 2) return emptyList()
    fun average(points: List<Pair<Int, Int>>): Color {
        var red = 0f
        var green = 0f
        var blue = 0f
        for ((x, y) in points) {
            val argb = at(x.coerceIn(0, width - 1), y.coerceIn(0, height - 1))
            red += (argb shr 16 and 0xFF) / 255f
            green += (argb shr 8 and 0xFF) / 255f
            blue += (argb and 0xFF) / 255f
        }
        return Color(red / points.size, green / points.size, blue / points.size)
    }
    val deepX = (width * EdgeDepth).toInt().coerceAtLeast(1)
    val deepY = (height * EdgeDepth).toInt().coerceAtLeast(1)
    fun grid(xs: IntProgression, ys: IntProgression) = xs.flatMap { x -> ys.map { y -> x to y } }
    val alongY = 0 until height step (height / Along).coerceAtLeast(1)
    val alongX = 0 until width step (width / Along).coerceAtLeast(1)
    return listOf(
        average(grid(0 until deepX step (deepX / Into).coerceAtLeast(1), alongY)),
        average(grid(width - deepX until width step (deepX / Into).coerceAtLeast(1), alongY)),
        average(grid(alongX, 0 until deepY step (deepY / Into).coerceAtLeast(1))),
        average(grid(alongX, height - deepY until height step (deepY / Into).coerceAtLeast(1))),
    )
}

// The colors moved a part of the way toward new ones, so the light drifts with the picture instead of flickering with each cut.
fun List<Color>.toward(next: List<Color>, by: Float = 0.35f): List<Color> =
    if (size != next.size) next else zip(next) { old, new -> lerp(old, new, by) }

// A small picture of a video at a time, for the seek bar.
class VideoThumbnail(val positionMs: Long, val image: androidx.compose.ui.graphics.ImageBitmap)

// No pictures, for an engine that cannot make them.
val NoThumbnails: StateFlow<VideoThumbnail?> = MutableStateFlow(null)
