package com.sumizuri.app.feature.theme.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.sumizuri.app.feature.theme.ThemeEditorState
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.editor_badge_style
import com.sumizuri.app.shared.resources.editor_cover_aspect
import com.sumizuri.app.shared.resources.editor_cover_border
import com.sumizuri.app.shared.resources.editor_cover_corners
import com.sumizuri.app.shared.resources.editor_cover_shadow
import com.sumizuri.app.shared.resources.editor_progress
import com.sumizuri.app.shared.resources.editor_progress_floating
import com.sumizuri.app.shared.resources.editor_progress_height
import com.sumizuri.app.shared.resources.editor_progress_style
import com.sumizuri.app.shared.resources.editor_title_lines
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import com.sumizuri.app.ui.kit.setting.ChoiceSetting
import com.sumizuri.app.ui.kit.setting.SwitchSetting
import com.sumizuri.app.ui.theme.spec.CoverAspect
import com.sumizuri.app.ui.theme.spec.CoverBadgeStyle
import com.sumizuri.app.ui.theme.spec.ProgressStyle
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

// The tab of covers: their shape, how many lines of the name show, how the count of unread chapters is drawn, the line around a cover and the shadow under it, the bar of how far a title is read, and the corners.
@Composable
internal fun CoverPanel(state: ThemeEditorState) {
    val covers = state.draft.options.covers
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChoiceSetting(Res.string.editor_cover_aspect, CoverAspect.entries, covers.aspect, {
            choiceLabel(it)
        }) { value -> state.options { it.copy(covers = it.covers.copy(aspect = value)) } }
        ChoiceSetting(Res.string.editor_title_lines, listOf(1, 2, 3), covers.titleLines, {
            it.toString()
        }) { value -> state.options { it.copy(covers = it.covers.copy(titleLines = value)) } }
        ChoiceSetting(Res.string.editor_badge_style, CoverBadgeStyle.entries, covers.badgeStyle, {
            choiceLabel(it)
        }) { value -> state.options { it.copy(covers = it.covers.copy(badgeStyle = value)) } }
        SwitchSetting(Res.string.editor_cover_border, covers.coverBorder, { on ->
            state.options {
                it.copy(covers = it.covers.copy(coverBorder = on))
            }
        })
        SwitchSetting(Res.string.editor_cover_shadow, state.draft.options.effects.coverShadow, { on ->
            state.options {
                it.copy(effects = it.effects.copy(coverShadow = on))
            }
        })
        AppSectionLabel(stringResource(Res.string.editor_progress))
        ChoiceSetting(Res.string.editor_progress_style, ProgressStyle.entries, covers.progressStyle, {
            choiceLabel(it)
        }) { value -> state.options { it.copy(covers = it.covers.copy(progressStyle = value)) } }
        ThemeSlider(state, Res.string.editor_progress_height, covers.progressHeight, 2f..8f, {
            "${it.roundToInt()} dp"
        }) { theme, value ->
            theme.copy(
                options = theme.options.copy(covers = theme.options.covers.copy(progressHeight = value)),
            )
        }
        SwitchSetting(Res.string.editor_progress_floating, covers.progressFloating, { on ->
            state.options {
                it.copy(covers = it.covers.copy(progressFloating = on))
            }
        })
        ShapeEditor(state, Res.string.editor_cover_corners, { it.cover }, { shapes, spec -> shapes.copy(cover = spec) })
        PageColorsPanel(state)
    }
}
