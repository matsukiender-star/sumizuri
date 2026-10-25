package com.sumizuri.app.feature.migration.single

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumizuri.app.data.LocalAppContainer

@Composable
fun MigrateRoute(titleId: Long, onBack: () -> Unit, onMoved: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "migrate_$titleId") {
        MigrateViewModel(titleId, container.library, container.sources, container.manager, container.migration)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    MigrateScreen(
        state = state,
        onBack = onBack,
        onQuery = viewModel::onQuery,
        onToggleSource = viewModel::onToggleSource,
        onChooseAll = viewModel::onChooseAll,
        onPick = viewModel::onPick,
    )
    MigrateConfirmDialog(state, viewModel::onToggleKeep, { viewModel.onConfirm(onMoved) }, viewModel::onDismissPick)
}
