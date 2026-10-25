package com.sumizuri.app.feature.library.top

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sumizuri.app.feature.library.LibraryPickCalls
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.library_favorite
import com.sumizuri.app.shared.resources.library_shelves
import com.sumizuri.app.shared.resources.library_unfavorite
import com.sumizuri.app.shared.resources.pick_migrate
import com.sumizuri.app.shared.resources.remove
import com.sumizuri.app.shared.resources.selection_mark_read
import com.sumizuri.app.shared.resources.selection_mark_unread
import com.sumizuri.app.shared.resources.title_action_move
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// The bar along the bottom with a name under each action. Moving to another source is done for one title in a page of its own, and for several in the page for moving many. The favorite action is null when favorites are off, and otherwise says whether it adds or takes off.
@Composable
internal fun LibraryPickBottom(count: Int, favorite: Boolean?, calls: LibraryPickCalls, modifier: Modifier = Modifier) {
    Surface(
        modifier.fillMaxWidth().padding(12.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
    ) {
        Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Action(
                Icons.Outlined.DoneAll,
                Res.string.selection_mark_read,
                Modifier.weight(1f),
            ) { calls.onMarkRead(true) }
            Action(
                Icons.Outlined.RemoveDone,
                Res.string.selection_mark_unread,
                Modifier.weight(1f),
            ) { calls.onMarkRead(false) }
            if (favorite != null) {
                Action(
                    if (favorite) Icons.Outlined.StarBorder else Icons.Outlined.Star,
                    if (favorite) Res.string.library_favorite else Res.string.library_unfavorite,
                    Modifier.weight(1f),
                ) {
                    calls.onFavorite(favorite)
                }
            }
            Action(Icons.AutoMirrored.Outlined.Label, Res.string.library_shelves, Modifier.weight(1f), calls.onShelves)
            if (count ==
                1
            ) {
                Action(Icons.Outlined.SwapHoriz, Res.string.title_action_move, Modifier.weight(1f), calls.onMove)
            } else {
                Action(Icons.Outlined.SwapHoriz, Res.string.pick_migrate, Modifier.weight(1f), calls.onMigrate)
            }
            Action(Icons.Outlined.PlaylistRemove, Res.string.remove, Modifier.weight(1f), calls.onRemove)
        }
    }
}

@Composable
private fun Action(
    icon: ImageVector,
    label: org.jetbrains.compose.resources.StringResource,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick) { Icon(icon, stringResource(label)) }
        Text(
            stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
        )
    }
}
