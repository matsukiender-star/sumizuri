package com.sumizuri.app.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumizuri.app.data.LocalAppContainer

@Composable
fun HistoryRoute(onOpenTitle: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { HistoryViewModel(container.library, container.historyPrefs, container.libraryPrefs) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val calls = remember(viewModel, onOpenTitle) {
        HistoryCalls(
            onOpenTitle = onOpenTitle,
            onKind = viewModel::onKind,
            onQuery = viewModel::onQuery,
            onPrefs = viewModel::onPrefs,
            onCoverSize = viewModel::onCoverSize,
            onRemove = viewModel::onRemove,
            onUndo = viewModel::onUndo,
        )
    }
    HistoryScreen(state, calls)
}
