package com.sumizuri.app.ui.kit.cover.focus

import androidx.compose.ui.graphics.ImageBitmap
import coil3.Image
import coil3.request.ImageRequest

// A small picture as a list of colors, one number for each pixel in the order of the rows, with the alpha in the top byte.
class CoverPixels(val width: Int, val height: Int, val argb: IntArray)

// The colors of a decoded picture, or null when they cannot be read.
expect fun Image.argbPixels(): CoverPixels?

// Asks for a picture the app can cut up, which on Android means one kept in ordinary memory and not on the graphics card.
expect fun ImageRequest.Builder.cpuReadable(): ImageRequest.Builder

// A picture to draw, made from the colors of a small picture.
expect fun CoverPixels.toImageBitmap(): ImageBitmap
