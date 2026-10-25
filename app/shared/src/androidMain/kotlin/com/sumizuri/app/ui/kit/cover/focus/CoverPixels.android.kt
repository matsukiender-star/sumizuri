package com.sumizuri.app.ui.kit.cover.focus

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.Image
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap

actual fun Image.argbPixels(): CoverPixels? {
    val bitmap = toBitmap()
    val width = bitmap.width
    val height = bitmap.height
    val argb = IntArray(width * height)
    bitmap.getPixels(argb, 0, width, 0, 0, width, height)
    return CoverPixels(width, height, argb)
}

actual fun ImageRequest.Builder.cpuReadable(): ImageRequest.Builder = allowHardware(false)

actual fun CoverPixels.toImageBitmap(): ImageBitmap =
    Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
