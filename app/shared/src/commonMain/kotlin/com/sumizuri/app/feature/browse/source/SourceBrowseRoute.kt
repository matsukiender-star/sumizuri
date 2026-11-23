package com.sumizuri.app.feature.browse.source

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.settings.SettingKey
import com.sumizuri.app.data.text.enumNamed
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.repo.InstalledSource
import com.sumizuri.app.source.valueOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun SourceBrowseRoute(
    source: InstalledSource,
    onBack: () -> Unit,
    onOpenSaved: (Long) -> Unit,
    onOpenPreview: (InstalledSource, SourceEntry) -> Unit,
    onEditCode: () -> Unit,
) {
    val container = LocalAppContainer.current
    // Opened from a row of recommendations, the page shows that genre. A model made now starts with it, and one kept from before is told.
    val asked = remember(source.id) { GenreRequest.take(source.id) }
    var made = false
    val viewModel = viewModel(key = "browse_${source.id}") {
        made = true
        SourceBrowseViewModel(
            source,
            container.library,
            container.sources,
            container.catalogs,
            container.manager,
            container.session,
            container.solver,
            asked,
        )
    }
    LaunchedEffect(asked) { if (asked != null && !made) viewModel.onOpenGenre(asked) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picksModel = viewModel(key = "picks_${source.id}") {
        SourcePicksViewModel(source.id, container.database.titles(), container.settings, container.reading) {
            // The source as the page has it now, since opening an older copy would replace the updated one.
            container.manager.open(viewModel.state.value.source).valueOrNull()
        }
    }
    val picks by picksModel.picks.collectAsStateWithLifecycle()
    val popularShown = state.tab == TabPopular && state.entries.isNotEmpty()
    LaunchedEffect(picksModel, popularShown) { if (popularShown) picksModel.onPopularShown() }
    val style by remember {
        container.settings.text(SettingKey.BrowseStyle).map {
            enumNamed<BrowseStyle>(it)
                ?: BrowseStyle.Grid
        }
    }.collectAsStateWithLifecycle(BrowseStyle.Grid)
    val scope = rememberCoroutineScope()
    val actions = remember(viewModel, onBack, onOpenSaved, onOpenPreview, onEditCode) {
        SourceBrowseActions(
            onBack = onBack,
            onQuery = viewModel::onQuery,
            onTab = viewModel::onTab,
            onLoadMore = viewModel::onLoadMore,
            onRetry = viewModel::onRetry,
            onSolve = viewModel::onSolve,
            onOpenEntry = { entry ->
                // An entry that is saved already opens as the saved title.
                scope.launch {
                    val saved = container.library.savedId(source.id, entry)
                    if (saved != null) onOpenSaved(saved) else onOpenPreview(source, entry)
                }
            },
            onOpenFilters = viewModel::onOpenFilters,
            onFilters = viewModel::onFilters,
            onOpenGenre = viewModel::onOpenGenre,
            onSaveTab = viewModel::onSaveTab,
            onRenameTab = viewModel::onRenameTab,
            onDeleteTab = viewModel::onDeleteTab,
            onOpenPreferences = viewModel::onOpenPreferences,
            onPreference = viewModel::onPreference,
            onClosePreferences = viewModel::onClosePreferences,
            onEditCode = onEditCode,
            onFetchDetails = viewModel::fetchDetails,
            onImport = viewModel::onImport,
            onStyle = { scope.launch { container.settings.setText(SettingKey.BrowseStyle, it.name) } },
            onRemove = viewModel::onRemove,
            onConfirmRemove = viewModel::onConfirmRemove,
            onCancelRemove = viewModel::onCancelRemove,
        )
    }
    val selection = viewModel(key = "pick_${source.id}") {
        SourceSelectionModel({ viewModel.state.value.source }, container.library, container.categories) {
            container.manager.open(viewModel.state.value.source).valueOrNull()
        }
    }
    SourceBrowseScreen(state, style, actions, selection, picks.takeIf { popularShown })
}
