package com.sumizuri.app.data.model.feed

import com.sumizuri.app.data.model.stats.dayOf
import com.sumizuri.app.data.text.monthIndexOf

enum class HeadingKind { Today, Yesterday, Date, ThisWeek, LastWeek, Month }

// The heading a feed puts over a stretch of time. The stamp tells one date or month from another, and is the day or the month number where the kind needs one.
data class FeedHeading(val kind: HeadingKind, val stamp: Long = 0)

private fun daysAgo(now: Long, at: Long): Long = (dayOf(now) - dayOf(at)).coerceAtLeast(0)

// The heading of a list: today and yesterday on their own, then the rest of the week, last week, and the month. A title that came out every few days is then one row in each stretch, and the days with one row each no longer each take a heading.
fun listHeading(now: Long, at: Long): FeedHeading = when (daysAgo(now, at)) {
    0L -> FeedHeading(HeadingKind.Today)
    1L -> FeedHeading(HeadingKind.Yesterday)
    else -> coarseHeading(now, at)
}

// The heading of a grid: this week, last week, or the month.
fun coarseHeading(now: Long, at: Long): FeedHeading {
    val ago = daysAgo(now, at)
    return when {
        ago < 7 -> FeedHeading(HeadingKind.ThisWeek)
        ago < 14 -> FeedHeading(HeadingKind.LastWeek)
        else -> FeedHeading(HeadingKind.Month, monthIndexOf(at).toLong())
    }
}
