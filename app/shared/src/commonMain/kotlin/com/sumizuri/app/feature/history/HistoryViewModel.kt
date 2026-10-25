package com.sumizuri.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.core.perf.timed
import com.sumizuri.app.data.db.chapter.UpdateRow
import com.sumizuri.app.data.library.LibraryRepository
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.data.model.feed.FeedPrefs
import com.sumizuri.app.data.model.feed.UpdateItem
import com.sumizuri.app.data.model.feed.coarseHeading
import com.sumizuri.app.data.model.feed.listHeading
import com.sumizuri.app.data.model.feed.mergeByGroup
import com.sumizuri.app.data.model.feed.mergeSameDay
import com.sumizuri.app.data.model.library.LibraryPrefs
import com.sumizuri.app.data.settings.FeedPrefsRepository
import com.sumizuri.app.data.settings.LibraryPrefsRepository
import com.sumizuri.app.data.text.nowMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// The choices made on the page itself, which are not kept between runs.
private data class Look(val kind: Kind? = null, val query: String = "")

private class Content(val rows: List<UpdateRow>, val titles: List<Title>)

private class Settings(val feed: FeedPrefs, val library: LibraryPrefs, val look: Look)

// What was read, folded by day, narrowed by kind and by a search in the name, and the taking of it out of the history with a way back.
class HistoryViewModel(
    private val library: LibraryRepository,
    private val feedPrefs: FeedPrefsRepository,
    private val libraryPrefs: LibraryPrefsRepository,
) : ViewModel() {
    private val look = MutableStateFlow(Look())

    // When each chapter in the list was read, so a removal can be taken back with the same times.
    private var times = emptyMap<Long, Long>()

    // The times of the last removal.
    private var removed = emptyMap<Long, Long>()

    // Only the titles are needed here, not their shelves. They are the titles of the reads themselves, so a title read without being in the library shows too.
    private val content = library.readingTitles.map { (rows, titles) -> Content(rows, titles) }

    // The reads folded by day for the last rows, kept while only the search or the kind changes.
    private var merged: Triple<Content, Boolean, List<UpdateItem>>? = null

    private val settings = combine(feedPrefs.prefs, libraryPrefs.prefs, look, ::Settings)

    val state: StateFlow<HistoryState> = combine(content, settings) { now, set ->
        timed("History list") {
            build(now, set)
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryState())

    private fun build(now: Content, set: Settings): HistoryState {
        val clock = nowMillis()
        times = now.rows.associate { it.id to it.at }
        val typed = set.look
        val words = typed.query.trim()
        val grid = set.feed.style.isGrid
        val folded = merged?.takeIf { (from, wasGrid) -> from === now && wasGrid == grid }?.third ?: run {
            val titles = now.titles.associateBy { it.id }
            // A title read over several days is one row for each stretch the page has a heading for.
            val items =
                mergeByGroup(mergeSameDay(now.rows, titles), clock, if (grid) ::coarseHeading else ::listHeading)
            items.also { merged = Triple(now, grid, it) }
        }
        val items = folded.filter { item ->
            (typed.kind == null || item.title.kind == typed.kind) &&
                (words.isEmpty() || item.title.name.contains(words, ignoreCase = true))
        }
        return HistoryState(
            true,
            items,
            typed.kind,
            typed.query,
            set.feed,
            set.library,
            clock,
            folded.mapTo(
                HashSet(),
            ) {
                it.title.kind
            },
        )
    }

    fun onKind(kind: Kind?) = look.update { it.copy(kind = kind) }

    fun onQuery(query: String) = look.update { it.copy(query = query) }

    fun onPrefs(change: (FeedPrefs) -> FeedPrefs) {
        viewModelScope.launch { feedPrefs.update(change) }
    }

    fun onCoverSize(width: Int) {
        viewModelScope.launch { libraryPrefs.update { it.copy(cellWidth = width) } }
    }

    // Takes the chapters of the items out of the history. They stay read.
    fun onRemove(items: List<UpdateItem>) {
        val ids = items.flatMap { it.chapterIds }
        removed = ids.mapNotNull { id -> times[id]?.let { id to it } }.toMap()
        viewModelScope.launch { library.forgetReadings(ids) }
    }

    fun onUndo() {
        val back = removed
        viewModelScope.launch { library.restoreReadings(back) }
    }
}
