package com.sumizuri.app.feature.updates.row

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.history_remove
import com.sumizuri.app.shared.resources.selection_mark_read
import org.jetbrains.compose.resources.stringResource

// Lets a row be swiped to the left to do its action, which marks it read or takes it out of the list. The row leaves the page when that is done, so the swipe itself never moves it away.
@Composable
internal fun FeedSwipe(enabled: Boolean, action: FeedAction, onAction: () -> Unit, content: @Composable () -> Unit) {
    if (!enabled) {
        content()
        return
    }
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onAction()
            state.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    SwipeToDismissBox(
        state,
        backgroundContent = {
            if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                val colors = MaterialTheme.colorScheme
                val remove = action == FeedAction.Remove
                val back = if (remove) colors.errorContainer else colors.primaryContainer
                val front = if (remove) colors.onErrorContainer else colors.onPrimaryContainer
                Row(
                    Modifier.fillMaxSize().background(back).padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        stringResource(if (remove) Res.string.history_remove else Res.string.selection_mark_read),
                        color = front,
                    )
                    Icon(
                        if (remove) Icons.Outlined.DeleteOutline else Icons.Outlined.DoneAll,
                        null,
                        Modifier.padding(start = 8.dp),
                        tint = front,
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false,
    ) { content() }
}
