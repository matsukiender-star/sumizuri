package com.sumizuri.app.feature.player.overlay

import com.sumizuri.app.data.model.player.PlayerPrefs
import com.sumizuri.app.playback.SubtitleStyle

// How the subtitles look for these choices.
internal fun PlayerPrefs.subtitleStyle() = SubtitleStyle(
    sizeSp = PlayerPrefs.SubtitleSizes[subtitleSize.coerceIn(0, 2)],
    color = PlayerPrefs.SubtitleColors[subtitleColor.coerceIn(0, 3)],
    background = subtitleBackground,
    bold = subtitleBold,
    raise = subtitleRaise,
)
