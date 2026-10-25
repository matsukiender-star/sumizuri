package com.sumizuri.app.feature.updates.row

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.sumizuri.app.data.model.feed.FeedRowStyle
import com.sumizuri.app.data.model.feed.UpdateItem
import com.sumizuri.app.data.model.library.LibraryDisplayStyle
import com.sumizuri.app.ui.motion.entrance

// The titles of one stretch of time, as cells of a grid or as rows in the style the person chose. The action is what the button and the swipe of a row do, and onAction gets the item they were on.
internal fun LazyGridScope.feed(
    group: List<UpdateItem>,
    style: LibraryDisplayStyle,
    rowStyle: FeedRowStyle,
    rowSwipe: Boolean,
    action: FeedAction,
    onOpenTitle: (Long) -> Unit,
    onAction: (List<UpdateItem>) -> Unit,
) {
    if (style.isGrid) {
        itemsIndexed(group, key = { _, item -> "${item.title.id}_${item.chapterIds.first()}" }) { _, item ->
            UpdateGridTile(item, style, {
                onOpenTitle(item.title.id)
            }, { onAction(listOf(item)) }, Modifier.animateItem().entrance())
        }
        return
    }
    itemsIndexed(
        group,
        key = { _, item -> "${item.title.id}_${item.chapterIds.first()}" },
        span = { _, _ -> GridItemSpan(maxLineSpan) },
    ) { index, item ->
        val open = { onOpenTitle(item.title.id) }
        val act = { onAction(listOf(item)) }
        val lift = if (rowStyle == FeedRowStyle.Cards) Modifier else Modifier.entrance()
        Readable(true, Modifier.animateItem().then(lift)) {
            FeedSwipe(rowSwipe, action, act) {
                when (rowStyle) {
                    FeedRowStyle.Cards -> UpdateCardRow(
                        item,
                        style == LibraryDisplayStyle.CompactList,
                        open,
                        act,
                        action,
                    )

                    FeedRowStyle.Timeline -> UpdateTimelineRow(item, index == group.lastIndex, open, act, action)

                    FeedRowStyle.Inbox -> Column {
                        if (index >
                            0
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        }
                        UpdateInboxRow(item, open, act, action)
                    }
                }
            }
        }
    }
}
