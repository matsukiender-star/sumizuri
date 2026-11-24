package com.sumizuri.app.feature.library

import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.Status
import com.sumizuri.app.data.model.library.LibraryPrefs
import com.sumizuri.app.data.model.library.LibrarySection
import com.sumizuri.app.data.model.library.SortRule
import com.sumizuri.app.feature.library.top.LibraryTab
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.repo.InstalledSource

// Everything the library page can ask for, so the page is given one thing and not twenty.
internal class LibraryCalls(
    val onQuery: (String) -> Unit,
    val onKind: (Kind?) -> Unit,
    val onToggleUnreachable: () -> Unit,
    val onPrefs: ((LibraryPrefs) -> LibraryPrefs) -> Unit,
    val onShelfRule: (id: Long, rule: SortRule?, status: Status?) -> Unit,
    val onSelect: (Long) -> Unit,
    val onClearSelection: () -> Unit,
    val onSelectAll: () -> Unit,
    val onInvert: () -> Unit,
    val onMarkRead: (Boolean) -> Unit,
    val onFavorite: (Boolean) -> Unit,
    val onRemove: () -> Unit,
    val onShelves: (add: Set<Long>, remove: Set<Long>) -> Unit,
    // Puts titles on a shelf they were dragged onto.
    val onDropOnShelf: (titles: Set<Long>, shelf: Long) -> Unit,
    val onCreateShelf: suspend (String) -> Long?,
    val onOpenTitle: (Long) -> Unit,
    val onOpenShelves: () -> Unit,
    val onMove: (Long) -> Unit,
    val onMigrate: (List<Long>) -> Unit,
    val onUpdate: () -> Unit,
    val onCancelUpdate: () -> Unit,
    val onDismissFailures: () -> Unit,
    val onRandom: () -> Unit,
    // Opens the whole page a part of the library stands for.
    val onSeeAll: (LibrarySection) -> Unit,
    // Opens the page where the titles whose source is missing or off are mended.
    val onOpenMissing: () -> Unit,
    // Opens the page of the chapters saved to the device and on the way.
    val onOpenDownloads: () -> Unit,
    // Opens the search of every source for the words typed in the search of the library.
    val onSearchSources: (String) -> Unit,
    // Opens the chapter a title goes on from, as its continue button would.
    val onContinue: (Long) -> Unit,
    val onSelectShelf: (Long?) -> Unit,
    val onOpenLive: () -> Unit,
    // Plays a favorite channel from the live tab of the library.
    val onPlayLive: (playlistId: String, channelId: String) -> Unit = { _, _ -> },
    // Opens a recommended title, and the source of a row of recommendations with its genre chosen.
    val onOpenPick: (InstalledSource, SourceEntry) -> Unit = { _, _ -> },
    val onOpenPickGenre: (InstalledSource, Map<String, Any?>) -> Unit = { _, _ -> },
    // Opens syncing with the code ready to scan, from the button at the top of the library.
    val onOpenSync: () -> Unit = {},
) {
    // Opens one shelf, or all the titles for null.
    fun onShelf(id: Long?) = onSelectShelf(id)

    fun onTab(tab: LibraryTab) {
        when (tab) {
            LibraryTab.All -> onSelectShelf(null)
            is LibraryTab.ByKind -> onKind(tab.kind)
            LibraryTab.Uncategorized -> onSelectShelf(UncategorizedShelf)
            is LibraryTab.Shelf -> onSelectShelf(tab.id)
            LibraryTab.Live -> onOpenLive()
        }
    }
}

// What can be done to the picked titles of the library.
internal class LibraryPickCalls(
    val onClose: () -> Unit,
    val onSelectAll: () -> Unit,
    val onInvert: () -> Unit,
    val onMarkRead: (Boolean) -> Unit,
    val onFavorite: (Boolean) -> Unit,
    val onShelves: () -> Unit,
    val onMove: () -> Unit,
    val onMigrate: () -> Unit,
    val onRemove: () -> Unit,
)
