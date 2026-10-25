package com.sumizuri.app.feature.history

import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.feed.FeedPrefs
import com.sumizuri.app.data.model.feed.UpdateItem
import com.sumizuri.app.data.model.library.LibraryPrefs

// The history page: what was read, folded by day and narrowed by kind and search, with how the page is set.
data class HistoryState(
    val loaded: Boolean = false,
    val items: List<UpdateItem> = emptyList(),
    val kind: Kind? = null,
    val query: String = "",
    val prefs: FeedPrefs = FeedPrefs(),
    val library: LibraryPrefs = LibraryPrefs(),
    // The time the page was worked out at, for the headings.
    val now: Long = 0,
    // The kinds of what was read, before the kind is narrowed, so the kinds are only offered when there is a choice.
    val kinds: Set<Kind> = emptySet(),
)

// What the history page can ask for.
class HistoryCalls(
    val onOpenTitle: (Long) -> Unit,
    val onKind: (Kind?) -> Unit,
    val onQuery: (String) -> Unit,
    val onPrefs: ((FeedPrefs) -> FeedPrefs) -> Unit,
    val onCoverSize: (Int) -> Unit,
    val onRemove: (List<UpdateItem>) -> Unit,
    val onUndo: () -> Unit,
)
