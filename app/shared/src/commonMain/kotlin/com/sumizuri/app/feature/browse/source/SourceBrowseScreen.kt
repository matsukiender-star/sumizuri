package com.sumizuri.app.feature.browse.source

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.feature.browse.asTitle
import com.sumizuri.app.feature.browse.repo.RemoveSourceDialog
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.browse_nothing
import com.sumizuri.app.shared.resources.browse_repeated
import com.sumizuri.app.ui.kit.cover.CoverTile
import com.sumizuri.app.ui.kit.cover.shownName
import com.sumizuri.app.ui.kit.page.LoadStatus
import com.sumizuri.app.ui.kit.page.PageGrid
import com.sumizuri.app.ui.kit.page.fullSpan
import org.jetbrains.compose.resources.stringResource

// One source with its tabs, a search, filters and its settings, and the entries as covers, rows or cards.
@Composable
fun SourceBrowseScreen(state: SourceBrowseState, style: BrowseStyle, actions: SourceBrowseActions) {
    val source = state.source
    val grid = rememberLazyGridState()
    var filtersOpen by remember { mutableStateOf(false) }
    var preferencesOpen by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<String?>(null) }
    LoadMoreWhenNear(grid, state.entries.size to state.loading, actions.onLoadMore)

    Column(Modifier.fillMaxSize()) {
        SourceBrowseTopBar(
            state,
            style,
            actions.onStyle,
            onBack = actions.onBack,
            onSaveTab = { saving = true },
            onFilters = {
                actions.onOpenFilters()
                filtersOpen = true
            },
            onPreferences = {
                actions.onOpenPreferences()
                preferencesOpen = true
            },
            onEditCode = actions.onEditCode,
            onImport = actions.onImport,
            onRemove = actions.onRemove,
        )
        PageGrid(maxCell = style.cell, state = grid, gap = if (style == BrowseStyle.Grid) 12.dp else 8.dp) {
            fullSpan {
                // The header keeps a readable width at the left, while the covers under it fill the whole row.
                Box(Modifier.fillMaxWidth()) { SourceBrowseHeader(state, actions, onEditTab = { editing = it }) }
            }
            items(state.entries, key = { it.url }) { entry ->
                val title = entry.asTitle(source.kind, source.id)
                val saved = entry.url in state.inLibrary || entry.url in state.adding
                val open = { actions.onOpenEntry(entry) }
                // A long press adds a title that is not saved yet.
                val add = if (saved) null else ({ actions.onQuickAdd(entry) })
                when (style) {
                    BrowseStyle.Grid -> CoverTile(
                        title,
                        onClick = open,
                        modifier = Modifier.animateItem(),
                        saved = saved,
                        onLongClick = add,
                    )

                    BrowseStyle.List -> BrowseLine(title, saved, open, Modifier.animateItem(), onLongClick = add)

                    BrowseStyle.Cards -> BrowseCard(title, saved, open, Modifier.animateItem(), onLongClick = add)
                }
            }
            fullSpan {
                Column {
                    LoadStatus(
                        state.loading,
                        state.error,
                        actions.onRetry,
                        onSolve = actions.onSolve.takeIf {
                            state.challengeUrl !=
                                null
                        },
                    )
                    if (!state.loading && state.error == null && state.entries.isEmpty()) {
                        Text(
                            stringResource(Res.string.browse_nothing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    if (state.repeated) {
                        Text(
                            stringResource(Res.string.browse_repeated),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }

    if (filtersOpen && state.filterGroups.isNotEmpty()) {
        SourceFilterSheet(state.filterGroups, state.filters, actions.onFilters, onDismiss = { filtersOpen = false })
    }
    if (preferencesOpen && state.preferences.isNotEmpty()) {
        SourcePreferencesSheet(state.preferences, state.preferenceValues, actions.onPreference, onDismiss = {
            preferencesOpen = false
            actions.onClosePreferences()
        })
    }
    if (saving) SavedTabDialog(state.activeQuery.orEmpty(), actions.onSaveTab, onDismiss = { saving = false })
    editing?.let { name ->
        SavedTabDialog(
            initial = name,
            onConfirm = { actions.onRenameTab(name, it) },
            onDismiss = { editing = null },
            onDelete = { actions.onDeleteTab(name) },
        )
    }
    state.removing?.let {
        RemoveSourceDialog(it.copy(name = source.shownName()), actions.onConfirmRemove, actions.onCancelRemove)
    }
    // The page closes once its source is gone.
    LaunchedEffect(state.removed) { if (state.removed) actions.onBack() }
}

// Asks for the next page when the last few items are in view, and again after each page lands in case the list is still short.
@Composable
private fun LoadMoreWhenNear(grid: LazyGridState, progress: Any, onLoadMore: () -> Unit) {
    val near by remember(grid) {
        derivedStateOf {
            val info = grid.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 6
        }
    }
    LaunchedEffect(near, progress) { if (near) onLoadMore() }
}
