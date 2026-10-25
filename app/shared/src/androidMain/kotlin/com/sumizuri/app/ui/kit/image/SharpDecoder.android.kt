package com.sumizuri.app.ui.kit.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.ComponentRegistry
import coil3.Image
import coil3.toBitmap
import com.sumizuri.app.source.model.Descramble
import kotlin.math.min

actual fun ComponentRegistry.Builder.addSharpDecoder() = Unit

// Hardware bitmaps came with Android 8.
private const val FirstHardware = 26

actual fun List<ImageBitmap>.onGraphicsMemory(): List<ImageBitmap> {
    if (Build.VERSION.SDK_INT < FirstHardware) return this
    return map { slice ->
        val soft = slice.asAndroidBitmap()
        // A slice the card cannot take stays as it was, and a slice that moved is let go of in the memory of the app.
        val hard = runCatching { soft.copy(Bitmap.Config.HARDWARE, false) }.getOrNull() ?: return@map slice
        soft.recycle()
        hard.asImageBitmap()
    }
}

actual fun Image.sliceTall(maxHeight: Int, scramble: Descramble?): List<ImageBitmap> {
    val whole = toBitmap()
    val bitmap = if (scramble == null) whole else whole.putBack(scramble)
    val slices = (0 until bitmap.height step maxHeight).map { top ->
        Bitmap.createBitmap(bitmap, 0, top, bitmap.width, min(maxHeight, bitmap.height - top)).asImageBitmap()
    }
    // A tall picture is copied into its slices, so the whole one is of no use any more and is freed now. A picture in one slice may be the very same bitmap, and a shuffled one is left to the collector.
    if (scramble == null && slices.size > 1) whole.recycle()
    return slices
}

// Puts the tiles of a shuffled picture back. Tile number n of the order goes where tile n of the grid is, and the edge left over after the division is dropped.
private fun Bitmap.putBack(spec: Descramble): Bitmap {
    val tileWidth = width / spec.cols
    val tileHeight = height / spec.rows
    if (tileWidth <= 0 || tileHeight <= 0) return this
    val out = Bitmap.createBitmap(tileWidth * spec.cols, tileHeight * spec.rows, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    spec.order.forEachIndexed { place, from ->
        if (from !in 0 until spec.cols * spec.rows) return@forEachIndexed
        val left = from % spec.cols * tileWidth
        val top = from / spec.cols * tileHeight
        val x = place % spec.cols * tileWidth
        val y = place / spec.cols * tileHeight
        canvas.drawBitmap(
            this,
            Rect(left, top, left + tileWidth, top + tileHeight),
            Rect(
                x,
                y,
                x + tileWidth,
                y + tileHeight,
            ),
            null,
        )
    }
    return out
}

// Android tells its collector about the memory of pictures, so it frees them in time by itself.
actual fun List<ImageBitmap>.release() = Unit
