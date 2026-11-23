package com.sumizuri.app.feature.library.top

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.library.Category
import com.sumizuri.app.data.model.library.LibraryPrefs
import com.sumizuri.app.feature.library.LibraryState
import com.sumizuri.app.feature.library.UncategorizedShelf
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.all
import com.sumizuri.app.shared.resources.shelf_none
import com.sumizuri.app.shared.resources.shelves_title
import com.sumizuri.app.shared.resources.suki_live_tab
import com.sumizuri.app.ui.kit.chips.FilterTabs
import org.jetbrains.compose.resources.stringResource

// One entry in the unified scrollable bar above the library.
internal sealed interface LibraryTab {
    data object All : LibraryTab
    data class ByKind(val kind: Kind) : LibraryTab
    data object Uncategorized : LibraryTab
    data class Shelf(val id: Long) : LibraryTab
    data object Live : LibraryTab
}

// Kind tabs always appear in the Suki order: Video, Manga, Novels.
private val KindTabOrder = listOf(Kind.Anime, Kind.Manga, Kind.Novel)

// Kept for UpdatesViewModel, which still filters updates by shelf IDs.
internal fun shelfTabs(prefs: LibraryPrefs, categories: List<Category>, hasUncategorized: Boolean): List<Long?> =
    if (categories.isEmpty()) {
        listOf(null)
    } else {
        buildList {
            if (!prefs.hideAllTab) add(null)
            if (hasUncategorized && !prefs.hideNoShelfTab) add(UncategorizedShelf)
            categories.forEach { add(it.id) }
        }
    }

// Builds the unified tab order, excluding Live from horizontal swipe targets.
internal fun libraryTabs(
    prefs: LibraryPrefs,
    categories: List<Category>,
    hasUncategorized: Boolean,
    includeLive: Boolean = false,
): List<LibraryTab> = buildList {
    if (!prefs.hideAllTab) add(LibraryTab.All)
    KindTabOrder.forEach { add(LibraryTab.ByKind(it)) }
    if (categories.isNotEmpty() && hasUncategorized && !prefs.hideNoShelfTab) add(LibraryTab.Uncategorized)
    categories.forEach { add(LibraryTab.Shelf(it.id)) }
    if (includeLive) add(LibraryTab.Live)
}

internal fun LibraryState.swipeOrder(): List<LibraryTab> =
    libraryTabs(prefs, categories, hasUncategorized, includeLive = false)

internal fun LibraryState.activeTab(): LibraryTab = when (val cat = prefs.category) {
    UncategorizedShelf -> LibraryTab.Uncategorized

    null -> kind?.let { LibraryTab.ByKind(it) }
        ?: if (prefs.hideAllTab) LibraryTab.ByKind(KindTabOrder.first()) else LibraryTab.All

    else -> LibraryTab.Shelf(cat)
}

// Picks the first visible kind tab when "All" is hidden and neither a kind nor a shelf is selected.
internal fun effectiveLibraryKind(prefs: LibraryPrefs, kind: Kind?): Kind? =
    if (prefs.category == null && kind == null && prefs.hideAllTab) KindTabOrder.first() else kind

// The shelf to show when the open one has no tab any more, because the tab was hidden or the shelf is gone.
internal fun LibraryPrefs.settled(categories: List<Category>): LibraryPrefs {
    if (categories.isEmpty()) return if (category == null) this else copy(category = null)
    val fine = when (category) {
        null -> true
        UncategorizedShelf -> !hideNoShelfTab
        else -> categories.any { it.id == category }
    }
    return if (fine) this else copy(category = null)
}

// Unified library category bar: All (optional), Video, Manga, Novels, shelves, and Live (when visible).
@Composable
internal fun ShelfTabs(
    state: LibraryState,
    showLive: Boolean,
    liveOpen: Boolean,
    onSelect: (LibraryTab) -> Unit,
    onEdit: () -> Unit,
) {
    val all = stringResource(Res.string.all)
    val none = stringResource(Res.string.shelf_none)
    val liveLabel = stringResource(Res.string.suki_live_tab)
    val names = state.categories.associate { it.id to it.name }
    val tabs = libraryTabs(state.prefs, state.categories, state.hasUncategorized, includeLive = showLive)
    val items = tabs.map { tab ->
        tab to when (tab) {
            LibraryTab.All -> all
            is LibraryTab.ByKind -> stringResource(tab.kind.label)
            LibraryTab.Uncategorized -> none
            is LibraryTab.Shelf -> names[tab.id].orEmpty()
            LibraryTab.Live -> liveLabel
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FilterTabs(
            items = items,
            selected = if (liveOpen) LibraryTab.Live else state.activeTab(),
            onSelect = onSelect,
            modifier = Modifier.weight(1f),
            onLongPress = { onEdit() },
        )
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = stringResource(Res.string.shelves_title),
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
