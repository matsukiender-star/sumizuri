package com.sumizuri.app.data.model.feed

import com.sumizuri.app.data.db.chapter.UpdateRow
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.data.model.stats.dayOf

// One title on the updates page with the new chapters it stands for, newest first. When several came the same day they are one item.
data class UpdateItem(
    val title: Title,
    val chapterIds: List<Long>,
    val numbers: List<Double>,
    // The name of the newest chapter, when it has one.
    val name: String?,
    val at: Long,
) {
    val count: Int get() = chapterIds.size
}

// The chapters of one title that came the same day as one item, newest first. A chapter of a title that is not in the library is left out.
fun mergeSameDay(rows: List<UpdateRow>, titles: Map<Long, Title>): List<UpdateItem> {
    val groups = LinkedHashMap<Pair<Long, Long>, MutableList<UpdateRow>>()
    for (row in rows) {
        if (row.titleId in
            titles
        ) {
            groups.getOrPut(row.titleId to dayOf(row.at)) { mutableListOf() } += row
        }
    }
    return groups.values.map { group ->
        val first = group.first()
        UpdateItem(titles.getValue(first.titleId), group.map { it.id }, group.map { it.number }, first.name, first.at)
    }
}

// Each title once for each stretch of time the page groups by, the week or month of a grid by default, with the chapters of all its days counted together.
fun mergeByGroup(
    items: List<UpdateItem>,
    now: Long,
    heading: (now: Long, at: Long) -> FeedHeading = ::coarseHeading,
): List<UpdateItem> {
    val groups = LinkedHashMap<Pair<FeedHeading, Long>, MutableList<UpdateItem>>()
    for (item in items) groups.getOrPut(heading(now, item.at) to item.title.id) { mutableListOf() } += item
    return groups.values.map { group ->
        if (group.size ==
            1
        ) {
            group.single()
        } else {
            group.first().copy(chapterIds = group.flatMap { it.chapterIds }, numbers = group.flatMap { it.numbers })
        }
    }
}
