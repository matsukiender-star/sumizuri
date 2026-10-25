package com.sumizuri.app.feature.settings.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumizuri.app.data.model.player.PlayerPrefs
import com.sumizuri.app.data.model.player.SubtitleBackground
import com.sumizuri.app.playback.SubtitleStyle
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.player_bg_box
import com.sumizuri.app.shared.resources.player_bg_none
import com.sumizuri.app.shared.resources.player_bg_outline
import com.sumizuri.app.shared.resources.player_color_cyan
import com.sumizuri.app.shared.resources.player_color_green
import com.sumizuri.app.shared.resources.player_color_white
import com.sumizuri.app.shared.resources.player_color_yellow
import com.sumizuri.app.shared.resources.player_group_subtitles
import com.sumizuri.app.shared.resources.player_position_high
import com.sumizuri.app.shared.resources.player_position_low
import com.sumizuri.app.shared.resources.player_position_usual
import com.sumizuri.app.shared.resources.player_set_language
import com.sumizuri.app.shared.resources.player_set_language_none
import com.sumizuri.app.shared.resources.player_set_show_subtitles
import com.sumizuri.app.shared.resources.player_set_sub_background
import com.sumizuri.app.shared.resources.player_set_sub_bold
import com.sumizuri.app.shared.resources.player_set_sub_color
import com.sumizuri.app.shared.resources.player_set_sub_position
import com.sumizuri.app.shared.resources.player_set_sub_size
import com.sumizuri.app.shared.resources.player_size_large
import com.sumizuri.app.shared.resources.player_size_medium
import com.sumizuri.app.shared.resources.player_size_small
import com.sumizuri.app.shared.resources.player_sub_preview
import com.sumizuri.app.ui.kit.setting.ChoiceSetting
import com.sumizuri.app.ui.kit.setting.SettingsSection
import com.sumizuri.app.ui.kit.setting.SwitchSetting
import com.sumizuri.app.ui.text.languageName
import com.sumizuri.app.ui.theme.FixedColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// The languages a person can ask for first, and nothing for no choice.
private val Languages = listOf("", "en", "ja", "es", "pt", "fr", "de", "it", "ru", "ar", "id", "vi", "th", "tr", "zh", "ko")

private val SizeNames = listOf(Res.string.player_size_small, Res.string.player_size_medium, Res.string.player_size_large)
private val ColorNames = listOf(Res.string.player_color_white, Res.string.player_color_yellow, Res.string.player_color_cyan, Res.string.player_color_green)
private val PositionNames = listOf(Res.string.player_position_low, Res.string.player_position_usual, Res.string.player_position_high)

private fun backgroundName(background: SubtitleBackground): StringResource = when (background) {
    SubtitleBackground.None -> Res.string.player_bg_none
    SubtitleBackground.Outline -> Res.string.player_bg_outline
    SubtitleBackground.Box -> Res.string.player_bg_box
}

// The group of the subtitles: a line that shows how they will look, then the size, color, style, weight and place of the letters, and which subtitle to turn on.
@Composable
internal fun PlayerSubtitleSettings(prefs: PlayerPrefs, change: ((PlayerPrefs) -> PlayerPrefs) -> Unit) {
    SettingsSection(Res.string.player_group_subtitles) {
        padded { SubtitlePreview(prefs) }
        padded { ChoiceSetting(Res.string.player_set_sub_size, listOf(0, 1, 2), prefs.subtitleSize, { stringResource(SizeNames[it]) }) { value -> change { it.copy(subtitleSize = value) } } }
        padded { ChoiceSetting(Res.string.player_set_sub_color, listOf(0, 1, 2, 3), prefs.subtitleColor, { stringResource(ColorNames[it]) }) { value -> change { it.copy(subtitleColor = value) } } }
        padded { ChoiceSetting(Res.string.player_set_sub_background, SubtitleBackground.entries, prefs.subtitleBackground, { stringResource(backgroundName(it)) }) { value -> change { it.copy(subtitleBackground = value) } } }
        padded { SwitchSetting(Res.string.player_set_sub_bold, prefs.subtitleBold, { on -> change { it.copy(subtitleBold = on) } }) }
        padded { ChoiceSetting(Res.string.player_set_sub_position, listOf(0, 1, 2), prefs.subtitleRaise, { stringResource(PositionNames[it]) }) { value -> change { it.copy(subtitleRaise = value) } } }
        padded { SwitchSetting(Res.string.player_set_show_subtitles, prefs.showSubtitles, { on -> change { it.copy(showSubtitles = on) } }) }
        padded { ChoiceSetting(Res.string.player_set_language, Languages, prefs.subtitleLanguage, { if (it.isEmpty()) stringResource(Res.string.player_set_language_none) else languageName(it) }) { language -> change { it.copy(subtitleLanguage = language) } } }
    }
}

// A sample line over a dark picture, drawn with the look that was chosen.
@Composable
private fun SubtitlePreview(prefs: PlayerPrefs) {
    val sizeSp = PlayerPrefs.SubtitleSizes[prefs.subtitleSize.coerceIn(0, 2)]
    val raise = SubtitleStyle.RaiseDp[prefs.subtitleRaise.coerceIn(0, 2)] / 2
    val box = prefs.subtitleBackground == SubtitleBackground.Box
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(FixedColors.VideoStill),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Text(
            stringResource(Res.string.player_sub_preview),
            Modifier.padding(bottom = (8 + raise).dp).then(if (box) Modifier.background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 6.dp) else Modifier),
            color = Color(PlayerPrefs.SubtitleColors[prefs.subtitleColor.coerceIn(0, 3)]),
            fontSize = sizeSp.sp,
            fontWeight = if (prefs.subtitleBold) FontWeight.Bold else FontWeight.Medium,
            style = TextStyle(shadow = if (prefs.subtitleBackground == SubtitleBackground.Outline) Shadow(Color.Black, Offset(0f, 0f), 8f) else null),
        )
    }
}
