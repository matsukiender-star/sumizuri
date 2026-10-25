package com.sumizuri.app.feature.library.part

import com.sumizuri.app.data.model.FeedItem
import com.sumizuri.app.data.model.library.LibrarySection
import com.sumizuri.app.feature.library.LibraryState

// The titles a part of the top of the library shows, or null when the part is left out: it is off, it has no titles, or titles are being picked. The titles are those of the open kind and shelf. Updates and History show even with no titles, since they took the place of a tab and are the way to its page.
internal fun partItems(state: LibraryState, section: LibrarySection): List<FeedItem>? {
    val prefs = state.prefs
    if (state.selecting || prefs.countOf(section) <= 0) return null
    val items = when (section) {
        LibrarySection.Continue -> state.recent
        LibrarySection.Updates -> state.updates
        LibrarySection.History -> state.history
        LibrarySection.Library -> return null
    }
    return items.takeIf { it.isNotEmpty() || section != LibrarySection.Continue }
}
