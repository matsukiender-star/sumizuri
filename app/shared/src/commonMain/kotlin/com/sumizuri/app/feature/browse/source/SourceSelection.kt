package com.sumizuri.app.feature.browse.source

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.data.library.CategoryRepository
import com.sumizuri.app.data.library.LibraryRepository
import com.sumizuri.app.data.model.library.Category
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.library_shelves
import com.sumizuri.app.shared.resources.pick_favorite
import com.sumizuri.app.shared.resources.suki_pick_add
import com.sumizuri.app.shared.resources.suki_pick_cover
import com.sumizuri.app.source.Source
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.repo.InstalledSource
import com.sumizuri.app.source.valueOrNull
import com.sumizuri.app.ui.kit.dock.DockAction
import com.sumizuri.app.ui.kit.dock.PickDock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// Saves an entry of a source in the library with its details and chapters when the source gives them, and gives its id. One in the library already is left as it is.
internal suspend fun saveEntry(
    library: LibraryRepository,
    source: InstalledSource,
    reading: Source?,
    entry: SourceEntry,
): Long {
    library.libraryId(source.id, entry)?.let { return it }
    val details = reading?.details(entry)?.valueOrNull()
    val full =
        details?.copy(
            url = entry.url,
            title = details.title.ifBlank { entry.title },
            coverUrl =
            details.coverUrl ?: entry.coverUrl,
        )
            ?: entry
    val chapters = reading?.chapters(full)?.valueOrNull().orEmpty()
    return library.addFromSource(source.id, source.kind, full, chapters)
}

// The covers picked on the page of a source, by their address in the order they were picked, and what is done to them at once: into the library, as favorites, onto shelves, or one with a cover of its own.
class SourceSelectionModel(
    private val source: () -> InstalledSource,
    private val library: LibraryRepository,
    private val shelves: CategoryRepository,
    private val open: suspend () -> Source?,
) : ViewModel() {
    private val mutablePicked = MutableStateFlow<Map<String, SourceEntry>>(emptyMap())
    val picked: StateFlow<Map<String, SourceEntry>> = mutablePicked

    // The addresses being saved now, which show as saved already.
    private val mutableSaving = MutableStateFlow<Set<String>>(emptySet())
    val saving: StateFlow<Set<String>> = mutableSaving

    val shelfList: Flow<List<Category>> = shelves.categories

    fun toggle(entry: SourceEntry) = mutablePicked.update {
        if (entry.url in
            it
        ) {
            it - entry.url
        } else {
            it + (entry.url to entry)
        }
    }

    // Picks every entry shown, or none when all of them are picked already.
    fun selectAll(entries: List<SourceEntry>) = mutablePicked.update { now ->
        if (entries.all { it.url in now }) emptyMap() else entries.associateBy { it.url }
    }

    fun invert(entries: List<SourceEntry>) = mutablePicked.update { now ->
        entries
            .filter {
                it.url !in now
            }.associateBy { it.url }
    }

    fun clear() {
        mutablePicked.value = emptyMap()
    }

    fun onAdd() = saveThen {}

    fun onFavorite() = saveThen { library.setFavorite(it, true) }

    fun onShelves(add: Set<Long>, remove: Set<Long>) = saveThen { shelves.change(it, add, remove) }

    // Saves the one picked title and gives its id, for its cover to be changed.
    fun onCover(show: (Long) -> Unit) = saveThen { ids -> ids.singleOrNull()?.let(show) }

    suspend fun createShelf(name: String): Long? = shelves.add(name)

    // Ends the picking, saves the picked titles one after another, as a site may not take many at once, and then does the rest with their ids.
    private fun saveThen(then: suspend (List<Long>) -> Unit) {
        val entries = mutablePicked.value.values.toList()
        if (entries.isEmpty()) return
        mutablePicked.value = emptyMap()
        val urls = entries.map { it.url }.toSet()
        mutableSaving.update { it + urls }
        viewModelScope.launch {
            try {
                val reading = open()
                then(entries.map { saveEntry(library, source(), reading, it) })
            } finally {
                mutableSaving.update { it - urls }
            }
        }
    }
}

// The dock of the picked covers of a source, the same as the one of the library: how many, all, flip and stop, and what can be done to them.
@Composable
internal fun SourcePickBottom(
    count: Int,
    total: Int,
    onAll: () -> Unit,
    onInvert: () -> Unit,
    onClose: () -> Unit,
    onAdd: () -> Unit,
    onFavorite: () -> Unit,
    onShelves: () -> Unit,
    onCover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PickDock(count, count == total, onAll, onClose, modifier, onInvert = onInvert) {
        DockAction(Icons.Outlined.LibraryAdd, stringResource(Res.string.suki_pick_add), false, onAdd)
        DockAction(Icons.Outlined.Star, stringResource(Res.string.pick_favorite), false, onFavorite)
        DockAction(Icons.AutoMirrored.Outlined.Label, stringResource(Res.string.library_shelves), false, onShelves)
        if (count == 1) DockAction(Icons.Outlined.Image, stringResource(Res.string.suki_pick_cover), false, onCover)
    }
}
