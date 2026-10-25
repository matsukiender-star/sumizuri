package com.sumizuri.app.ui.kit.image

import androidx.compose.ui.graphics.ImageBitmap
import coil3.ComponentRegistry
import coil3.Image
import com.sumizuri.app.source.model.Descramble

// Makes a picture that is shown much smaller than it is come out clean on the systems that draw it as it is. It is cut down once while it is decoded, with a good filter, and the cut down picture is what the memory keeps.
expect fun ComponentRegistry.Builder.addSharpDecoder()

// Cuts a decoded picture into pieces no taller than maxHeight pixels, each its own image, so a very tall page never has to fit in one texture of the graphics card. A picture that was cut into tiles and shuffled is put back together first.
expect fun Image.sliceTall(maxHeight: Int, scramble: Descramble?): List<ImageBitmap>

// Moves the slices of a page to the memory of the graphics card where the system can, and gives them as they were where it cannot. A slice in the memory of the app is sent to the card the first time it is drawn, on the thread that draws, and a big one makes the screen stop for a moment. Done here, it is done away from that thread, and it takes nothing from the memory of the app. Slices are read as pixels before this, so it is the last step.
expect fun List<ImageBitmap>.onGraphicsMemory(): List<ImageBitmap>

// Lets go of slices that were replaced, such as the ones a page had before its borders were cut, where the system would otherwise keep them until its collector runs.
expect fun List<ImageBitmap>.release()
