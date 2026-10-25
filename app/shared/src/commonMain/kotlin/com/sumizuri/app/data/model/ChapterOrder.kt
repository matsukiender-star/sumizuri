package com.sumizuri.app.data.model

// How the chapters of every title are put in order. The order of the source follows the site, which stays right when a site numbers its chapters in two ways, such as Chapter 216 next to Chapter 130 S2. The number and the date are there for a site whose own order is wrong.
enum class ChapterSort(val key: String) {
    Source("source"),
    Number("number"),
    Date("date"),
    ;

    companion object {
        fun of(key: String?): ChapterSort = entries.firstOrNull { it.key == key } ?: Source
    }
}

// What the order needs to know of a chapter: its number, its place in the list of the source, or -1 when the source does not list it, its date, its season and its name, which can hold a volume, a season, a piece or a range besides the number.
class OrderKey(
    val id: Long,
    val number: Double,
    val sourceOrder: Int,
    val date: Long,
    val season: String?,
    val name: String?,
)

// The chapters in the order they are read, from the first to the latest. A title with seasons goes season by season, each in the chosen order.
fun <T> readingOrder(chapters: List<T>, sort: ChapterSort, key: (T) -> OrderKey): List<T> {
    val keyed = chapters.map { it to key(it) }
    val seasons = keyed.map { it.second.season }
    if (seasons.none { it != null }) return inOrder(keyed, sort).map { it.first }
    val order = seasonOrder(seasons)
    return keyed
        .groupBy { order.rankOf(it.second.season) }
        .toList()
        .sortedBy { it.first }
        .flatMap { inOrder(it.second, sort) }
        .map { it.first }
}

// A chapter with the place its name and number give it among the others.
private class Ranked<T>(val item: T, val key: OrderKey, val rank: ChapterRank)

private fun <T> inOrder(keyed: List<Pair<T, OrderKey>>, sort: ChapterSort): List<Pair<T, OrderKey>> {
    val ranks = chapterRanks(keyed.map { it.second.name to it.second.number })
    val ranked = keyed.mapIndexed { index, (item, key) -> Ranked(item, key, ranks[index]) }
    val byNumber = ranked.sortedWith(compareBy({ it.rank }, { it.key.id }))
    val shown = when (sort) {
        ChapterSort.Number -> byNumber
        ChapterSort.Date -> ranked.sortedWith(compareBy({ it.key.date }, { it.rank }, { it.key.id }))
        ChapterSort.Source -> bySource(ranked, byNumber)
    }
    return shown.map { it.item to it.key }
}

// In the order of the site. Most sites list their newest chapter first, so their list is read from its end, and the places the names and numbers give tell which way a site lists them. A chapter the site does not list, such as one brought from a backup, goes after the last listed chapter whose place is not after its own. With no list known yet, the places decide.
private fun <T> bySource(ranked: List<Ranked<T>>, byNumber: List<Ranked<T>>): List<Ranked<T>> {
    val listed = ranked.filter { it.key.sourceOrder >= 0 }.sortedWith(compareBy({ it.key.sourceOrder }, { it.key.id }))
    if (listed.isEmpty()) return byNumber
    val steps = listed.map { it.rank }.zipWithNext()
    val newestFirst = steps.count { (a, b) -> b < a } >= steps.count { (a, b) -> b > a }
    val read = (if (newestFirst) listed.asReversed() else listed).toMutableList()
    for (chapter in byNumber.filter { it.key.sourceOrder < 0 }) {
        val after = read.indexOfLast { it.rank <= chapter.rank }
        read.add(after + 1, chapter)
    }
    return read
}
