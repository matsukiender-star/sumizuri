package com.sumizuri.app.feature.library

import com.sumizuri.app.data.model.FeedItem
import com.sumizuri.app.data.model.Status
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.data.model.library.Category
import com.sumizuri.app.data.model.library.LibraryEntry
import com.sumizuri.app.data.model.library.QuickFilter
import com.sumizuri.app.data.model.library.SearchTarget
import com.sumizuri.app.data.model.library.shelf.ShelfContext

// The small rules the library page sorts its titles by: the newest of a feed, whether a title is on a shelf or passes a quick filter, and what search looks at.

// The newest titles of a feed, once each, that are in the library on the open kind and shelf.
internal fun libraryRowOf(feed: List<FeedItem>, here: Set<Long>, count: Int): List<FeedItem> =
    if (count <= 0) emptyList() else feed.filter { it.title.id in here }.distinctBy { it.title.id }.take(count)

internal fun isOnShelf(shelf: Long?, entry: LibraryEntry, shelves: List<Category>, context: ShelfContext): Boolean =
    when (shelf) {
        null -> true
        UncategorizedShelf -> shelves.none { it.holds(entry, context) }
        else -> shelves.firstOrNull { it.id == shelf }?.holds(entry, context) == true
    }

internal fun passesFilter(filter: QuickFilter, title: Title, favorites: Boolean): Boolean = when (filter) {
    QuickFilter.Unread -> title.unread > 0
    QuickFilter.CaughtUp -> title.unread == 0
    QuickFilter.Completed -> title.status == Status.Completed
    QuickFilter.Favorite -> !favorites || title.favorite
}

internal fun searchTargetOf(entry: LibraryEntry, shelfNames: Map<Long, String>, sourceNames: Map<String, String>) =
    SearchTarget(
        name = entry.title.name.lowercase(),
        author = entry.title.author.lowercase(),
        genres = entry.title.genres.map { it.lowercase() },
        status = entry.title.status.name
            .lowercase(),
        kind = entry.title.kind.name
            .lowercase(),
        source = sourceNames[entry.title.sourceId].orEmpty(),
        shelves = entry.categories.mapNotNull { shelfNames[it] },
        unread = entry.title.unread,
    )
