package com.sumizuri.app.feature.library.shelves

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
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.library.Category
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.display_drag
import com.sumizuri.app.shared.resources.shelves_every_kind
import com.sumizuri.app.shared.resources.shelves_left_out
import com.sumizuri.app.shared.resources.shelves_smart
import com.sumizuri.app.shared.resources.titles_count
import com.sumizuri.app.ui.kit.card.AppCard
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// One shelf as a card, in the way the parts of the library top are: a tile with an icon, the name with how many titles it holds and whether it is left out of updates, and a handle to drag it by. Tapping the card opens the dialog to change it.
@Composable
internal fun ShelfCard(shelf: Category, lifted: Boolean, handle: Modifier, onEdit: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val count = pluralStringResource(Res.plurals.titles_count, shelf.titles, shelf.titles)
    val scope = shelf.kind?.let { stringResource(it.label) } ?: stringResource(Res.string.shelves_every_kind)
    val note = listOfNotNull(
        scope,
        count,
        if (shelf.isSmart) {
            stringResource(
                Res.string.shelves_smart,
            )
        } else {
            null
        },
        shelf.sort?.let {
            stringResource(it.label)
        },
        if (shelf.excludeFromUpdate) stringResource(Res.string.shelves_left_out) else null,
    ).joinToString(" · ")
    AppCard(modifier.fillMaxWidth().shadow(if (lifted) 6.dp else 0.dp, MaterialTheme.shapes.medium), onClick = onEdit) {
        Row(
            Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
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
            ) { Icon(Icons.AutoMirrored.Outlined.Label, null, Modifier.size(19.dp), tint = colors.onSurfaceVariant) }
            Column(Modifier.weight(1f)) {
                Text(shelf.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(note, style = MaterialTheme.typography.bodySmall, color = colors.outline)
            }
            Box(Modifier.padding(8.dp).then(handle)) {
                Icon(Icons.Filled.DragHandle, stringResource(Res.string.display_drag), tint = colors.outline)
            }
        }
    }
}
