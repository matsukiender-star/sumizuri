package com.sumizuri.app.data.net

// The text of the QR code in a picture, such as a WireGuard config shared as a code, or null when the picture has none the app can read.
expect fun qrTextOf(picture: ByteArray): String?
