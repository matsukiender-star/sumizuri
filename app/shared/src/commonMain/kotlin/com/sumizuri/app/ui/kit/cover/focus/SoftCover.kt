package com.sumizuri.app.ui.kit.cover.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// The cover is read this many pixels wide for a soft backdrop, and each pixel is spread this far into its neighbours. Stretched across a window that comes to a light blur, as much as the backdrop had when the blur was worked out on every frame, so the cover still shows its colors and shapes.
private const val SoftWidth = 160
private const val SoftRadius = 2

// How many softened covers are kept, so a title opened again shows its backdrop at once. Each is about 150 kilobytes.
private const val Kept = 20

// The softened covers by address, the one used last at the end. It is used from the main thread only.
private val softened = LinkedHashMap<String, ImageBitmap>()

// A cover that is asked for becomes the one used last.
private fun softenedGet(key: String): ImageBitmap? = softened.remove(key)?.also { softened[key] = it }

// The cover made soft for a backdrop, or null until it is ready. The cover is read small and blurred once, so the backdrop costs nothing while the page is drawn, and stretched over the page it shows no squares, as the small picture alone would.
@Composable
fun rememberSoftCover(request: ImageRequest): ImageBitmap? {
    val context = LocalPlatformContext.current
    val key = request.data.toString()
    return produceState(softenedGet(key), key) {
        if (value != null) return@produceState
        val small = request
            .newBuilder()
            .size(Size(SoftWidth, SoftWidth * 3 / 2))
            .cpuReadable()
            .build()
        val image = (SingletonImageLoader.get(context).execute(small) as? SuccessResult)?.image ?: return@produceState
        val soft =
            withContext(Dispatchers.Default) { image.argbPixels()?.blurred(SoftRadius)?.toImageBitmap() }
                ?: return@produceState
        softened.remove(key)
        softened[key] = soft
        if (softened.size > Kept) softened.remove(softened.keys.first())
        value = soft
    }.value
}

// Spreads each pixel into its neighbours: three passes of a box blur across and then down, which together come close to a smooth gaussian blur.
internal fun CoverPixels.blurred(radius: Int): CoverPixels {
    var pixels = argb
    repeat(3) {
        pixels = boxPass(pixels, width, height, radius, across = true)
        pixels = boxPass(pixels, width, height, radius, across = false)
    }
    return CoverPixels(width, height, pixels)
}

// One pass of a box blur along every row, or down every column. Each pixel becomes the average of those within the radius, and the edge pixels are repeated past the edge.
private fun boxPass(from: IntArray, width: Int, height: Int, radius: Int, across: Boolean): IntArray {
    val out = IntArray(from.size)
    val lines = if (across) height else width
    val length = if (across) width else height
    val span = radius * 2 + 1
    for (line in 0 until lines) {
        fun at(i: Int): Int {
            val clamped = i.coerceIn(0, length - 1)
            return if (across) line * width + clamped else clamped * width + line
        }
        var red = 0
        var green = 0
        var blue = 0
        for (i in -radius..radius) {
            val c = from[at(i)]
            red += c shr 16 and 0xFF
            green += c shr 8 and 0xFF
            blue += c and 0xFF
        }
        for (i in 0 until length) {
            out[at(i)] = (0xFF shl 24) or (red / span shl 16) or (green / span shl 8) or (blue / span)
            // The window moves on by one: the pixel at its back leaves and the next one comes in.
            val gone = from[at(i - radius)]
            val come = from[at(i + radius + 1)]
            red += (come shr 16 and 0xFF) - (gone shr 16 and 0xFF)
            green += (come shr 8 and 0xFF) - (gone shr 8 and 0xFF)
            blue += (come and 0xFF) - (gone and 0xFF)
        }
    }
    return out
}
