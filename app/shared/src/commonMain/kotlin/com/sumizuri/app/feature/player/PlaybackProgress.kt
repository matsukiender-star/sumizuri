package com.sumizuri.app.feature.player

// Where an episode was left and whether it counts as watched, as fractions from 0 to 1 of its length.
object PlaybackProgress {
    // An episode that was left this far in starts again from the beginning, and one left before the minimum is not worth resuming.
    private const val RestartAt = 0.95
    private const val MinimumResumeMs = 5000L

    // How far the position is, or null while the length is not known.
    fun fraction(positionMs: Long, durationMs: Long): Double? = if (durationMs <=
        0
    ) {
        null
    } else {
        (positionMs.toDouble() / durationMs).coerceIn(0.0, 1.0)
    }

    // The place to start from, in milliseconds, or null when the episode should start from the beginning.
    fun resume(stored: Double?, durationMs: Long): Long? {
        if (stored == null || durationMs <= 0 || stored >= RestartAt) return null
        val position = (stored * durationMs).toLong()
        return position.takeIf { it >= MinimumResumeMs }
    }
}

// A time in milliseconds as minutes and seconds, with the hours when there are any.
internal fun formatTime(milliseconds: Long): String {
    val total = (milliseconds / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours >
        0
    ) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
