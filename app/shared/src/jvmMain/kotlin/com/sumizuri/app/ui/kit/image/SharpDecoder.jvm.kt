package com.sumizuri.app.ui.kit.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import coil3.ComponentRegistry
import coil3.Image
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.SkiaImageDecoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.Dimension
import coil3.size.Size
import coil3.toBitmap
import com.sumizuri.app.source.model.Descramble
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.IRect
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

// Only this many pictures are read whole at a time. A picture is decoded at its full size before it is shrunk, so more at once would fill the memory.
private val FullDecodes = Semaphore(2)

// A picture is cut down only when it is bigger than the room it has by more than this.
private const val Enough = 1.1f

// Reads a picture whole, and then cuts it down to the size it is wanted at with a filter that keeps the edges clean. Coil cuts it down by itself with a cheap filter that leaves jagged edges, which show on the covers of a grid.
private class SharpDecoder(private val inner: Decoder, private val options: Options) : Decoder {
    override suspend fun decode(): DecodeResult? = FullDecodes.withPermit { decodeAndShrink() }

    private suspend fun decodeAndShrink(): DecodeResult? {
        val decoded = inner.decode() ?: return null
        val wantedWidth = (options.size.width as? Dimension.Pixels)?.px
        val wantedHeight = (options.size.height as? Dimension.Pixels)?.px
        if (wantedWidth == null && wantedHeight == null) return decoded
        val source = decoded.image.toBitmap()
        val scale = max(
            wantedWidth?.let { it.toFloat() / source.width } ?: 0f,
            wantedHeight?.let { it.toFloat() / source.height } ?: 0f,
        )
        if (scale <= 0f || scale > 1f / Enough) return decoded
        val width = ceil(source.width * scale).toInt().coerceAtLeast(1)
        val height = ceil(source.height * scale).toInt().coerceAtLeast(1)
        // A cubic filter looks at only a few pixels, so a big cut in one step leaves jagged edges. The picture is halved with an averaging filter until it is near the size, and the cubic filter does the last small step.
        val smaller = Bitmap().apply { allocPixels(ImageInfo.makeN32(width, height, ColorAlphaType.PREMUL)) }
        var current = org.jetbrains.skia.Image
            .makeFromBitmap(source)
        try {
            while (current.width >= width * 2 && current.height >= height * 2) {
                val next = halved(current) ?: return decoded
                // Each step is let go of at once, since a native picture is only freed by the collector otherwise, long after it was needed.
                current.close()
                current = next
            }
            if (!current.scalePixels(
                    smaller.peekPixels() ?: return decoded,
                    SamplingMode.MITCHELL,
                    false,
                )
            ) {
                return decoded
            }
        } finally {
            current.close()
        }
        smaller.setImmutable()
        // The picture at full size is let go of at once, since the collector would free it only long after, and many covers at once fill the memory.
        source.close()
        // Marked as cut down, so the memory keeps it for this size and not for every size.
        return DecodeResult(smaller.asImage(), isSampled = true)
    }

    class Factory : Decoder.Factory {
        private val skia = SkiaImageDecoder.Factory()

        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? =
            skia.create(result, options.copy(size = Size.ORIGINAL), imageLoader).let {
                SharpDecoder(it, options)
            }
    }
}

// The picture at half its size, each new pixel the average of the four it covers. Null when it could not be made.
private fun halved(image: org.jetbrains.skia.Image): org.jetbrains.skia.Image? {
    val half = Bitmap().apply {
        allocPixels(
            ImageInfo.makeN32(image.width / 2, image.height / 2, ColorAlphaType.PREMUL),
        )
    }
    if (!image.scalePixels(half.peekPixels() ?: return null, SamplingMode.LINEAR, false)) return null
    half.setImmutable()
    val image = org.jetbrains.skia.Image
        .makeFromBitmap(half)
    // The picture holds the pixels now, so the bitmap that made it is let go of at once.
    half.close()
    return image
}

// A desktop sends a picture to the card as it is drawn, so there is nothing to move. Each slice is made fixed, since a picture that can still change is copied every time it is drawn.
actual fun List<ImageBitmap>.onGraphicsMemory(): List<ImageBitmap> = onEach { it.asSkiaBitmap().setImmutable() }

actual fun ComponentRegistry.Builder.addSharpDecoder() {
    add(SharpDecoder.Factory())
}

// The slices share the pixels of the whole picture, which is made fixed so no slice copies them, and the picture itself is let go of at once. Before, each slice was copied twice and the whole picture waited for the collector, which kept about three times each page in memory.
actual fun Image.sliceTall(maxHeight: Int, scramble: Descramble?): List<ImageBitmap> {
    val decoded = toBitmap()
    val whole = if (scramble == null) decoded else decoded.putBack(scramble).also { decoded.close() }
    whole.setImmutable()
    val slices = (0 until whole.height step maxHeight).mapNotNull { top ->
        val piece = Bitmap()
        val rect = IRect.makeXYWH(0, top, whole.width, min(maxHeight, whole.height - top))
        if (whole.extractSubset(piece, rect)) {
            piece.asComposeImageBitmap()
        } else {
            piece.close()
            null
        }
    }
    whole.close()
    return slices
}

// Puts the tiles of a shuffled picture back. Tile number n of the order goes where tile n of the grid is, and the edge left over after the division is dropped.
private fun Bitmap.putBack(spec: Descramble): Bitmap {
    val tileWidth = width / spec.cols
    val tileHeight = height / spec.rows
    if (tileWidth <= 0 || tileHeight <= 0) return this
    val out = Bitmap()
    out.allocN32Pixels(tileWidth * spec.cols, tileHeight * spec.rows)
    val canvas = Canvas(out)
    val picture = org.jetbrains.skia.Image
        .makeFromBitmap(this)
    spec.order.forEachIndexed { place, from ->
        if (from !in 0 until spec.cols * spec.rows) return@forEachIndexed
        val source = Rect.makeXYWH(
            (from % spec.cols * tileWidth).toFloat(),
            (from / spec.cols * tileHeight).toFloat(),
            tileWidth.toFloat(),
            tileHeight.toFloat(),
        )
        val target = Rect.makeXYWH(
            (place % spec.cols * tileWidth).toFloat(),
            (place / spec.cols * tileHeight).toFloat(),
            tileWidth.toFloat(),
            tileHeight.toFloat(),
        )
        canvas.drawImageRect(picture, source, target)
    }
    canvas.close()
    picture.close()
    return out
}

actual fun List<ImageBitmap>.release() {
    forEach { it.asSkiaBitmap().close() }
}
