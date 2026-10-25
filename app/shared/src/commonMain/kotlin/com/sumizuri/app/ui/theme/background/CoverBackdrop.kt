package com.sumizuri.app.ui.theme.background

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import coil3.compose.rememberAsyncImagePainter
import com.sumizuri.app.ui.kit.image.imageRequest
import com.sumizuri.app.ui.theme.spec.ThemeBackground
import kotlin.math.ceil
import kotlin.math.sqrt

// The covers of the library the background can show, as their addresses, given by the app.
val LocalBackdropCovers = compositionLocalOf<List<String>> { emptyList() }

// How wide a cover of the mosaic is at the usual scale, in pixels of the screen at its density.
private const val CoverDp = 110f

// How far the mosaic leans, in degrees.
private const val Lean = -12f

// The covers the mosaic draws in turn, as many as the background asks for and the library has.
@Composable
internal fun rememberBackdropCovers(count: Int): List<Painter> =
    LocalBackdropCovers.current.take(count).map { url -> rememberAsyncImagePainter(imageRequest(url, maxWidth = 240)) }

// A slow mosaic of covers that leans a little and drifts on the diagonal, faint so the pages on it stay easy to read.
internal fun DrawScope.drawCovers(time: Long, look: ThemeBackground, covers: List<Painter>, strength: Float) {
    if (covers.isEmpty() || strength <= 0f) return
    val width = CoverDp * density * look.scale.coerceIn(0.4f, 3f)
    val height = width * 1.45f
    val gap = width * 0.14f
    val stepX = width + gap
    val stepY = height + gap
    // The mosaic drifts one cover across in a minute at the usual speed, and stands still when the background does.
    val drift = (time % 60_000L) / 60_000f
    val alpha = (0.16f * strength).coerceIn(0.03f, 0.5f)
    val reach = sqrt(size.width * size.width + size.height * size.height)
    val columns = ceil(reach / stepX).toInt() + 2
    val rows = ceil(reach / stepY).toInt() + 2
    rotate(Lean, Offset(size.width / 2, size.height / 2)) {
        val left = (size.width - columns * stepX) / 2 - drift * stepX
        val top = (size.height - rows * stepY) / 2 - drift * stepY
        for (row in 0 until rows) {
            // Every other row is shifted half a cover, so the mosaic does not line up in columns.
            val shift = if (row % 2 == 0) 0f else stepX / 2
            for (column in 0 until columns) {
                val painter = covers[(row * 7 + column * 3) % covers.size]
                translate(left + column * stepX + shift, top + row * stepY) {
                    with(painter) { draw(Size(width, height), alpha) }
                }
            }
        }
    }
}
