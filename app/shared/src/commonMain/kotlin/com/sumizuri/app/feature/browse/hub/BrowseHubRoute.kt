package com.sumizuri.app.feature.browse.hub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.model.Repo
import com.sumizuri.app.data.sources.RepoMigration
import com.sumizuri.app.feature.browse.extensions.ExtensionsViewModel
import com.sumizuri.app.feature.browse.extensions.SourceFileImporter
import com.sumizuri.app.navigation.AppLinks
import com.sumizuri.app.source.repo.InstalledSource
import com.sumizuri.app.ui.kit.input.rememberFilePicker

@Composable
fun BrowseHubRoute(
    onOpenSource: (InstalledSource) -> Unit,
    onOpenRepo: (Repo) -> Unit,
    onOpenSearch: (String) -> Unit,
    onNewExtension: () -> Unit,
    onEditSource: (InstalledSource) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel =
        viewModel {
            BrowseHubViewModel(
                container.sources,
                container.repos,
                container.session,
                container.updates,
                container.library,
                container.manager,
            )
        }
    // The same model as the Repos tab, so adding from the bar opens the windows of that tab.
    val extensions =
        viewModel {
            ExtensionsViewModel(
                container.repos,
                container.updates,
                SourceFileImporter(container.manager, container.sources),
                RepoMigration(container.manager.repos, container.sources, container.database),
            )
        }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // A link that asks to add a repo fills the window of adding one, which still waits for the person to press Add.
    val linkedRepo by AppLinks.addRepo.collectAsStateWithLifecycle()
    LaunchedEffect(linkedRepo) {
        val address = linkedRepo ?: return@LaunchedEffect
        viewModel.onTab(BrowseTab.Extensions)
        extensions.onAddOpen()
        extensions.onAddress(address)
        AppLinks.repoTaken()
    }
    val pickFile = rememberFilePicker { path ->
        viewModel.onTab(BrowseTab.Extensions)
        extensions.onImportPicked(path)
    }
    val actions =
        remember(
            viewModel,
            extensions,
            onOpenSource,
            onOpenRepo,
            onOpenSearch,
            onNewExtension,
            onEditSource,
            pickFile,
        ) {
            BrowseHubActions(
                onTab = viewModel::onTab,
                onKind = viewModel::onKind,
                onGrouping = viewModel::onGrouping,
                onQuery = viewModel::onQuery,
                onOpenSource = onOpenSource,
                onPin = viewModel::onPin,
                onEnabled = viewModel::onEnabled,
                onEditCode = onEditSource,
                onRemove = viewModel::onRemove,
                onConfirmRemove = viewModel::onConfirmRemove,
                onCancelRemove = viewModel::onCancelRemove,
                onUpdateAll = viewModel::onUpdateAll,
                onOpenSearch = onOpenSearch,
                onOpenRepo = onOpenRepo,
                onAddRepo = {
                    viewModel.onTab(BrowseTab.Extensions)
                    extensions.onAddOpen()
                },
                onAddFile = pickFile,
                onNewSource = onNewExtension,
            )
        }
    BrowseHubScreen(state, actions)
}
