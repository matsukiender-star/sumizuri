package com.sumizuri.app.feature.updates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.feed.FeedRowStyle
import com.sumizuri.app.data.model.feed.UpdateItem
import com.sumizuri.app.feature.library.top.UpdateFailuresSheet
import com.sumizuri.app.feature.library.top.swipeShelves
import com.sumizuri.app.feature.updates.row.FeedAction
import com.sumizuri.app.feature.updates.row.FeedDayHeading
import com.sumizuri.app.feature.updates.row.Readable
import com.sumizuri.app.feature.updates.row.feed
import com.sumizuri.app.feature.updates.row.groupByHeading
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.display_updates_title
import com.sumizuri.app.shared.resources.selection_mark_read
import com.sumizuri.app.shared.resources.undo
import com.sumizuri.app.shared.resources.updates_empty
import com.sumizuri.app.shared.resources.updates_mark_all_message
import com.sumizuri.app.shared.resources.updates_mark_all_title
import com.sumizuri.app.shared.resources.updates_marked
import com.sumizuri.app.ui.kit.dialog.ConfirmDialog
import com.sumizuri.app.ui.kit.page.LocalBottomClearance
import com.sumizuri.app.ui.kit.page.PageGrid
import com.sumizuri.app.ui.kit.page.ReadableWhen
import com.sumizuri.app.ui.kit.page.fullSpan
import com.sumizuri.app.ui.kit.scroll.PullRefresh
import com.sumizuri.app.ui.motion.entrance
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// The windows the page can open over itself.
private enum class Window { None, Display, Failures, MarkAll, Upcoming }

// The updates page as in the Flutter app. A bar on top, then the note about titles that could not be checked, the kinds and the shelves as tabs, and the new chapters that are not read, under a heading for each day. A list keeps a readable column and a grid uses the whole width. Pulling the page down checks the library for new chapters, and a sideways swipe changes the shelf unless a swipe is set to mark a row read.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpdatesScreen(state: UpdatesState, calls: UpdatesCalls) {
    var window by remember { mutableStateOf(Window.None) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val marked = stringResource(Res.string.updates_marked)
    val undo = stringResource(Res.string.undo)
    val prefs = state.prefs
    val grid = prefs.style.isGrid
    // With nothing to swipe between, rows keep their swipe to mark read.
    val rowSwipe = prefs.swipeMarksRead || state.tabs.size < 2

    // Marks the items read and offers to take that back for a moment.
    fun markRead(items: List<UpdateItem>) {
        calls.onMarkRead(items, true)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(marked, undo) == SnackbarResult.ActionPerformed) calls.onMarkRead(items, false)
        }
    }

    Box(Modifier.fillMaxSize()) {
        ReadableWhen(!grid) {
            Column(Modifier.fillMaxSize()) {
                UpdatesTopBar(state, onUpcoming = {
                    window = Window.Upcoming
                }, onMarkAll = {
                    window = Window.MarkAll
                }, onUpdate = calls.onUpdate, onCancelUpdate = calls.onCancelUpdate, onDisplay = {
                    window =
                        Window.Display
                })
                // The bar shows how far the check has got, so the pull has no spinner of its own.
                PullRefresh(state.progress != null, calls.onUpdate, Modifier.weight(1f)) {
                    Box(Modifier.fillMaxSize().swipeShelves(state.tabs, state.shelf, !rowSwipe, calls.onShelf)) {
                        PageGrid(maxCell = state.library.cellWidth.dp) {
                            fullSpan { UpdatesHeader(state, calls) { window = Window.Failures } }
                            if (state.loaded && state.items.isEmpty()) {
                                fullSpan {
                                    Text(
                                        stringResource(Res.string.updates_empty),
                                        Modifier.entrance().fillMaxWidth().padding(32.dp),
                                        color = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            }
                            for ((heading, group) in groupByHeading(state.items, grid, state.now)) {
                                fullSpan {
                                    Readable(!grid) {
                                        FeedDayHeading(
                                            heading,
                                            group.first().at,
                                            group.size,
                                            onMarkAllRead = if (!grid &&
                                                prefs.rowStyle == FeedRowStyle.Inbox
                                            ) {
                                                ({ markRead(group) })
                                            } else {
                                                null
                                            },
                                        )
                                    }
                                }
                                feed(
                                    group,
                                    prefs.style,
                                    prefs.rowStyle,
                                    rowSwipe,
                                    FeedAction.MarkRead,
                                    calls.onOpenTitle,
                                    ::markRead,
                                )
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = LocalBottomClearance.current))
    }
    val close = { window = Window.None }
    when (window) {
        Window.None -> Unit

        Window.Display -> FeedDisplaySheet(
            Res.string.display_updates_title,
            prefs,
            state.library.cellWidth,
            calls.onPrefs,
            calls.onCoverSize,
            close,
            swipeSwitch = true,
        )

        Window.Failures -> UpdateFailuresSheet(state.failures, calls.onOpenTitle, close)

        Window.Upcoming -> UpcomingSheet({ id ->
            close()
            calls.onOpenTitle(id)
        }, close)

        Window.MarkAll -> ConfirmDialog(
            stringResource(Res.string.updates_mark_all_title),
            stringResource(Res.string.updates_mark_all_message),
            stringResource(Res.string.selection_mark_read),
            onConfirm = {
                markRead(state.items)
                close()
            },
            onDismiss = close,
        )
    }
}
