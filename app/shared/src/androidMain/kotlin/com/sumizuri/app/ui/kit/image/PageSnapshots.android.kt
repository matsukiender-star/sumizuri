package com.sumizuri.app.ui.kit.image

import android.graphics.Bitmap
import coil3.toBitmap
import java.io.ByteArrayOutputStream

internal actual fun encodeSnapshot(image: coil3.Image): ByteArray? = runCatching {
    ByteArrayOutputStream().use { out ->
        image.toBitmap().compress(Bitmap.CompressFormat.JPEG, 82, out)
        out.toByteArray()
    }
}.getOrNull()
