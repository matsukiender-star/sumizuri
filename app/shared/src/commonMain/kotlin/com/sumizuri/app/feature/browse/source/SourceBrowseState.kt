package com.sumizuri.app.feature.browse.source

import com.sumizuri.app.data.model.SavedCatalog
import com.sumizuri.app.feature.browse.repo.RemovePrompt
import com.sumizuri.app.source.model.FilterGroup
import com.sumizuri.app.source.model.SourceCatalog
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.model.SourcePreference
import com.sumizuri.app.source.repo.InstalledSource
import com.sumizuri.app.ui.kit.input.PickedFile

data class SourceBrowseState(
    val source: InstalledSource,
    // The folder a source of files on the device reads, which files can be added to, or null for any other source.
    val localFolder: String? = null,
    // The text in the search bar, and the search the Filtered tab shows, which waits for a pause in typing.
    val query: String = "",
    val activeQuery: String? = null,
    val tab: String = TabPopular,
    // The named lists the source offers, and the searches the reader kept as tabs.
    val catalogs: List<SourceCatalog> = emptyList(),
    val savedTabs: List<SavedCatalog> = emptyList(),
    // What the filters are set to, the filters the source offers once they are read, and whether it has any.
    val filters: Map<String, Any?> = emptyMap(),
    val filterGroups: List<FilterGroup> = emptyList(),
    val hasFilters: Boolean = false,
    // The settings the source lets the reader change, once they are read, with their values.
    val hasPreferences: Boolean = false,
    val preferences: List<SourcePreference> = emptyList(),
    val preferenceValues: Map<String, Any?> = emptyMap(),
    val entries: List<SourceEntry> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    // The address of the site when a bot check stopped the list, so a person can be sent there to pass it.
    val challengeUrl: String? = null,
    val hasMore: Boolean = false,
    // True when the source gave its last page again, which ended the list.
    val repeated: Boolean = false,
    // The addresses of the entries of this source that are already in the library.
    val inLibrary: Set<String> = emptySet(),
    // The entries being added to the library from a long press on their cover.
    val adding: Set<String> = emptySet(),
    // Asks before removing a source that titles in the library came from, and turns true once it is removed so the page closes.
    val removing: RemovePrompt? = null,
    val removed: Boolean = false,
) {
    // Whether any filter is set to something, which is what the badge on the filter button says.
    val filtersActive: Boolean
        get() = filters.values.any { value ->
            when (value) {
                null -> false
                is Boolean -> value
                is Collection<*> -> value.isNotEmpty()
                is String -> value.isNotEmpty()
                else -> true
            }
        }
}

// Everything the source page can ask for, so the page is given one thing and not a dozen.
class SourceBrowseActions(
    val onBack: () -> Unit,
    val onQuery: (String) -> Unit,
    val onTab: (String) -> Unit,
    val onLoadMore: () -> Unit,
    val onRetry: () -> Unit,
    val onSolve: () -> Unit,
    val onOpenEntry: (SourceEntry) -> Unit,
    val onOpenFilters: () -> Unit,
    val onFilters: (Map<String, Any?>) -> Unit,
    val onSaveTab: (String) -> Unit,
    val onRenameTab: (String, String) -> Unit,
    val onDeleteTab: (String) -> Unit,
    val onOpenPreferences: () -> Unit,
    val onPreference: (String, Any?) -> Unit,
    val onClosePreferences: () -> Unit,
    // Opens the code of the source in the editor.
    val onEditCode: () -> Unit,
    // Adds an entry to the library from a long press, without opening it.
    val onQuickAdd: (SourceEntry) -> Unit,
    // Copies picked files into the folder of a source of files on the device.
    val onImport: (List<PickedFile>) -> Unit,
    // Lays the entries out another way, for every source.
    val onStyle: (BrowseStyle) -> Unit,
    // Removes the source, asking first when titles in the library came from it.
    val onRemove: () -> Unit,
    val onConfirmRemove: () -> Unit,
    val onCancelRemove: () -> Unit,
)

// The tabs of a source page are told apart by a key. A catalog of the source and a tab the reader saved carry their own name after a mark.
const val TabPopular = "popular"
const val TabLatest = "latest"
const val TabSearch = "search"
const val CatalogMark = "cat:"
const val SavedMark = "my:"
