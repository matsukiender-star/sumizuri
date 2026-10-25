package com.sumizuri.app.data.blocks

import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.Status
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.data.model.stats.ReadingStats.Companion.DayMs
import com.sumizuri.app.data.model.stats.TimeOfDay
import com.sumizuri.app.data.model.stats.timeOf
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.weekday_0
import com.sumizuri.app.shared.resources.weekday_1
import com.sumizuri.app.shared.resources.weekday_2
import com.sumizuri.app.shared.resources.weekday_3
import com.sumizuri.app.shared.resources.weekday_4
import com.sumizuri.app.shared.resources.weekday_5
import com.sumizuri.app.shared.resources.weekday_6

private val Weekdays =
    listOf(
        Res.string.weekday_0,
        Res.string.weekday_1,
        Res.string.weekday_2,
        Res.string.weekday_3,
        Res.string.weekday_4,
        Res.string.weekday_5,
        Res.string.weekday_6,
    )

// The year, month and day of a day counted from 1970, by the calendar everyone uses.
internal fun civilDate(day: Long): Triple<Int, Int, Int> {
    val z = day + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
    return Triple((yoe + era * 400 + if (m <= 2) 1 else 0).toInt(), m, d)
}

// Monday is 0. The first of January 1970 was a Thursday.
internal fun weekdayIndex(day: Long): Int = (day + 3).mod(7)

// What a title is counted under when titles are grouped, which for genres is each of its genres. A title with no genre or no author is left out of those, as a bar of the unknown would only crowd out the rest.
private fun keysOf(title: Title, by: String): List<String> = when (by) {
    "genre" -> title.genres
    "kind" -> listOf(title.kind.name)
    "status" -> listOf(title.status.name)
    "source" -> listOf(title.sourceId)
    "author" -> listOfNotNull(title.author.takeIf { it.isNotBlank() })
    else -> listOf(title.name)
}

// A label that names a kind or a status of the app carries the name in the strings, so it shows in the language of the app.
private fun pointOf(key: String, count: Int, by: String): Point = when (by) {
    "kind" -> Point(key, count.toDouble(), Kind.entries.firstOrNull { it.name == key }?.label)
    "status" -> Point(key, count.toDouble(), Status.entries.firstOrNull { it.name == key }?.label)
    else -> Point(key, count.toDouble())
}

// How many titles fall under each genre, kind, status, source or author, the most first.
internal fun groupTitles(titles: List<Title>, by: String): List<Point> {
    val counts = LinkedHashMap<String, Int>()
    for (title in titles) for (key in keysOf(title, by)) counts[key] = (counts[key] ?: 0) + 1
    return counts.entries.sortedByDescending { it.value }.map { pointOf(it.key, it.value, by) }
}

// How many chapters were read on each day, weekday, time of day, hour or month, or for each title, genre, kind or source. Days, hours and months are in order with the empty ones in between, so a line over them keeps its shape. The others put the most first.
internal fun groupReads(context: BlockContext, reads: List<ReadEvent>, by: String): List<Point> = when (by) {
    "day" -> {
        val counts = reads.groupingBy { context.dayOf(it.at) }.eachCount()
        if (counts.isEmpty()) {
            emptyList()
        } else {
            (counts.keys.min()..context.today.coerceAtLeast(counts.keys.max())).map { day ->
                val (_, month, date) = civilDate(day)
                Point("$month/$date", (counts[day] ?: 0).toDouble())
            }
        }
    }

    "weekday" -> {
        val counts = reads.groupingBy { weekdayIndex(context.dayOf(it.at)) }.eachCount()
        (0..6).map { Point("${it + 1}", (counts[it] ?: 0).toDouble(), Weekdays[it]) }
    }

    "time" -> {
        val counts = reads.groupingBy { timeOf((context.local(it.at).mod(DayMs) / HourMs).toInt()) }.eachCount()
        TimeOfDay.entries.map { Point(it.name, (counts[it] ?: 0).toDouble(), it.label) }
    }

    "hour" -> {
        val counts = reads.groupingBy { (context.local(it.at).mod(DayMs) / HourMs).toInt() }.eachCount()
        (0..23).map { Point("$it", (counts[it] ?: 0).toDouble()) }
    }

    "month" -> {
        val counts = reads
            .groupingBy {
                civilDate(context.dayOf(it.at)).let { (year, month, _) -> year * 12 + month - 1 }
            }.eachCount()
        if (counts.isEmpty()) {
            emptyList()
        } else {
            (counts.keys.min()..counts.keys.max()).map {
                Point(
                    "${it / 12}/${it % 12 + 1}",
                    (
                        counts[it]
                            ?: 0
                        ).toDouble(),
                )
            }
        }
    }

    else -> {
        val counts = LinkedHashMap<String, Int>()
        for (read in reads) {
            val title = context.titleById[read.titleId] ?: continue
            for (key in keysOf(title, by)) counts[key] = (counts[key] ?: 0) + 1
        }
        counts.entries.sortedByDescending { it.value }.map { pointOf(it.key, it.value, by) }
    }
}
