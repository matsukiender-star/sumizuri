package com.sumizuri.app.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.model.library.LibrarySection
import kotlinx.coroutines.launch

@Composable
fun LibraryRoute(onOpenTitle: (Long) -> Unit, onOpenChapter: (Long) -> Unit, onOpenShelves: () -> Unit, onMove: (Long) -> Unit, onMigrate: (List<Long>) -> Unit, onSeeAll: (LibrarySection) -> Unit, onOpenMissing: () -> Unit, onOpenDownloads: () -> Unit, onSearchSources: (String) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { LibraryViewModel(container.library, container.session, container.libraryPrefs, container.categories, container.sources, container.sync) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val calls = remember(viewModel, onOpenTitle, onOpenChapter, onOpenShelves, onMove, onMigrate, onSeeAll, onOpenMissing, onOpenDownloads, onSearchSources) {
        LibraryCalls(
            onQuery = viewModel::onQuery,
            onKind = viewModel::onKind,
            onToggleUnreachable = viewModel::onToggleUnreachable,
            onPrefs = viewModel::onPrefs,
            onShelfRule = viewModel::onShelfRule,
            onSelect = viewModel::onSelect,
            onClearSelection = viewModel::onClearSelection,
            onSelectAll = viewModel::onSelectAll,
            onInvert = viewModel::onInvertSelection,
            onMarkRead = viewModel::onMarkRead,
            onFavorite = viewModel::onFavorite,
            onRemove = viewModel::onRemove,
            onShelves = viewModel::onShelves,
            onDropOnShelf = viewModel::onDropOnShelf,
            onCreateShelf = viewModel::onCreateShelf,
            onOpenTitle = onOpenTitle,
            onOpenShelves = onOpenShelves,
            onSeeAll = onSeeAll,
            onMove = onMove,
            onMigrate = onMigrate,
            onUpdate = viewModel::onUpdate,
            onCancelUpdate = viewModel::onCancelUpdate,
            onDismissFailures = viewModel::onDismissFailures,
            onRandom = { viewModel.randomTitle()?.let(onOpenTitle) },
            onOpenMissing = onOpenMissing,
            onOpenDownloads = onOpenDownloads,
            onSearchSources = onSearchSources,
            onContinue = { id -> scope.launch { continueChapterOf(container.library, id)?.let(onOpenChapter) ?: onOpenTitle(id) } },
        )
    }
    LibraryScreen(state, calls)
}
