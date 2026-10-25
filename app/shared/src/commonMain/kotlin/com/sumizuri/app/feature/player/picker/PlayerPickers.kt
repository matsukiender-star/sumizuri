package com.sumizuri.app.feature.player.picker

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.SubtitlesOff
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.feature.player.SessionState
import com.sumizuri.app.feature.player.VideoChoice
import com.sumizuri.app.feature.player.overlay.speedText
import com.sumizuri.app.playback.AspectMode
import com.sumizuri.app.playback.PlayerState
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.player_audio
import com.sumizuri.app.shared.resources.player_audio_dub
import com.sumizuri.app.shared.resources.player_audio_sub
import com.sumizuri.app.shared.resources.player_episode_playing
import com.sumizuri.app.shared.resources.player_episode_watched
import com.sumizuri.app.shared.resources.player_episodes
import com.sumizuri.app.shared.resources.player_fit
import com.sumizuri.app.shared.resources.player_log_hide
import com.sumizuri.app.shared.resources.player_log_show
import com.sumizuri.app.shared.resources.player_quality
import com.sumizuri.app.shared.resources.player_server
import com.sumizuri.app.shared.resources.player_settings_title
import com.sumizuri.app.shared.resources.player_speed
import com.sumizuri.app.shared.resources.player_subtitle_load_file
import com.sumizuri.app.shared.resources.player_subtitles
import com.sumizuri.app.shared.resources.player_subtitles_off
import com.sumizuri.app.ui.kit.card.CardPlace
import com.sumizuri.app.ui.text.numberedUnit
import org.jetbrains.compose.resources.stringResource

// The panel of one choice, with its lines together in one card.
@Composable
internal fun PlayerPicker(
    picker: Picker,
    session: SessionState,
    engine: PlayerState,
    aspect: AspectMode,
    logShown: Boolean,
    calls: PickerCalls,
) {
    val video = session.current
    val versions = VideoChoice.versions(session.videos, video)
    when (picker) {
        Picker.Quality -> {
            val lines = session.videos.mapIndexed { index, item ->
                Line(
                    Icons.Outlined.HighQuality,
                    item.quality ?: stringResource(Res.string.player_server, index + 1),
                    { calls.onVideo(item) },
                    selected =
                    item == video,
                )
            }
            PickerPanel(stringResource(Res.string.player_quality), calls.onClose) { lines(lines) }
        }

        Picker.Speed -> {
            val lines = PickerSpeeds.map { speed ->
                Line(
                    Icons.Outlined.Speed,
                    speedText(speed) + "×",
                    { calls.onSpeed(speed) },
                    selected =
                    kotlin.math.abs(engine.speed - speed) < 0.001f,
                )
            }
            PickerPanel(stringResource(Res.string.player_speed), calls.onClose) { lines(lines) }
        }

        Picker.Subtitles -> {
            val lines = buildList {
                add(
                    Line(
                        Icons.Outlined.SubtitlesOff,
                        stringResource(Res.string.player_subtitles_off),
                        { calls.onSubtitle(null) },
                        selected =
                        engine.subtitle == null,
                    ),
                )
                for (track in engine.subtitles) {
                    add(
                        Line(
                            Icons.Outlined.Subtitles,
                            trackName(track),
                            { calls.onSubtitle(track.id) },
                            selected =
                            engine.subtitle == track.id,
                        ),
                    )
                }
                add(
                    Line(
                        Icons.Outlined.FileOpen,
                        stringResource(Res.string.player_subtitle_load_file),
                        calls.onLoadSubtitle,
                    ),
                )
            }
            PickerPanel(stringResource(Res.string.player_subtitles), calls.onClose) { lines(lines) }
        }

        Picker.Audio -> {
            val lines = buildList {
                // A source that gives sub and dub as separate videos switches between them here.
                if (versions != null) {
                    add(
                        Line(
                            Icons.Outlined.ClosedCaption,
                            stringResource(Res.string.player_audio_sub),
                            { calls.onVideo(versions.first) },
                            selected =
                            video != null && VideoChoice.isSub(video),
                        ),
                    )
                    add(
                        Line(
                            Icons.Outlined.RecordVoiceOver,
                            stringResource(Res.string.player_audio_dub),
                            { calls.onVideo(versions.second) },
                            selected =
                            video != null && VideoChoice.isDub(video),
                        ),
                    )
                }
                for (track in engine.audios) {
                    add(
                        Line(
                            Icons.Outlined.AudioFile,
                            trackName(track),
                            { calls.onAudio(track.id) },
                            selected =
                            engine.audio == track.id,
                        ),
                    )
                }
            }
            PickerPanel(stringResource(Res.string.player_audio), calls.onClose) { lines(lines) }
        }

        Picker.Episodes -> {
            val state = rememberLazyListState(initialFirstVisibleItemIndex = (session.index - 2).coerceAtLeast(0))
            val playingNote = stringResource(Res.string.player_episode_playing)
            val watchedNote = stringResource(Res.string.player_episode_watched)
            val lines = session.episodes.mapIndexed { index, ep ->
                val playing = index == session.index
                Line(
                    icon = if (playing) {
                        Icons.Rounded.PlayCircle
                    } else if (ep.read) {
                        Icons.Outlined.CheckCircleOutline
                    } else {
                        Icons.Outlined.PlayCircleOutline
                    },
                    title =
                    numberedUnit(Kind.Anime, ep.number) +
                        (ep.name?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                    onClick = { calls.onEpisode(ep.id) },
                    subtitle = if (playing) {
                        playingNote
                    } else if (ep.read) {
                        watchedNote
                    } else {
                        null
                    },
                    selected = playing,
                    key = ep.id,
                )
            }
            PickerPanel(stringResource(Res.string.player_episodes), calls.onClose, state) { lines(lines) }
        }

        Picker.Settings -> {
            val lines = buildList {
                if (session.videos.size >
                    1
                ) {
                    add(
                        Line(Icons.Outlined.HighQuality, stringResource(Res.string.player_quality), {
                            calls.onOpen(Picker.Quality)
                        }, value = video?.quality.orEmpty(), opens = true),
                    )
                }
                if (hasSubtitleChoices(engine, video)) {
                    add(
                        Line(
                            Icons.Outlined.Subtitles,
                            stringResource(Res.string.player_subtitles),
                            { calls.onOpen(Picker.Subtitles) },
                            value =
                            engine.subtitles.firstOrNull { it.id == engine.subtitle }?.let { trackName(it) }
                                ?: stringResource(Res.string.player_subtitles_off),
                            opens = true,
                        ),
                    )
                }
                if (hasAnyAudio(engine, video) || versions != null) {
                    add(
                        Line(
                            Icons.Outlined.AudioFile,
                            stringResource(Res.string.player_audio),
                            { calls.onOpen(Picker.Audio) },
                            value = engine.audios
                                .firstOrNull { it.id == engine.audio }
                                ?.let { trackName(it) }
                                .orEmpty(),
                            opens = true,
                        ),
                    )
                }
                add(
                    Line(
                        Icons.Outlined.Speed,
                        stringResource(Res.string.player_speed),
                        { calls.onOpen(Picker.Speed) },
                        value =
                        speedText(engine.speed) + "×",
                        opens = true,
                    ),
                )
                add(
                    Line(
                        Icons.Outlined.AspectRatio,
                        stringResource(Res.string.player_fit),
                        calls.onFit,
                        value = stringResource(aspect.label),
                        opens = true,
                    ),
                )
                add(
                    Line(
                        Icons.Outlined.Terminal,
                        stringResource(if (logShown) Res.string.player_log_hide else Res.string.player_log_show),
                        calls.onToggleLog,
                    ),
                )
            }
            PickerPanel(stringResource(Res.string.player_settings_title), calls.onClose) { lines(lines) }
        }
    }
}

// One line of a panel, worked out before the list is drawn so each knows its place in the card.
private class Line(
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit,
    val subtitle: String? = null,
    val selected: Boolean = false,
    val value: String? = null,
    val opens: Boolean = false,
    val key: Any? = null,
)

// The lines of a panel as the rows of one card.
private fun LazyListScope.lines(lines: List<Line>) {
    itemsIndexed(lines, key = { index, line -> line.key ?: index }) { index, line ->
        PickerRow(
            line.icon,
            line.title,
            line.onClick,
            CardPlace(index, lines.size),
            line.subtitle,
            line.selected,
            line.value,
            line.opens,
        )
    }
}
