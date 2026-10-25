package com.sumizuri.app.feature.library.top

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sumizuri.app.data.model.library.QuickFilter
import com.sumizuri.app.feature.library.LibraryCalls
import com.sumizuri.app.feature.library.LibraryState
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.library_sort
import com.sumizuri.app.ui.kit.chips.ChipRow
import com.sumizuri.app.ui.kit.chips.ChipSeparator
import com.sumizuri.app.ui.kit.chips.KindChips
import com.sumizuri.app.ui.kit.chips.SortChip
import com.sumizuri.app.ui.kit.chips.ToggleChip
import com.sumizuri.app.ui.kit.chips.hasKindChoice
import org.jetbrains.compose.resources.stringResource

// One row of chips under the shelves: the order, the kinds when the library has more than one, and the quick filters, each one tap away.
@Composable
internal fun LibraryControls(
    state: LibraryState,
    calls: LibraryCalls,
    onSort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs = state.prefs
    ChipRow(modifier) {
        SortChip(
            stringResource(state.rule.sort.label),
            state.rule.ascending,
            onSort,
            stringResource(Res.string.library_sort),
        )
        if (hasKindChoice(state.kinds, state.kind)) {
            ChipSeparator()
            KindChips(state.kind, state.kinds, calls.onKind)
        }
        ChipSeparator()
        for (filter in QuickFilter.entries.filter { prefs.favoritesEnabled || it != QuickFilter.Favorite }) {
            val on = filter in prefs.filters
            ToggleChip(stringResource(filter.label), on, {
                calls.onPrefs {
                    it.copy(
                        filters = if (on) {
                            it.filters - filter
                        } else {
                            it.filters +
                                filter
                        },
                    )
                }
            })
        }
    }
}
