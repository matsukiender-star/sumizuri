package com.sumizuri.app.ui.kit.cover

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.SourceState
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.ui.kit.cover.badge.PickBadge
import com.sumizuri.app.ui.kit.cover.badge.SavedBadge
import com.sumizuri.app.ui.kit.cover.badge.SourceWarningBadge
import com.sumizuri.app.ui.kit.cover.badge.UnreadBadge
import com.sumizuri.app.ui.motion.entrance
import com.sumizuri.app.ui.motion.feedbackScale
import com.sumizuri.app.ui.text.displayName

// How a cover is looked at in a grid that picks: whether titles are being picked, and whether this one is.
class CoverPick(val picking: Boolean, val picked: Boolean)

// A cover with its name underneath, a ringed count when something is unread, a mark when it is saved and a warning when its source is missing or off. The count and the name can be left out, and while titles are picked the cover shows a ring that is filled for the ones picked.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoverTile(
    title: Title,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    saved: Boolean = false,
    showUnread: Boolean = true,
    showName: Boolean = true,
    pick: CoverPick? = null,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    Column(
        modifier
            .entrance()
            .feedbackScale(
                source,
            ).combinedClickable(
                interactionSource = source,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
                onDoubleClick = onDoubleClick,
            ),
    ) {
        Box {
            CoverArt(title, Modifier.alpha(if (pick?.picked == true) 0.55f else 1f))
            if (saved) SavedBadge(Modifier.align(Alignment.TopStart).padding(6.dp))
            if (pick?.picking == true) PickBadge(pick.picked, Modifier.align(Alignment.TopStart).padding(6.dp))
            if (title.sourceState !=
                SourceState.Ready
            ) {
                SourceWarningBadge(Modifier.align(Alignment.BottomStart).padding(6.dp))
            }
            if (showUnread && title.unread > 0) {
                UnreadBadge(title.unread, Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
        }
        if (showName) {
            Text(
                title.displayName(),
                Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
