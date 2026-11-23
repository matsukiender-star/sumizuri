package com.sumizuri.app.feature.browse.source

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.feature.browse.asTitle
import com.sumizuri.app.feature.browse.repo.RemoveSourceDialog
import com.sumizuri.app.feature.library.LibraryPickCalls
import com.sumizuri.app.feature.library.top.LibraryPickTop
import com.sumizuri.app.feature.library.top.ShelvesDialog
import com.sumizuri.app.feature.title.info.EditTitleDialog
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.browse_nothing
import com.sumizuri.app.shared.resources.browse_repeated
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.ui.kit.cover.CoverPick
import com.sumizuri.app.ui.kit.cover.CoverTile
import com.sumizuri.app.ui.kit.cover.shownName
import com.sumizuri.app.ui.kit.page.LoadStatus
import com.sumizuri.app.ui.kit.page.PageGrid
import com.sumizuri.app.ui.kit.page.fullSpan
import com.sumizuri.app.ui.motion.Rise
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

// One source with its tabs, a search, filters and its settings, and the entries as covers, rows or cards.
@Composable
fun SourceBrowseScreen(
    state: SourceBrowseState,
    style: BrowseStyle,
    actions: SourceBrowseActions,
    selection: SourceSelectionModel,
    picks: SourcePicks? = null,
) {
    val source = state.source
    val grid = rememberLazyGridState()
    var filtersOpen by remember { mutableStateOf(false) }
    var preferencesOpen by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<String?>(null) }
    var hovered by remember { mutableStateOf<SourceEntry?>(null) }
    var shownHover by remember { mutableStateOf<SourceEntry?>(null) }
    var loadingPreviewUrl by remember { mutableStateOf<String?>(null) }
    val previewCache = remember(source.id) { mutableStateMapOf<String, SourceEntry>() }
    val previewEnabled = style != BrowseStyle.Cards
    LoadMoreWhenNear(grid, state.entries.size to state.loading, actions.onLoadMore)
    // A long press on a cover picks it, and while covers are picked a tap picks another and the bars change.
    val picked by selection.picked.collectAsStateWithLifecycle()
    val savingNow by selection.saving.collectAsStateWithLifecycle()
    val picking = picked.isNotEmpty()
    var shelvesOpen by remember { mutableStateOf(false) }
    var coverFor by remember { mutableStateOf<Long?>(null) }
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), isBackEnabled = picking) {
        selection.clear()
    }

    // Shows the hover panel after a brief pause and fetches missing details only for that single entry.
    LaunchedEffect(hovered?.url, previewEnabled) {
        val target = hovered
        if (!previewEnabled || target == null) {
            shownHover = null
            return@LaunchedEffect
        }
        delay(HoverPreviewDelayMs)
        val cached = previewCache[target.url]
        shownHover = cached ?: target
        if (cached == null && !target.hasPreviewInfo()) {
            loadingPreviewUrl = target.url
            try {
                val fetched = actions.onFetchDetails(target)
                val merged = target.mergeDetails(fetched)
                previewCache[target.url] = merged
                if (hovered?.url == target.url) shownHover = merged
            } finally {
                if (loadingPreviewUrl == target.url) loadingPreviewUrl = null
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (picking) {
                LibraryPickTop(
                    picked.size,
                    state.entries.size,
                    LibraryPickCalls(
                        onClose = selection::clear,
                        onSelectAll = { selection.selectAll(state.entries) },
                        onInvert = { selection.invert(state.entries) },
                        onMarkRead = {},
                        onFavorite = {},
                        onShelves = {},
                        onMove = {},
                        onMigrate = {},
                        onRemove = {},
                    ),
                )
            } else {
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
            }
            PageGrid(maxCell = style.cell, state = grid, gap = if (style == BrowseStyle.Grid) 12.dp else 8.dp) {
                fullSpan {
                    // The header keeps a readable width at the left, while the covers under it fill the whole row.
                    Box(Modifier.fillMaxWidth()) { SourceBrowseHeader(state, actions, onEditTab = { editing = it }) }
                }
                picks?.let {
                    fullSpan {
                        SourcePicksRow(
                            it,
                            state,
                            actions,
                            picked.keys,
                            savingNow,
                            selection::toggle,
                        )
                    }
                }
                items(state.entries, key = { it.url }) { entry ->
                    val title = entry.asTitle(source.kind, source.id)
                    val saved = entry.url in state.inLibrary || entry.url in savingNow
                    val pick = { selection.toggle(entry) }
                    val open = if (picking) pick else ({ actions.onOpenEntry(entry) })
                    val isPicked = entry.url in picked
                    val hoverMod = Modifier
                        .animateItem()
                        .mouseHoverPreview(
                            enabled = previewEnabled,
                            key = entry.url,
                            onEnter = { hovered = entry },
                            onExit = { if (hovered?.url == entry.url) hovered = null },
                        )
                    when (style) {
                        BrowseStyle.Grid -> CoverTile(
                            title,
                            onClick = open,
                            modifier = hoverMod,
                            saved = saved,
                            pick = CoverPick(picking, isPicked),
                            onLongClick = pick,
                        )

                        BrowseStyle.List -> BrowseLine(
                            title,
                            saved,
                            open,
                            hoverMod,
                            onLongClick = pick,
                            picked = isPicked,
                        )

                        BrowseStyle.Cards -> BrowseCard(
                            title,
                            saved,
                            open,
                            Modifier.animateItem(),
                            onLongClick = pick,
                            picked = isPicked,
                        )
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
        val activeHover = shownHover?.let { previewCache[it.url] ?: it }.takeIf { !picking }
        HoverPreviewBanner(
            entry = activeHover,
            kind = source.kind,
            sourceId = source.id,
            loading = activeHover != null && loadingPreviewUrl == activeHover.url,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        // The bar of actions rises from the bottom while covers are picked.
        Rise(picking, Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars)) {
            SourcePickBottom(
                picked.size,
                onAdd = selection::onAdd,
                onFavorite = selection::onFavorite,
                onShelves = { shelvesOpen = true },
                onCover = { selection.onCover { coverFor = it } },
            )
        }
    }
    if (shelvesOpen) {
        val shelves by selection.shelfList.collectAsStateWithLifecycle(emptyList())
        ShelvesDialog(
            categories = shelves.filter { it.id >= 0 && (it.kind == null || it.kind == source.kind) },
            picked = picked.size,
            onShelves = emptyMap(),
            onCreate = selection::createShelf,
            onApply = { add, remove ->
                selection.onShelves(add, remove)
                shelvesOpen = false
            },
            onDismiss = { shelvesOpen = false },
        )
    }
    coverFor?.let { id ->
        val container = LocalAppContainer.current
        val title by remember(id) { container.library.title(id) }.collectAsStateWithLifecycle(null)
        title?.let { EditTitleDialog(it, onRefresh = {}) { coverFor = null } }
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
