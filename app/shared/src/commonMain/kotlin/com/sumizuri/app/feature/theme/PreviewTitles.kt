package com.sumizuri.app.feature.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.data.model.library.LibrarySortField
import kotlinx.coroutines.flow.map

// The most titles the preview shows, which fills the biggest grid of the desktop preview.
private const val Most = 24

// The titles of the library read most lately, so the preview shows the app as the person has it. Empty while it loads and when the library is empty.
@Composable
internal fun rememberPreviewTitles(): List<Title> {
    val library = LocalAppContainer.current.library
    val titles = remember(library) {
        library.entries(setOf(LibrarySortField.LastRead)).map { all ->
            all.sortedByDescending { it.lastRead }.take(Most).map { it.title }
        }
    }
    return titles.collectAsStateWithLifecycle(emptyList()).value
}
