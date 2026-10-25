package com.sumizuri.app.data.library

import com.sumizuri.app.data.model.Title

// How many of the newest chapters of a title the schedule is worked out from.
const val SmartSample = 12

private const val Day = 24L * 60 * 60 * 1000

// A title needs this many different release days before its rhythm is trusted.
private const val FewestDays = 3

// The shortest and the longest gap the rhythm of a title can have.
private const val ShortestGap = Day
private const val LongestGap = 90 * Day

// A title is asked a little before the day it is due, since sources are not exact about when a chapter came out.
private const val Early = Day

// A title with nothing new for this long is taken as resting, and is asked once a week.
private const val Resting = 180 * Day

// The day of the last chapter of a title and the usual gap between its chapters, which is the middle of the gaps between the days its newest chapters came out.
private class Rhythm(val last: Long, val gap: Long)

// The rhythm of a title, or null when it has too few dated chapters to have one.
private fun rhythmOf(times: List<Long>): Rhythm? {
    val days = times.map { it / Day }.distinct().sortedDescending()
    if (days.size < FewestDays) return null
    val gaps = days.zipWithNext { newer, older -> (newer - older) * Day }.sorted()
    return Rhythm(days.first() * Day, gaps[gaps.size / 2].coerceIn(ShortestGap, LongestGap))
}

// Works out which titles are not due for a check yet. The next chapter of a title is expected one gap after the last one. A title with too few dated chapters to have a rhythm is always asked, and so is a title whose next chapter is already late. A resting title is asked on one day of the week, which depends on the title, so the resting ones are not all asked together.
fun titlesNotDue(releases: Map<Long, List<Long>>, now: Long): Set<Long> = buildSet {
    for ((id, times) in releases) {
        val rhythm = rhythmOf(times) ?: continue
        if (now - rhythm.last > Resting) {
            if ((now / Day) % 7 != id % 7) add(id)
            continue
        }
        if (rhythm.last + rhythm.gap - Early > now) add(id)
    }
}

// When the next chapter of each title is expected, as epoch milliseconds. Titles with no rhythm and resting titles have no expected day.
fun nextReleases(releases: Map<Long, List<Long>>, now: Long): Map<Long, Long> = buildMap {
    for ((id, times) in releases) {
        val rhythm = rhythmOf(times) ?: continue
        if (now - rhythm.last > Resting) continue
        put(id, rhythm.last + rhythm.gap)
    }
}

// A title of the library and the day its next chapter is expected.
data class UpcomingTitle(val title: Title, val at: Long)

// What the calendar of releases shows for a title: the days its newest chapters came out, and when its next ones are expected, which is the day of its last chapter and the usual gap after it. A title with no rhythm, or one resting, has no expected days.
class ReleasePlan(val title: Title, val past: List<Long>, val last: Long?, val gap: Long?)

fun releasePlans(titles: List<Title>, releases: Map<Long, List<Long>>, now: Long): List<ReleasePlan> =
    titles.mapNotNull { title ->
        val times = releases[title.id].orEmpty()
        if (times.isEmpty()) return@mapNotNull null
        val rhythm = rhythmOf(times)?.takeIf { now - it.last <= Resting }
        ReleasePlan(title, times, rhythm?.last, rhythm?.gap)
    }
