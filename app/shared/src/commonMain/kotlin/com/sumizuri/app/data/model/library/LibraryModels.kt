package com.sumizuri.app.data.model.library

import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.Status
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.data.model.library.shelf.ShelfCondition
import com.sumizuri.app.data.model.library.shelf.ShelfContext

// A shelf of the library with how many titles it holds.
data class Category(
    val id: Long,
    val name: String,
    val excludeFromUpdate: Boolean,
    val titles: Int,
    // What the shelf is ordered by and which way, or null to use the order of the library, and the one status it shows, or null for any.
    val sort: LibrarySortField? = null,
    val ascending: Boolean = true,
    val status: Status? = null,
    // The field that settles ties of the order of the shelf, and its direction.
    val then: LibrarySortField? = null,
    val thenAscending: Boolean = true,
    // The kind of title the shelf is for, or null for a shelf that is for every kind.
    val kind: Kind? = null,
    // What puts titles on the shelf by itself: the conditions, and whether a title must meet all of them or any one. A shelf with none holds only the titles put on it.
    val conditions: List<ShelfCondition> = emptyList(),
    val matchAll: Boolean = true,
) {
    // The conditions that have a value, which are the ones that count.
    private val active: List<ShelfCondition> get() = conditions.filter { it.complete }

    val isSmart: Boolean get() = active.isNotEmpty()

    // Whether a title meets the conditions of the shelf.
    fun matches(title: Title, context: ShelfContext): Boolean {
        val list = active
        if (list.isEmpty()) return false
        return if (matchAll) list.all { it.matches(title, context) } else list.any { it.matches(title, context) }
    }

    // Whether the title is on the shelf, because it was put there or because it meets the conditions.
    fun holds(entry: LibraryEntry, context: ShelfContext): Boolean =
        id in entry.categories || matches(entry.title, context)

    // The order of the shelf when it has one of its own.
    val rule: SortRule? get() = sort?.let { SortRule(it, ascending, then, thenAscending) }
}

// A title of the library with the shelves it is on, and when it was last read and when its newest chapter came, as epoch milliseconds, which are 0 unless an order reads them.
data class LibraryEntry(val title: Title, val categories: Set<Long>, val lastRead: Long = 0, val latest: Long = 0)

// The name and the language of a source, for the marks on the covers.
data class SourceBrief(val name: String, val lang: String)
