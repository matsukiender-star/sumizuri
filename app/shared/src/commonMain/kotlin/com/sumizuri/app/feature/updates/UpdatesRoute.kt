package com.sumizuri.app.feature.updates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumizuri.app.data.LocalAppContainer

@Composable
fun UpdatesRoute(onOpenTitle: (Long) -> Unit, onOpenMissing: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel =
        viewModel {
            UpdatesViewModel(
                container.library,
                container.sync,
                container.updatesPrefs,
                container.libraryPrefs,
                container.categories,
                container.sources,
            )
        }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val calls = remember(viewModel, onOpenTitle, onOpenMissing) {
        UpdatesCalls(
            onOpenTitle = onOpenTitle,
            onKind = viewModel::onKind,
            onShelf = viewModel::onShelf,
            onPrefs = viewModel::onPrefs,
            onCoverSize = viewModel::onCoverSize,
            onMarkRead = viewModel::onMarkRead,
            onUpdate = viewModel::onUpdate,
            onCancelUpdate = viewModel::onCancelUpdate,
            onDismissFailures = viewModel::onDismissFailures,
            onOpenMissing = onOpenMissing,
        )
    }
    UpdatesScreen(state, calls)
}
