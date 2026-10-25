package com.sumizuri.app.feature.updates.row

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.feed.UpdateItem
import com.sumizuri.app.data.model.library.LibraryDisplayStyle
import com.sumizuri.app.ui.kit.cover.CoverArt
import com.sumizuri.app.ui.kit.cover.badge.UnreadBadge
import com.sumizuri.app.ui.motion.feedbackScale
import com.sumizuri.app.ui.text.displayName

// A new chapter in a grid: the cover with the number of new chapters on it when there are several. The name and the chapter are under the cover, over it, or left off, as the style says. A press and hold marks it read.
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun UpdateGridTile(
    item: UpdateItem,
    style: LibraryDisplayStyle,
    onOpen: () -> Unit,
    onMarkRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val caption = feedSpanText(item.title.kind, item.numbers, null)
    val tap = modifier
        .feedbackScale(
            source,
        ).combinedClickable(interactionSource = source, indication = null, onClick = onOpen, onLongClick = onMarkRead)
    when (style) {
        LibraryDisplayStyle.CompactGrid -> Box(tap) {
            Cover(item)
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))))
                    .padding(start = 8.dp, end = 8.dp, top = 22.dp, bottom = 6.dp),
            ) {
                Text(
                    item.title.displayName(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                )
            }
        }

        LibraryDisplayStyle.CoverGrid -> Box(tap) { Cover(item) }

        else -> Column(tap) {
            Cover(item)
            Text(
                item.title.displayName(),
                Modifier.padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Cover(item: UpdateItem) {
    Box {
        CoverArt(item.title)
        if (item.count > 1) UnreadBadge(item.count, Modifier.align(Alignment.TopEnd).padding(6.dp))
    }
}
