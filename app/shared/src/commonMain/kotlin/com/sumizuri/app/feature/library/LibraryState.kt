package com.sumizuri.app.feature.library

import com.sumizuri.app.data.model.FeedItem
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.data.model.library.Category
import com.sumizuri.app.data.model.library.LibraryPrefs
import com.sumizuri.app.data.model.library.SortRule
import com.sumizuri.app.data.model.library.SourceBrief
import com.sumizuri.app.data.sync.UpdateFailure
import com.sumizuri.app.data.sync.UpdateProgress

// The tab for the titles that are on no shelf.
const val UncategorizedShelf = -1L

data class LibraryState(
    val loaded: Boolean = false,
    val query: String = "",
    val kind: Kind? = null,
    // The titles as they are shown, narrowed by the kind, the shelf, the quick filters and the search, and put in order.
    val titles: List<Title> = emptyList(),
    // How many titles the library holds before anything narrows it down.
    val total: Int = 0,
    // How many titles have a source that is gone or switched off, and whether only they are shown.
    val unreachable: Int = 0,
    val onlyUnreachable: Boolean = false,
    val prefs: LibraryPrefs = LibraryPrefs(),
    val categories: List<Category> = emptyList(),
    // The order that applies to what is shown, from the open shelf, the tab, the kind or the default.
    val rule: SortRule = SortRule(),
    // The shelves the picked titles can be put on, and the one kind they are of when they are all of one.
    val pickShelves: List<Category> = emptyList(),
    val pickKind: Kind? = null,
    // True when some title is on no shelf, so a tab for those is shown.
    val hasUncategorized: Boolean = false,
    // The kinds the library holds, so the kinds are only offered when there is a choice.
    val kinds: Set<Kind> = emptySet(),
    // The titles picked for a bulk action, which makes the page a selection page while there are any.
    val selected: Set<Long> = emptySet(),
    // For each shelf, how many of the picked titles are on it.
    val selectedShelves: Map<Long, Int> = emptyMap(),
    // How far the check of the library for new chapters has got, or null when none runs.
    val progress: UpdateProgress? = null,
    // The titles that could not be asked at the last check, and why.
    val failures: List<UpdateFailure> = emptyList(),
    // How many titles the last check left unchecked because the connection was lost and did not come back.
    val unchecked: Int = 0,
    // The rows above the covers: the titles read last, the newest chapters and the history, each as long as the person chose.
    val recent: List<FeedItem> = emptyList(),
    val updates: List<FeedItem> = emptyList(),
    val history: List<FeedItem> = emptyList(),
    // The name and the language of each source, for the marks on the covers.
    val sources: Map<String, SourceBrief> = emptyMap(),
) {
    val selecting: Boolean get() = selected.isNotEmpty()

    // The shelf that is open, or null when all the titles or the titles on no shelf are shown.
    val shelf: Category? get() = categories.firstOrNull { it.id == prefs.category }
}
