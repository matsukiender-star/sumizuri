package com.sumizuri.app.feature.updates.row

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.feed.FeedHeading
import com.sumizuri.app.data.model.feed.HeadingKind
import com.sumizuri.app.data.model.feed.UpdateItem
import com.sumizuri.app.data.model.feed.coarseHeading
import com.sumizuri.app.data.model.feed.listHeading
import com.sumizuri.app.data.text.dateText
import com.sumizuri.app.data.text.monthYearText
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.day_last_week
import com.sumizuri.app.shared.resources.day_this_week
import com.sumizuri.app.shared.resources.day_today
import com.sumizuri.app.shared.resources.day_yesterday
import com.sumizuri.app.shared.resources.feed_mark_day_read
import org.jetbrains.compose.resources.stringResource

// The heading over the titles of one day, or of one week or month in a grid: the name in small capitals, how many titles are under it, a thin line, and when asked a button that marks all of them read. The time is one that belongs to the group, for the headings that are a date or a month.
@Composable
internal fun FeedDayHeading(
    heading: FeedHeading,
    at: Long,
    count: Int,
    onMarkAllRead: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val text = when (heading.kind) {
        HeadingKind.Today -> stringResource(Res.string.day_today)
        HeadingKind.Yesterday -> stringResource(Res.string.day_yesterday)
        HeadingKind.Date -> dateText(at)
        HeadingKind.ThisWeek -> stringResource(Res.string.day_this_week)
        HeadingKind.LastWeek -> stringResource(Res.string.day_last_week)
        HeadingKind.Month -> monthYearText(at)
    }
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = colors.outline,
        )
        Text(
            count.toString(),
            Modifier
                .clip(
                    RoundedCornerShape(10.dp),
                ).background(colors.surfaceContainerHighest)
                .padding(horizontal = 7.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant,
        )
        HorizontalDivider(Modifier.weight(1f).padding(start = 4.dp), color = colors.outlineVariant)
        if (onMarkAllRead !=
            null
        ) {
            TextButton(onMarkAllRead) {
                Text(stringResource(Res.string.feed_mark_day_read), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// The items in the order they came, under the heading of today, yesterday, the week or the month, or only the week or the month in a grid.
internal fun groupByHeading(
    items: List<UpdateItem>,
    grid: Boolean,
    now: Long,
): List<Pair<FeedHeading, List<UpdateItem>>> {
    val groups = mutableListOf<Pair<FeedHeading, MutableList<UpdateItem>>>()
    for (item in items) {
        val heading = if (grid) coarseHeading(now, item.at) else listHeading(now, item.at)
        if (groups.isEmpty() || groups.last().first != heading) groups += heading to mutableListOf()
        groups.last().second += item
    }
    return groups
}
