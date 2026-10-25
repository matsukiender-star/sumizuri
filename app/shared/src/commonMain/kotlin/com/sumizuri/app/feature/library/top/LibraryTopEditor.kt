package com.sumizuri.app.feature.library.top

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.library.LibraryPrefs
import com.sumizuri.app.data.model.library.LibrarySection
import com.sumizuri.app.data.model.library.ShelfStyle
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.count_off
import com.sumizuri.app.shared.resources.count_titles
import com.sumizuri.app.shared.resources.display_drag
import com.sumizuri.app.shared.resources.display_hide_row
import com.sumizuri.app.shared.resources.display_home_hint
import com.sumizuri.app.shared.resources.display_part_count
import com.sumizuri.app.shared.resources.display_show_row
import com.sumizuri.app.shared.resources.display_style
import com.sumizuri.app.shared.resources.ok
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.card.AppCard
import com.sumizuri.app.ui.kit.dialog.AppDialog
import com.sumizuri.app.ui.kit.scroll.DragReorderColumn
import com.sumizuri.app.ui.kit.setting.ChoiceSetting
import org.jetbrains.compose.resources.stringResource

// The parts of the top of the library as a list of cards in the way the Flutter app edits its shelves: a tile with an icon, the name with how it looks, an eye that hides it and a handle to drag it by. Tapping a card opens how it looks and how many titles it holds. A hidden card stays in the list, faded.
@Composable
internal fun LibraryTopEditor(prefs: LibraryPrefs, onChange: ((LibraryPrefs) -> LibraryPrefs) -> Unit) {
    var editing by remember { mutableStateOf<LibrarySection?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(Res.string.display_home_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        DragReorderColumn(prefs.sectionOrder, {
            it
        }, { saved -> onChange { it.copy(sectionOrder = saved) } }) { section, lifted, handle, rowModifier ->
            PartCard(
                prefs,
                section,
                lifted,
                handle,
                onEdit = { editing = section },
                onToggle = { on -> onChange { it.withCount(section, if (on) LibraryPrefs.ShownCount else 0) } },
                modifier = rowModifier,
            )
        }
    }
    editing?.let { section -> PartDialog(prefs, section, onChange) { editing = null } }
}

// One card of the list.
@Composable
private fun PartCard(
    prefs: LibraryPrefs,
    section: LibrarySection,
    lifted: Boolean,
    handle: Modifier,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val covers = section == LibrarySection.Library
    val count = prefs.countOf(section)
    val shown = covers || count > 0
    val note = if (covers) {
        null
    } else {
        listOf(
            stringResource(prefs.styleOf(section).label),
            if (count == 0) stringResource(Res.string.count_off) else stringResource(Res.string.count_titles, count),
        ).joinToString(" · ")
    }
    AppCard(
        modifier.fillMaxWidth().shadow(if (lifted) 6.dp else 0.dp, MaterialTheme.shapes.medium),
        onClick = if (covers) null else onEdit,
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp).alpha(if (shown) 1f else 0.55f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(
                        40.dp,
                    ).background(
                        colors.surfaceContainerHighest,
                        MaterialTheme.shapes.small,
                    ).border(1.dp, colors.outlineVariant, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) { Icon(iconOf(section), null, Modifier.size(19.dp), tint = colors.onSurfaceVariant) }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(section.label),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = colors.outline)
            }
            if (covers) {
                Icon(Icons.Outlined.Lock, null, Modifier.padding(end = 4.dp).size(18.dp), tint = colors.outline)
            } else {
                IconButton({ onToggle(!shown) }) {
                    Icon(
                        if (shown) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        stringResource(if (shown) Res.string.display_hide_row else Res.string.display_show_row),
                        tint = if (shown) colors.onSurfaceVariant else colors.outline,
                    )
                }
            }
            Box(Modifier.padding(8.dp).then(handle)) {
                Icon(Icons.Filled.DragHandle, stringResource(Res.string.display_drag), tint = colors.outline)
            }
        }
    }
}

// How a part looks and how many titles it holds.
@Composable
private fun PartDialog(
    prefs: LibraryPrefs,
    section: LibrarySection,
    onChange: ((LibraryPrefs) -> LibraryPrefs) -> Unit,
    onDone: () -> Unit,
) {
    val counts = LibraryPrefs.Counts
    AppDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(section.label)) },
        text = {
            Column {
                ChoiceSetting(Res.string.display_style, ShelfStyle.entries, prefs.styleOf(section), {
                    stringResource(it.label)
                }) { style -> onChange { it.withStyle(section, style) } }
                ChoiceSetting(Res.string.display_part_count, counts, prefs.countOf(section), { n ->
                    if (n ==
                        0
                    ) {
                        stringResource(Res.string.count_off)
                    } else {
                        stringResource(Res.string.count_titles, n)
                    }
                }) { n -> onChange { it.withCount(section, n) } }
            }
        },
        confirmButton = { InkButton(onDone) { Text(stringResource(Res.string.ok)) } },
    )
}

private fun iconOf(section: LibrarySection): ImageVector = when (section) {
    LibrarySection.Continue -> Icons.Outlined.PlayCircle
    LibrarySection.Updates -> Icons.Outlined.NewReleases
    LibrarySection.History -> Icons.Outlined.History
    LibrarySection.Library -> Icons.Outlined.CollectionsBookmark
}
