package com.sumizuri.app.ui.kit.image

import coil3.toBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

internal actual fun encodeSnapshot(image: coil3.Image): ByteArray? = runCatching {
    Image.makeFromBitmap(image.toBitmap()).use { it.encodeToData(EncodedImageFormat.JPEG, 82)?.bytes }
}.getOrNull()
