package com.sumizuri.app.data.model

// The seasons of a title in the order they are shown and watched, and whether the chapters with no season are its first season.
class SeasonOrder(val keys: List<String?>, val noneIsFirst: Boolean) {
    private val rank = keys.withIndex().associate { it.value to it.index }

    fun rankOf(season: String?): Int = rank[season] ?: keys.size
}

// Numbered seasons go by their number and named ones follow. Chapters with no season go last, or first as season one when every other season is numbered above one, since some sources leave the first season unnamed.
fun seasonOrder(seasons: Collection<String?>): SeasonOrder {
    val keyed = seasons.filterNotNull().distinct()
    val numbered = keyed.filter { it.toDoubleOrNull() != null }.sortedBy { it.toDouble() }
    val named = keyed.filter { it.toDoubleOrNull() == null }
    val none = null in seasons
    val noneIsFirst = none && named.isEmpty() && numbered.isNotEmpty() && numbered.all { it.toDouble() > 1 }
    val keys = buildList {
        if (noneIsFirst) add(null)
        addAll(numbered)
        addAll(named)
        if (none && !noneIsFirst) add(null)
    }
    return SeasonOrder(keys, noneIsFirst)
}
