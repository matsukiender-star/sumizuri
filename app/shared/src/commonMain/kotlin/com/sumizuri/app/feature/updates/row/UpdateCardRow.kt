package com.sumizuri.app.feature.updates.row

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.feed.UpdateItem
import com.sumizuri.app.ui.kit.card.AppListRow
import com.sumizuri.app.ui.kit.cover.CoverArt
import com.sumizuri.app.ui.text.agoText
import com.sumizuri.app.ui.text.displayName

// A new chapter as a card: a cover, the title, which chapter it is, and at the end the time, the count and the mark read button. The compact list has a smaller cover.
@Composable
internal fun UpdateCardRow(
    item: UpdateItem,
    dense: Boolean,
    onOpen: () -> Unit,
    onAction: () -> Unit,
    action: FeedAction = FeedAction.MarkRead,
) {
    AppListRow(
        title = item.title.displayName(),
        subtitle = feedSpanText(item.title.kind, item.numbers, item.name),
        leading = { CoverArt(item.title, Modifier.width(if (dense) 34.dp else 46.dp), letterSize = 16) },
        trailing = { RowEnd(agoText(item.at), item.count, onAction, action) },
        onClick = onOpen,
    )
}

// The count of chapters a row stands for, when it stands for more than one.
@Composable
private fun CountPill(count: Int) {
    Text(
        count.toString(),
        Modifier
            .clip(
                RoundedCornerShape(999.dp),
            ).background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 7.dp, vertical = 1.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary,
    )
}

// The end of a card row: the time with the count under it, and the button of the row.
@Composable
internal fun RowEnd(time: String, count: Int, onAction: () -> Unit, action: FeedAction = FeedAction.MarkRead) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
            if (count > 1) CountPill(count)
        }
        FeedActionButton(action, onAction)
    }
}
