package com.sumizuri.app.data.net

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

actual fun qrTextOf(picture: ByteArray): String? {
    val image = runCatching { ImageIO.read(ByteArrayInputStream(picture)) }.getOrNull() ?: return null
    val pixels = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
    return qrFromPixels(image.width, image.height, pixels)
}
