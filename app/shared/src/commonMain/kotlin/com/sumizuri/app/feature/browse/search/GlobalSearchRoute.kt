package com.sumizuri.app.feature.browse.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.repo.InstalledSource
import kotlinx.coroutines.launch

@Composable
fun GlobalSearchRoute(
    initial: String,
    kind: Int?,
    onBack: () -> Unit,
    onOpenSaved: (Long) -> Unit,
    onOpenPreview: (InstalledSource, SourceEntry) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel =
        viewModel {
            GlobalSearchViewModel(
                initial,
                kind,
                container.sources,
                container.manager,
                container.library,
                container.solver,
            )
        }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    GlobalSearchScreen(
        state = state,
        onBack = onBack,
        onQuery = viewModel::onQuery,
        onKind = viewModel::onKind,
        onRetrySource = viewModel::onRetrySource,
        onSolveSource = viewModel::onSolveSource,
    ) { source, entry ->
        // An entry that is saved already opens as the saved title.
        scope.launch {
            val saved = viewModel.savedId(source, entry)
            if (saved != null) onOpenSaved(saved) else onOpenPreview(source, entry)
        }
    }
}
