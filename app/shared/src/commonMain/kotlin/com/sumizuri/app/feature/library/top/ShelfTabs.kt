package com.sumizuri.app.feature.library.top

import androidx.compose.runtime.Composable
import com.sumizuri.app.data.model.library.Category
import com.sumizuri.app.data.model.library.LibraryPrefs
import com.sumizuri.app.feature.library.LibraryState
import com.sumizuri.app.feature.library.UncategorizedShelf
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.all
import com.sumizuri.app.shared.resources.shelf_none
import com.sumizuri.app.ui.kit.chips.FilterTabs
import org.jetbrains.compose.resources.stringResource

// The shelves in the order of the tabs, which is all the titles, the titles on no shelf, and then each shelf, less the two tabs the person hid. A swipe goes through them in the same order.
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

internal fun LibraryState.shelfOrder(): List<Long?> = shelfTabs(prefs, categories, hasUncategorized)

// The shelf to show when the open one has no tab any more, because the tab was hidden or the shelf is gone.
internal fun LibraryPrefs.settled(categories: List<Category>): LibraryPrefs {
    if (categories.isEmpty()) return if (category == null) this else copy(category = null)
    val fine = when (category) {
        null -> !hideAllTab
        UncategorizedShelf -> !hideNoShelfTab
        else -> categories.any { it.id == category }
    }
    return if (fine) this else copy(category = if (hideAllTab) categories.first().id else null)
}

// The shelves as tabs. A long press on a tab opens the page where the shelves are changed.
@Composable
internal fun ShelfTabs(state: LibraryState, onShelf: (Long?) -> Unit, onEdit: () -> Unit) {
    if (state.categories.isEmpty()) return
    val all = stringResource(Res.string.all)
    val none = stringResource(Res.string.shelf_none)
    val names = state.categories.associate { it.id to it.name }
    val items = state.shelfOrder().map { id ->
        id to when (id) {
            null -> all
            UncategorizedShelf -> none
            else -> names.getValue(id)
        }
    }
    FilterTabs(items, state.prefs.category, onShelf, onLongPress = { onEdit() })
}
