package com.sumizuri.app.feature.migration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumizuri.app.data.LocalAppContainer

// The page for moving many titles at once. The work lives in the session of the app, so it goes on while the page is left, and the ids say which titles to take, with none for the whole library.
@Composable
fun MigrationRoute(ids: List<Long>, onBack: () -> Unit, onOpenTitle: (Long) -> Unit) {
    val container = LocalAppContainer.current
    val session = container.migrations
    val state by session.state.collectAsStateWithLifecycle()
    val shelves by container.categories.categories.collectAsStateWithLifecycle(emptyList())
    LaunchedEffect(ids) { session.load(ids) }
    MigrationScreen(state, shelves, session, onBack, onOpenTitle)
}
