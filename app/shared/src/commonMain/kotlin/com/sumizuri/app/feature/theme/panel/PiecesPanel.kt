package com.sumizuri.app.feature.theme.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.feature.theme.EditorPart
import com.sumizuri.app.feature.theme.ThemeEditorState
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.editor_piece_corners
import com.sumizuri.app.shared.resources.editor_switch_icons
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import com.sumizuri.app.ui.kit.setting.SwitchSetting

// The tab of the smaller pieces: chips, switches, dialogs and the bar at the bottom, each under its own heading. A piece tapped on the preview scrolls its heading into view.
@Composable
internal fun PiecesPanel(state: ThemeEditorState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Group(state, EditorPart.Buttons) {
            ShapeEditor(state, Res.string.editor_piece_corners, {
                it.chip
            }, { shapes, spec -> shapes.copy(chip = spec) }, startOpen = true)
        }
        Group(state, EditorPart.Controls) {
            SwitchSetting(Res.string.editor_switch_icons, state.draft.options.controls.switchIcons, { on ->
                state
                    .options {
                        it.copy(controls = it.controls.copy(switchIcons = on))
                    }
            })
        }
        Group(state, EditorPart.Popups) {
            ShapeEditor(state, Res.string.editor_piece_corners, {
                it.dialog
            }, { shapes, spec -> shapes.copy(dialog = spec) }, startOpen = true)
        }
        Group(state, EditorPart.Navigation) {
            ShapeEditor(state, Res.string.editor_piece_corners, {
                it.navBar
            }, { shapes, spec -> shapes.copy(navBar = spec) }, startOpen = true)
        }
    }
}

// The settings of one piece under its name, brought into view when the piece is the one in focus.
@Composable
private fun Group(state: ThemeEditorState, part: EditorPart, content: @Composable () -> Unit) {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(state.focus) { if (state.focus == part) requester.bringIntoView() }
    Column(Modifier.bringIntoViewRequester(requester)) {
        AppSectionLabel(part.label)
        content()
    }
}
