package com.sumizuri.app.playback

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// How often the picture is looked at for its colors.
private const val EveryMs = 450L

// The picture is copied this small, which the system scales for free and is all the colors need.
private const val SmallWidth = 32
private const val SmallHeight = 18

// Copies the picture of the video from the screen while the light around it is wanted, and gives the engine the colors at its edges. A video drawn on a texture, or a system that refuses the copy, leaves the light out.
internal suspend fun readAmbient(view: PlayerView, exo: ExoEngine) {
    val small = Bitmap.createBitmap(SmallWidth, SmallHeight, Bitmap.Config.ARGB_8888)
    val main = Handler(Looper.getMainLooper())
    try {
        while (true) {
            delay(EveryMs)
            if (!exo.glowOn) continue
            val surface = view.videoSurfaceView as? SurfaceView ?: continue
            if (!surface.holder.surface.isValid || surface.width == 0) continue
            val copied = suspendCancellableCoroutine { done ->
                runCatching {
                    PixelCopy.request(surface, small, { result -> done.resume(result == PixelCopy.SUCCESS) }, main)
                }.onFailure { done.resume(false) }
            }
            if (!copied || !exo.glowOn) continue
            val next = edgeColors(SmallWidth, SmallHeight) { x, y -> small.getPixel(x, y) }
            exo.glow.value = exo.glow.value.toward(next)
        }
    } finally {
        small.recycle()
    }
}
