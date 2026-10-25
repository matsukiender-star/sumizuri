package com.sumizuri.app.feature.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumizuri.app.data.library.ReleasePlan
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.data.text.CivilDate
import com.sumizuri.app.data.text.localOffsetMillis
import com.sumizuri.app.data.text.monthYearText
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.data.text.weekdayName
import com.sumizuri.app.data.text.weekdayOf
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.calendar_came_out
import com.sumizuri.app.shared.resources.calendar_expected
import com.sumizuri.app.shared.resources.calendar_nothing
import com.sumizuri.app.ui.kit.cover.CoverArt
import org.jetbrains.compose.resources.stringResource

private const val DayMs = 86_400_000L

// How many covers fit in a day of the calendar before the rest are counted.
private const val CoversInDay = 2

// What happens on one day for one title: a chapter came out, or one is expected.
private class DayItem(val title: Title, val expected: Boolean)

// A month of releases: the days chapters of the library came out and the days the next ones are expected, by the rhythm of each title. A tap on a day lists its titles under the month.
@Composable
internal fun ReleaseCalendar(plans: List<ReleasePlan>, onOpenTitle: (Long) -> Unit) {
    val now = remember { nowMillis() }
    val offset = remember(now) { localOffsetMillis(now) }
    val today = (now + offset).floorDiv(DayMs)
    var away by remember { mutableIntStateOf(0) }
    val first = remember(today, away) { CivilDate.ofEpochDay(today).monthsAway(away) }
    val start = first.epochDay
    val end = start + first.daysInMonth
    val days = remember(plans, start) { daysOf(plans, start, end, today, offset) }
    var picked by remember(start) { mutableStateOf(if (away == 0) today else null) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton({ away-- }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null) }
            Text(
                monthYearText(start * DayMs + DayMs / 2),
                Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton({ away++ }) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) }
        }
        Row(Modifier.fillMaxWidth()) {
            for (weekday in 1..7) {
                Text(
                    weekdayName(weekday).take(2),
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        val lead = weekdayOf(start) - 1
        val cells = (List(lead) { null } + (start until end).toList()).chunked(7)
        for (week in cells) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (slot in 0 until 7) {
                    val day = week.getOrNull(slot)
                    Box(Modifier.weight(1f)) {
                        if (day !=
                            null
                        ) {
                            DayCell(day, start, days[day].orEmpty(), day == today, day == picked) { picked = day }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        picked?.let { day -> DayList(days[day].orEmpty(), onOpenTitle) }
    }
}

// The titles of each day of the month, those that came out first.
private fun daysOf(
    plans: List<ReleasePlan>,
    start: Long,
    end: Long,
    today: Long,
    offset: Long,
): Map<Long, List<DayItem>> {
    val found = HashMap<Long, MutableList<DayItem>>()
    fun dayOf(at: Long) = (at + offset).floorDiv(DayMs)
    for (plan in plans) {
        for (day in plan.past.map(::dayOf).distinct()) {
            if (day in start until end) found.getOrPut(day) { mutableListOf() } += DayItem(plan.title, false)
        }
        val last = plan.last ?: continue
        val gap = plan.gap ?: continue
        var next = last + gap
        while (dayOf(next) < end) {
            val day = dayOf(next)
            if (day >= maxOf(start, today) && found[day]?.none { it.title.id == plan.title.id } != false) {
                found.getOrPut(day) { mutableListOf() } += DayItem(plan.title, true)
            }
            next += gap
        }
    }
    return found.mapValues { (_, items) -> items.sortedBy { it.expected } }
}

@Composable
private fun DayCell(day: Long, start: Long, items: List<DayItem>, today: Boolean, picked: Boolean, onPick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.62f)
            .clip(MaterialTheme.shapes.small)
            .background(if (picked) colors.primaryContainer else colors.surfaceContainerLow)
            .then(if (today) Modifier.border(1.5.dp, colors.primary, MaterialTheme.shapes.small) else Modifier)
            .clickable(onClick = onPick)
            .padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            (day - start + 1).toString(),
            fontSize = 11.sp,
            fontWeight = if (today) FontWeight.Bold else FontWeight.Normal,
        )
        for (item in items.take(CoversInDay)) {
            CoverArt(item.title, Modifier.fillMaxWidth().alpha(if (item.expected) 0.45f else 1f), letterSize = 9)
        }
        if (items.size > CoversInDay) Text("+${items.size - CoversInDay}", fontSize = 10.sp, color = colors.outline)
    }
}

// The titles of the day picked, each saying whether its chapter came out or is expected.
@Composable
private fun DayList(items: List<DayItem>, onOpenTitle: (Long) -> Unit) {
    if (items.isEmpty()) {
        Text(
            stringResource(Res.string.calendar_nothing),
            color = MaterialTheme.colorScheme.outline,
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }
    for (item in items) {
        Row(
            Modifier.fillMaxWidth().clickable { onOpenTitle(item.title.id) }.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoverArt(item.title, Modifier.width(32.dp), letterSize = 12)
            Text(item.title.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(if (item.expected) Res.string.calendar_expected else Res.string.calendar_came_out),
                style = MaterialTheme.typography.labelSmall,
                color = if (item.expected) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
            )
        }
    }
}
