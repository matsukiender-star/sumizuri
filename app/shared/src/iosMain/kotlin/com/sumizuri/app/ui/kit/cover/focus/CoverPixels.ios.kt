package com.sumizuri.app.ui.kit.cover.focus

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import coil3.Image
import coil3.request.ImageRequest
import coil3.toBitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Image as SkiaImage

actual fun Image.argbPixels(): CoverPixels? {
    val bitmap = toBitmap()
    val width = bitmap.width
    val height = bitmap.height
    val bytes = bitmap.readPixels(ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL)) ?: return null
    val argb = IntArray(width * height) { i ->
        val at = i * 4
        (bytes[at + 3].toInt() and 0xFF shl 24) or (bytes[at + 2].toInt() and 0xFF shl 16) or
            (bytes[at + 1].toInt() and 0xFF shl 8) or
            (bytes[at].toInt() and 0xFF)
    }
    return CoverPixels(width, height, argb)
}

// A picture is always in ordinary memory here, so there is nothing to ask for.
actual fun ImageRequest.Builder.cpuReadable(): ImageRequest.Builder = this

actual fun CoverPixels.toImageBitmap(): ImageBitmap {
    val bytes = ByteArray(width * height * 4)
    for (i in argb.indices) {
        val c = argb[i]
        val at = i * 4
        bytes[at] = (c and 0xFF).toByte()
        bytes[at + 1] = (c shr 8 and 0xFF).toByte()
        bytes[at + 2] = (c shr 16 and 0xFF).toByte()
        bytes[at + 3] = (c ushr 24).toByte()
    }
    return SkiaImage
        .makeRaster(
            ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL),
            bytes,
            width * 4,
        ).toComposeImageBitmap()
}
