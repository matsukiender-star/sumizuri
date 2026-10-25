package com.sumizuri.app.feature.title.chapter

import com.sumizuri.app.data.model.ChapterSort
import com.sumizuri.app.data.model.OrderKey
import com.sumizuri.app.data.model.readingOrder
import com.sumizuri.app.data.model.seasonOrder
import com.sumizuri.app.feature.title.ChapterItem

// A chapter read this lately still counts as where the reader is.
private const val RecentMs = 7L * 24 * 60 * 60 * 1000

// The season a chapter is in, for naming it next to its number. Only titles with seasons have one.
data class SeasonTag(val key: String?, val name: String?, val isFirst: Boolean)

// What the continue button opens, which is always the chapter after the furthest read. The other chapter, when there is one, is the chapter that was begun or read lately, to pick up again from.
data class ContinueTarget(
    val id: Long,
    val number: Double,
    val otherId: Long? = null,
    val otherNumber: Double? = null,
    val season: SeasonTag? = null,
    val otherSeason: SeasonTag? = null,
    // The names the list shows the two chapters by, so the button says the same, such as Chapter 130 S2 where the number is 215.
    val name: String? = null,
    val otherName: String? = null,
)

fun continueTargetOf(chapters: List<ChapterItem>, now: Long, sort: ChapterSort = ChapterSort.Number): ContinueTarget? {
    if (chapters.isEmpty()) return null
    val ordered = watchOrder(chapters, sort)
    val tagOf = seasonTags(chapters)
    val lastRead = ordered.indexOfLast { it.read }
    // Copies of the furthest read chapter that come after it are passed over, so the next one is a new number.
    val furthest = if (lastRead <
        0
    ) {
        ordered.first()
    } else {
        ordered.drop(lastRead + 1).firstOrNull { !sameSpot(it, ordered[lastRead]) }
            ?: ordered.last()
    }
    val recent = begun(ordered) ?: afterLatestRead(ordered, now)
    if (recent == null ||
        recent.id == furthest.id
    ) {
        return ContinueTarget(furthest.id, furthest.number, season = tagOf(furthest), name = furthest.name)
    }
    return ContinueTarget(
        furthest.id,
        furthest.number,
        otherId = recent.id,
        otherNumber = recent.number,
        season = tagOf(furthest),
        otherSeason = tagOf(recent),
        name = furthest.name,
        otherName = recent.name,
    )
}

// The chapters in the order they are read, season by season when the title has seasons, and in the chosen order within each.
internal fun watchOrder(chapters: List<ChapterItem>, sort: ChapterSort = ChapterSort.Number): List<ChapterItem> =
    readingOrder(chapters, sort) {
        OrderKey(it.id, it.number, it.sourceOrder, it.at, it.season, it.name)
    }

// How to name the season of any chapter of these, or always nothing when the title has no seasons.
internal fun seasonTags(chapters: List<ChapterItem>): (ChapterItem) -> SeasonTag? {
    if (chapters.none { it.season != null }) return { null }
    val order = seasonOrder(chapters.map { it.season })
    val names = chapters.filter { it.seasonName != null }.associate { it.season to it.seasonName }
    return { chapter -> SeasonTag(chapter.season, names[chapter.season], chapter.season == null && order.noneIsFirst) }
}

private fun sameSpot(a: ChapterItem, b: ChapterItem) = a.season == b.season && a.number == b.number

// The chapter that was begun and not finished, the highest one when there are several.
private fun begun(ordered: List<ChapterItem>): ChapterItem? = ordered.lastOrNull { !it.read && it.progress != null }

// The chapter after the one read last, when that was within the week.
private fun afterLatestRead(ordered: List<ChapterItem>, now: Long): ChapterItem? {
    // Chapters marked together share a time, and the furthest of them counts as the one read last.
    val marked = ordered.filter { it.read && it.readAt != null }
    val newest = marked.maxOfOrNull { it.readAt ?: 0 } ?: return null
    val latest = marked.last { it.readAt == newest }
    if (now - (latest.readAt ?: 0) > RecentMs) return null
    val index = ordered.indexOf(latest)
    return ordered.getOrNull(index + 1) ?: latest
}
