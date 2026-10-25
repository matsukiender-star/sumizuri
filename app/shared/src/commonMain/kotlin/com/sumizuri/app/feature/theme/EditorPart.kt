package com.sumizuri.app.feature.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.ui.graphics.vector.ImageVector
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.editor_part_app
import com.sumizuri.app.shared.resources.editor_part_buttons
import com.sumizuri.app.shared.resources.editor_part_cards
import com.sumizuri.app.shared.resources.editor_part_controls
import com.sumizuri.app.shared.resources.editor_part_cover
import com.sumizuri.app.shared.resources.editor_part_motion
import com.sumizuri.app.shared.resources.editor_part_navigation
import com.sumizuri.app.shared.resources.editor_part_popups
import com.sumizuri.app.shared.resources.editor_part_text
import com.sumizuri.app.shared.resources.editor_section_backdrop
import com.sumizuri.app.shared.resources.editor_section_basics
import com.sumizuri.app.shared.resources.editor_section_colors
import com.sumizuri.app.shared.resources.editor_section_covers
import com.sumizuri.app.shared.resources.editor_section_lists
import com.sumizuri.app.shared.resources.editor_section_motion
import com.sumizuri.app.shared.resources.editor_section_pieces
import com.sumizuri.app.shared.resources.editor_section_text
import org.jetbrains.compose.resources.StringResource

// The tabs of the editor, each a page of settings that is always one tap away.
internal enum class EditorSection(val label: StringResource, val icon: ImageVector) {
    Basics(Res.string.editor_section_basics, Icons.Outlined.AutoAwesome),
    Colors(Res.string.editor_section_colors, Icons.Outlined.Palette),
    Text(Res.string.editor_section_text, Icons.Outlined.TextFields),
    Covers(Res.string.editor_section_covers, Icons.Outlined.PhotoLibrary),
    Lists(Res.string.editor_section_lists, Icons.Outlined.ViewAgenda),
    Pieces(Res.string.editor_section_pieces, Icons.Outlined.ToggleOn),
    Motion(Res.string.editor_section_motion, Icons.Outlined.Animation),
    Backdrop(Res.string.editor_section_backdrop, Icons.Outlined.Wallpaper),
}

// The pieces of the preview that answer a tap, each with the tab its settings are on.
internal enum class EditorPart(val label: StringResource, val section: EditorSection) {
    App(Res.string.editor_part_app, EditorSection.Backdrop),
    Text(Res.string.editor_part_text, EditorSection.Text),
    Cards(Res.string.editor_part_cards, EditorSection.Lists),
    Cover(Res.string.editor_part_cover, EditorSection.Covers),
    Buttons(Res.string.editor_part_buttons, EditorSection.Pieces),
    Controls(Res.string.editor_part_controls, EditorSection.Pieces),
    Popups(Res.string.editor_part_popups, EditorSection.Pieces),
    Navigation(Res.string.editor_part_navigation, EditorSection.Pieces),
    Motion(Res.string.editor_part_motion, EditorSection.Motion),
}

// The piece of the preview a tab is about, which the preview scrolls to when the tab opens.
internal val EditorSection.firstPart: EditorPart?
    get() = when (this) {
        EditorSection.Basics, EditorSection.Colors -> null
        EditorSection.Text -> EditorPart.Text
        EditorSection.Covers -> EditorPart.Cover
        EditorSection.Lists -> EditorPart.Cards
        EditorSection.Pieces -> EditorPart.Buttons
        EditorSection.Motion, EditorSection.Backdrop -> null
    }
