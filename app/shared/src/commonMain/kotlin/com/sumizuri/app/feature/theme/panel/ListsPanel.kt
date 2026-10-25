package com.sumizuri.app.feature.theme.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.sumizuri.app.feature.theme.ThemeEditorState
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.editor_card_corners
import com.sumizuri.app.shared.resources.editor_chevrons
import com.sumizuri.app.shared.resources.editor_corners_each
import com.sumizuri.app.shared.resources.editor_icon_tiles
import com.sumizuri.app.shared.resources.editor_item_corners
import com.sumizuri.app.shared.resources.editor_tile_corners
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import com.sumizuri.app.ui.kit.setting.SwitchSetting

// The tab of cards and list rows: the tiles behind icons and the arrows at the end of rows, and the corners of cards, rows and tiles.
@Composable
internal fun ListsPanel(state: ThemeEditorState) {
    val components = state.draft.options.components
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SwitchSetting(Res.string.editor_icon_tiles, components.listIconTiles, { on ->
            state.options {
                it.copy(components = it.components.copy(listIconTiles = on))
            }
        })
        SwitchSetting(Res.string.editor_chevrons, components.listChevron, { on ->
            state.options {
                it.copy(components = it.components.copy(listChevron = on))
            }
        })
        AppSectionLabel(Res.string.editor_corners_each)
        ShapeEditor(state, Res.string.editor_card_corners, { it.card }, { shapes, spec -> shapes.copy(card = spec) })
        ShapeEditor(state, Res.string.editor_item_corners, { it.item }, { shapes, spec -> shapes.copy(item = spec) })
        ShapeEditor(
            state,
            Res.string.editor_tile_corners,
            { it.iconTile },
            { shapes, spec -> shapes.copy(iconTile = spec) },
        )
    }
}
