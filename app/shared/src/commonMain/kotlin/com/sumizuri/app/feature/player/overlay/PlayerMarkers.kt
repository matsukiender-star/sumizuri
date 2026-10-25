package com.sumizuri.app.feature.player.overlay

import com.sumizuri.app.source.model.TimeRange

enum class MarkerKind { Intro, Outro }

// The intro or the outro that is playing now, and where skipping it goes. When that is the end of the episode, the next episode is the natural place to go.
class ActiveMarker(val kind: MarkerKind, val skipToMs: Long, val endsEpisode: Boolean = false)

object PlayerMarkers {
    // A marker is offered this long before it starts.
    private const val LeadMs = 3000L

    fun active(positionMs: Long, intro: TimeRange?, outro: TimeRange?, durationMs: Long): ActiveMarker? {
        if (intro != null && within(positionMs, intro)) return ActiveMarker(MarkerKind.Intro, intro.endMs)
        if (outro != null && within(positionMs, outro)) {
            val near = durationMs > 0 && outro.endMs >= durationMs - 3000
            return ActiveMarker(MarkerKind.Outro, outro.endMs, near)
        }
        return null
    }

    private fun within(positionMs: Long, range: TimeRange): Boolean =
        positionMs >= range.startMs - LeadMs && positionMs < range.endMs
}
