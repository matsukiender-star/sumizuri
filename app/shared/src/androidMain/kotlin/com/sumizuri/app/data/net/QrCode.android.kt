package com.sumizuri.app.data.net

import android.graphics.BitmapFactory

actual fun qrTextOf(picture: ByteArray): String? {
    val bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size) ?: return null
    return try {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        qrFromPixels(bitmap.width, bitmap.height, pixels)
    } finally {
        bitmap.recycle()
    }
}
