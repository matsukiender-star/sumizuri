package com.sumizuri.app.feature.title.chapter

// A pattern needs this many dates, and the top weekday needs this share of them.
private const val FewestDates = 4
private const val ClearShare = 0.4
private const val DayMs = 24L * 60 * 60 * 1000

// The weekday of a time, with Monday as 0. The day is counted in UTC, and the first of January 1970 was a Thursday.
internal fun weekdayOf(time: Long): Int = ((time.floorDiv(DayMs) + 3).mod(7L)).toInt()

// The weekday a title most likely comes out on, with Monday as 0, guessed from the dates of its chapters. It is null when there is no clear pattern.
fun guessReleaseDay(times: List<Long>): Int? {
    val dates = times.filter { it > 0 }
    if (dates.size < FewestDates) return null
    val top = dates.groupingBy(::weekdayOf).eachCount().maxByOrNull { it.value } ?: return null
    return if (top.value < dates.size * ClearShare) null else top.key
}
