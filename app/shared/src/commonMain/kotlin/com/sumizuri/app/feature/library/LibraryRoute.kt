package com.sumizuri.app.feature.library

import com.sumizuri.app.feature.browse.source.GenreRequest
import com.sumizuri.app.feature.library.part.LibraryPicksModel
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.repo.InstalledSource
import com.sumizuri.app.source.valueOrNull
import kotlinx.coroutines.flow.map
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
fun LibraryRoute(onOpenTitle: (Long) -> Unit, onOpenChapter: (Long) -> Unit, onOpenShelves: () -> Unit, onMove: (Long) -> Unit, onMigrate: (List<Long>) -> Unit, onSeeAll: (LibrarySection) -> Unit, onOpenMissing: () -> Unit, onOpenDownloads: () -> Unit, onSearchSources: (String) -> Unit, showLive: Boolean = false, onOpenLive: () -> Unit = {}, onOpenSync: () -> Unit = {}, onPlayLive: (String, String) -> Unit = { _, _ -> }, onOpenPreview: (InstalledSource, SourceEntry) -> Unit = { _, _ -> }, onOpenSource: (InstalledSource) -> Unit = {}) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { LibraryViewModel(container.library, container.session, container.libraryPrefs, container.categories, container.sources, container.sync) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picksModel = viewModel(key = "library_picks") {
        LibraryPicksModel(
            container.sources.installed,
            container.session.browsePinned(),
            container.libraryPrefs.prefs.map { it.countOf(LibrarySection.Recommendations) },
            container.database.titles(),
            container.settings,
            container.reading,
        ) { container.manager.open(it).valueOrNull() }
    }
    val picks by picksModel.picks.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val calls = remember(viewModel, onOpenTitle, onOpenChapter, onOpenShelves, onMove, onMigrate, onSeeAll, onOpenMissing, onOpenDownloads, onSearchSources, onOpenLive, onOpenSync, onPlayLive, onOpenPreview, onOpenSource) {
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
            onSelectShelf = viewModel::onSelectShelf,
            onOpenLive = onOpenLive,
            onOpenSync = onOpenSync,
            onPlayLive = onPlayLive,
            onOpenPick = { source, entry ->
                // A title that is saved already opens as the saved title.
                scope.launch { container.library.savedId(source.id, entry)?.let(onOpenTitle) ?: onOpenPreview(source, entry) }
            },
            onOpenPickGenre = { source, filters ->
                GenreRequest.ask(source.id, filters)
                onOpenSource(source)
            },
        )
    }
    LibraryScreen(state, calls, showLive, picks)
}
