package com.sumizuri.app.feature.updates.row

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.history_remove
import com.sumizuri.app.shared.resources.selection_mark_read
import org.jetbrains.compose.resources.stringResource

// The button at the end of a feed row, which is there as well as the swipe since a mouse cannot swipe easily. Taking a row out of the history is drawn quieter than marking read, since it is not what the page is for and a row should not look like it asks to be closed.
@Composable
internal fun FeedActionButton(action: FeedAction, onClick: () -> Unit) {
    when (action) {
        FeedAction.MarkRead -> IconButton(onClick) {
            Icon(Icons.Outlined.DoneAll, stringResource(Res.string.selection_mark_read), Modifier.size(20.dp))
        }

        FeedAction.Remove -> IconButton(onClick) {
            Icon(
                Icons.Outlined.Close,
                stringResource(Res.string.history_remove),
                Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

// What the button at the end of a feed row does: marks it read on the updates page, takes it out of the list on the history page.
enum class FeedAction { MarkRead, Remove }
