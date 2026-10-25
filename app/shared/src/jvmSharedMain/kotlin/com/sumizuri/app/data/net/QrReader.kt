package com.sumizuri.app.data.net

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

// Reads a QR code from the colors of a picture, given as 0xAARRGGBB a row at a time, trying harder than usual since a code in a screenshot can be small.
internal fun qrFromPixels(width: Int, height: Int, pixels: IntArray): String? = runCatching {
    val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))
    MultiFormatReader().decode(bitmap, mapOf(DecodeHintType.TRY_HARDER to true)).text
}.getOrNull()
