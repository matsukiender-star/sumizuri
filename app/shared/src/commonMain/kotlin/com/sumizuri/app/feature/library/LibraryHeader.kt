package com.sumizuri.app.feature.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.sumizuri.app.core.perf.Perf
import com.sumizuri.app.core.perf.StartMark
import com.sumizuri.app.data.model.controls.LibraryCoverAction
import com.sumizuri.app.data.model.library.LibrarySection
import com.sumizuri.app.feature.library.display.LibraryDisplaySheet
import com.sumizuri.app.feature.library.display.LibrarySortSheet
import com.sumizuri.app.feature.library.part.LibraryPartOf
import com.sumizuri.app.feature.library.part.partItems
import com.sumizuri.app.feature.library.tile.LibraryTile
import com.sumizuri.app.feature.library.tile.TileCalls
import com.sumizuri.app.feature.library.tile.TileContext
import com.sumizuri.app.feature.library.top.ConnectionLostNotice
import com.sumizuri.app.feature.library.top.FailuresNotice
import com.sumizuri.app.feature.library.top.LibraryControls
import com.sumizuri.app.feature.library.top.LibraryPickBottom
import com.sumizuri.app.feature.library.top.LibraryPickTop
import com.sumizuri.app.feature.library.top.LibrarySearchRow
import com.sumizuri.app.feature.library.top.LibraryTopBar
import com.sumizuri.app.feature.library.top.MissingSourcesNotice
import com.sumizuri.app.feature.library.top.NoticeAction
import com.sumizuri.app.feature.library.top.SearchSourcesRow
import com.sumizuri.app.feature.library.top.ShelfTabs
import com.sumizuri.app.feature.library.top.ShelvesDialog
import com.sumizuri.app.feature.library.top.UpdateFailuresSheet
import com.sumizuri.app.feature.library.top.shelfOrder
import com.sumizuri.app.feature.library.top.swipeShelves
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.library_empty
import com.sumizuri.app.shared.resources.library_nothing_matches
import com.sumizuri.app.shared.resources.library_part_hidden
import com.sumizuri.app.shared.resources.library_remove
import com.sumizuri.app.shared.resources.library_remove_message
import com.sumizuri.app.shared.resources.library_show_all
import com.sumizuri.app.shared.resources.library_show_them
import com.sumizuri.app.shared.resources.library_title
import com.sumizuri.app.shared.resources.library_unreachable
import com.sumizuri.app.shared.resources.undo
import com.sumizuri.app.ui.controls.LocalAppGestures
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import com.sumizuri.app.ui.kit.cover.CoverPick
import com.sumizuri.app.ui.kit.dialog.ConfirmDialog
import com.sumizuri.app.ui.kit.input.SearchFocus
import com.sumizuri.app.ui.kit.page.LocalBottomClearance
import com.sumizuri.app.ui.kit.page.LocalHideBottomBar
import com.sumizuri.app.ui.kit.page.PageGrid
import com.sumizuri.app.ui.kit.page.fullSpan
import com.sumizuri.app.ui.kit.scroll.PullRefresh
import com.sumizuri.app.ui.motion.AppMotion
import com.sumizuri.app.ui.motion.Rise
import com.sumizuri.app.ui.motion.entrance
import com.sumizuri.app.ui.motion.motionMs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// What stands above the covers, which scrolls with them: the notes, the search, the shelves as tabs and the row of chips for the order, the kinds and the filters.
@Composable
internal fun LibraryHeader(
    state: LibraryState,
    calls: LibraryCalls,
    showSearch: Boolean,
    onSort: () -> Unit,
    onFailures: () -> Unit,
) {
    val prefs = state.prefs
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val notices = motionMs(AppMotion.Medium)
        AnimatedVisibility(
            state.unreachable > 0 && (state.unreachable > prefs.unreachableDismissed || state.onlyUnreachable),
            enter =
            fadeIn(tween(notices)) + expandVertically(tween(notices)),
            exit =
            fadeOut(tween(notices)) + shrinkVertically(tween(notices)),
        ) {
            // The note leads to where the sources are mended, and can also narrow the grid to the titles it is about. Closing it keeps it away until more titles are lost.
            MissingSourcesNotice(
                pluralStringResource(Res.plurals.library_unreachable, state.unreachable, state.unreachable),
                calls.onOpenMissing,
                extra = NoticeAction(
                    stringResource(
                        if (state.onlyUnreachable) Res.string.library_show_all else Res.string.library_show_them,
                    ),
                    calls.onToggleUnreachable,
                ),
                onDismiss = if (state.onlyUnreachable) {
                    null
                } else {
                    (
                        {
                            calls.onPrefs { it.copy(unreachableDismissed = state.unreachable) }
                        }
                        )
                },
            )
        }
        AnimatedVisibility(
            state.failures.isNotEmpty() && !state.selecting,
            enter =
            fadeIn(tween(notices)) + expandVertically(tween(notices)),
            exit =
            fadeOut(tween(notices)) + shrinkVertically(tween(notices)),
        ) {
            FailuresNotice(state.failures.size, onFailures, calls.onDismissFailures)
        }
        AnimatedVisibility(
            state.unchecked > 0 && state.progress == null && !state.selecting,
            enter =
            fadeIn(tween(notices)) + expandVertically(tween(notices)),
            exit =
            fadeOut(tween(notices)) + shrinkVertically(tween(notices)),
        ) {
            ConnectionLostNotice(state.unchecked, calls.onUpdate, calls.onDismissFailures)
        }
        AnimatedVisibility(
            showSearch,
            enter = fadeIn(tween(notices)) + expandVertically(tween(notices)),
            exit =
            fadeOut(tween(notices)) + shrinkVertically(tween(notices)),
        ) {
            LibrarySearchRow(state.query, calls.onQuery)
        }
        // The same words can be looked for in every source, for a title the library does not have yet.
        AnimatedVisibility(
            showSearch && state.query.isNotBlank() && !state.selecting,
            enter =
            fadeIn(tween(notices)) + expandVertically(tween(notices)),
            exit =
            fadeOut(tween(notices)) + shrinkVertically(tween(notices)),
        ) {
            SearchSourcesRow(state.query) { calls.onSearchSources(state.query.trim()) }
        }
        ShelfTabs(state, calls::onShelf, calls.onOpenShelves)
        LibraryControls(state, calls, onSort)
    }
}
