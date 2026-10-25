package com.sumizuri.app.feature.player.picker

import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.runtime.Composable
import com.sumizuri.app.playback.PlayerState
import com.sumizuri.app.playback.PlayerTrack
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.player_track_number
import com.sumizuri.app.source.model.SourceVideo
import org.jetbrains.compose.resources.stringResource

// The speeds the speed choice offers.
internal val PickerSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

// A track as a person reads it: its name, which has the language in it, else its number in the list.
@Composable
internal fun trackName(track: PlayerTrack): String = track.label.ifBlank {
    stringResource(
        Res.string.player_track_number,
        track.ordinal.takeIf { it > 0 }?.toString() ?: track.id,
    )
}

// Whether the video has subtitles to pick from, sound to pick from, and a choice between a subbed and a dubbed version.
internal fun hasSubtitleChoices(engine: PlayerState, video: SourceVideo?): Boolean =
    engine.subtitles.isNotEmpty() || video?.subtitles?.isNotEmpty() == true

internal fun hasAnyAudio(engine: PlayerState, video: SourceVideo?): Boolean =
    engine.audios.isNotEmpty() || video?.audioTracks?.isNotEmpty() == true

// The choices the player opens in a panel.
internal enum class Picker { Episodes, Speed, Quality, Subtitles, Audio, Settings }

// What the choices can do, given by the screen. Each closes the panel first.
internal class PickerCalls(
    val onClose: () -> Unit,
    val onOpen: (Picker) -> Unit,
    val onEpisode: (Long) -> Unit,
    val onSpeed: (Float) -> Unit,
    val onVideo: (SourceVideo) -> Unit,
    val onSubtitle: (String?) -> Unit,
    val onLoadSubtitle: () -> Unit,
    val onAudio: (String) -> Unit,
    val onFit: () -> Unit,
    val onToggleLog: () -> Unit,
)
