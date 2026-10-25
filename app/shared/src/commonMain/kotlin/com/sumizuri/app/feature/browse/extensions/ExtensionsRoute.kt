package com.sumizuri.app.feature.browse.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.model.Repo
import com.sumizuri.app.data.sources.RepoMigration

@Composable
fun ExtensionsRoute(onOpenRepo: (Repo) -> Unit, onNewExtension: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel =
        viewModel {
            ExtensionsViewModel(
                container.repos,
                container.updates,
                SourceFileImporter(container.manager, container.sources),
                RepoMigration(container.manager.repos, container.sources, container.database),
            )
        }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ExtensionsScreen(
        state = state,
        onOpenRepo = onOpenRepo,
        onCheck = viewModel::onCheck,
        onUpdate = viewModel::onUpdate,
        onUpdateAll = viewModel::onUpdateAll,
        onAddOpen = viewModel::onAddOpen,
        onAddress = viewModel::onAddress,
        onAddConfirm = viewModel::onAddConfirm,
        onAddDismiss = viewModel::onAddDismiss,
        onRemoveAsk = viewModel::onRemoveAsk,
        onRemoveConfirm = viewModel::onRemoveConfirm,
        onRemoveDismiss = viewModel::onRemoveDismiss,
        onImportPicked = viewModel::onImportPicked,
        onImportName = viewModel::onImportName,
        onImportKind = viewModel::onImportKind,
        onImportConfirm = viewModel::onImportConfirm,
        onImportDismiss = viewModel::onImportDismiss,
        onNewExtension = onNewExtension,
        onMoveAsk = viewModel::onMoveAsk,
        onMoveTarget = viewModel::onMoveTarget,
        onMoveRemoveOld = viewModel::onMoveRemoveOld,
        onMoveConfirm = viewModel::onMoveConfirm,
        onMoveDismiss = viewModel::onMoveDismiss,
    )
}
