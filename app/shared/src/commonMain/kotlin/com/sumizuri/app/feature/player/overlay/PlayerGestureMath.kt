package com.sumizuri.app.feature.player.overlay

import kotlin.math.abs
import kotlin.math.roundToLong

// The numbers behind the touch gestures of the player.
object PlayerGestureMath {
    // A drag across the whole width moves this many seconds, and the button for the next episode shows this long before the end.
    private const val FullWidthSeconds = 90
    const val UpNextWindowMs = 90_000L

    fun dragSeekDelta(dx: Float, width: Float): Long = if (width <=
        0f
    ) {
        0L
    } else {
        (dx / width * FullWidthSeconds * 1000).roundToLong()
    }

    fun clamp(positionMs: Long, durationMs: Long): Long = when {
        positionMs < 0 -> 0L
        durationMs > 0 && positionMs > durationMs -> durationMs
        else -> positionMs
    }

    // The volume after a drag up or down the height of the screen, from 0 to 100.
    fun dragVolume(start: Float, dy: Float, height: Float): Float = if (height <=
        0f
    ) {
        start
    } else {
        (start - dy / height * 100).coerceIn(0f, 100f)
    }

    fun showUpNext(hasNext: Boolean, positionMs: Long, durationMs: Long): Boolean {
        if (!hasNext || durationMs <= 0) return false
        val remaining = durationMs - positionMs
        return remaining >= 0 && remaining <= UpNextWindowMs
    }

    // How far a jump goes as text, such as +1:30 or -0:05.
    fun seekHint(targetMs: Long, fromMs: Long): String {
        val difference = targetMs - fromMs
        val total = abs(difference) / 1000
        return (if (difference < 0) "-" else "+") + "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
    }
}
