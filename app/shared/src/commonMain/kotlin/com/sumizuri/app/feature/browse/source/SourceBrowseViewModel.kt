package com.sumizuri.app.feature.browse.source

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.data.library.LibraryRepository
import com.sumizuri.app.data.local.LocalImport
import com.sumizuri.app.data.local.LocalSources
import com.sumizuri.app.data.local.importLocalFiles
import com.sumizuri.app.data.model.SavedCatalog
import com.sumizuri.app.data.settings.CatalogRepository
import com.sumizuri.app.data.settings.SessionRepository
import com.sumizuri.app.data.settings.catalogKey
import com.sumizuri.app.data.sources.SourceRepository
import com.sumizuri.app.feature.browse.repo.RemovePrompt
import com.sumizuri.app.source.Source
import com.sumizuri.app.source.SourceError
import com.sumizuri.app.source.SourceManager
import com.sumizuri.app.source.SourceResult
import com.sumizuri.app.source.browser.ChallengeSolver
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.repo.InstalledSource
import com.sumizuri.app.source.valueOrNull
import com.sumizuri.app.ui.kit.input.PickedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SearchDelayMs = 500L

// One source opened for browsing, with its popular and latest lists, the lists it offers, a search with filters, and the searches the reader kept as tabs.
class SourceBrowseViewModel(
    source: InstalledSource,
    private val library: LibraryRepository,
    private val sources: SourceRepository,
    private val catalogs: CatalogRepository,
    private val manager: SourceManager,
    private val session: SessionRepository,
    private val solver: ChallengeSolver,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        SourceBrowseState(
            source,
            localFolder = LocalSources
                .folderOf(
                    manager.localFolder,
                    source.id,
                ).toString()
                .takeIf {
                    LocalSources.isLocal(source.id)
                },
        ),
    )
    val state: StateFlow<SourceBrowseState> = mutableState

    private val key = catalogKey(source)
    private var job: Job? = null
    private var page = 1
    private var preferencesChanged = false
    private var saving: Job? = null

    init {
        viewModelScope.launch { sources.markUsed(source.id) }
        viewModelScope.launch {
            library.savedKeys(source.id).collect { keys -> mutableState.update { it.copy(inLibrary = keys) } }
        }
        viewModelScope.launch {
            catalogs
                .observe(
                    key,
                ).collect { list -> mutableState.update { it.copy(savedTabs = list) } }
        }
        viewModelScope.launch {
            opened()?.let { reading ->
                mutableState.update {
                    it.copy(
                        hasFilters = reading.info.hasFilters,
                        hasPreferences = reading.info.hasPreferences,
                    )
                }
                readCatalogs(reading)
            }
            // The list of the last visit comes back first, when the source still has it.
            val last = session.browseListing()?.lowercase()
            val known =
                last == TabPopular || last == TabLatest ||
                    mutableState.value.catalogs.any { last == CatalogMark + it.id }
            if (known && last != null) mutableState.update { it.copy(tab = last) }
            load(1)
        }
        // A source that changes while its page is open, by an update or in the editor, is used as it is now, and the list is loaded again with it.
        viewModelScope.launch {
            sources.installed.collect { all ->
                val now = all.firstOrNull { it.id == source.id } ?: return@collect
                if (now.version == mutableState.value.source.version) return@collect
                mutableState.update { it.copy(source = now) }
                load(1)
            }
        }
    }

    fun onQuery(text: String) {
        mutableState.update { it.copy(query = text) }
        // Waits for a pause in typing, so each key does not start a search.
        job?.cancel()
        job = viewModelScope.launch {
            delay(SearchDelayMs)
            val query = text.trim()
            when {
                query.isNotEmpty() -> mutableState.update { it.copy(tab = TabSearch, activeQuery = query) }

                mutableState.value.tab == TabSearch -> mutableState.update {
                    it.copy(
                        tab = TabPopular,
                        activeQuery = null,
                    )
                }

                else -> return@launch
            }
            load(1)
        }
    }

    fun onTab(tab: String) {
        val now = mutableState.value
        if (tab == now.tab && tab != TabSearch) return
        // A saved tab brings its filters, so they can be changed from there.
        val saved = now.savedTabs.firstOrNull { SavedMark + it.name == tab }
        mutableState.update { it.copy(tab = tab, filters = saved?.filters ?: it.filters) }
        if (tab == TabPopular || tab == TabLatest ||
            tab.startsWith(CatalogMark)
        ) {
            viewModelScope.launch { session.setBrowseListing(tab) }
        }
        reload()
    }

    // Reads the filters of the source the first time the sheet is opened.
    fun onOpenFilters() {
        if (mutableState.value.filterGroups.isNotEmpty()) return
        viewModelScope.launch {
            val reading = opened() ?: return@launch
            reading.filters().valueOrNull()?.let { groups -> mutableState.update { it.copy(filterGroups = groups) } }
        }
    }

    fun onFilters(filters: Map<String, Any?>) {
        mutableState.update { it.copy(filters = filters, tab = TabSearch) }
        reload()
    }

    // Keeps the search and the filters that are showing as a tab of their own.
    fun onSaveTab(name: String) {
        val now = mutableState.value
        val tab = SavedCatalog(name.trim(), now.activeQuery.orEmpty(), now.filters)
        if (tab.name.isEmpty()) return
        viewModelScope.launch {
            catalogs.save(key, now.savedTabs.filter { it.name != tab.name } + tab)
            mutableState.update { it.copy(tab = SavedMark + tab.name) }
        }
    }

    fun onRenameTab(old: String, name: String) {
        val now = mutableState.value
        if (name.isBlank()) return
        viewModelScope.launch {
            catalogs.save(key, now.savedTabs.map { if (it.name == old) it.copy(name = name.trim()) else it })
            if (now.tab == SavedMark + old) mutableState.update { it.copy(tab = SavedMark + name.trim()) }
        }
    }

    fun onDeleteTab(name: String) {
        val now = mutableState.value
        viewModelScope.launch { catalogs.save(key, now.savedTabs.filter { it.name != name }) }
        if (now.tab == SavedMark + name) onTab(TabPopular)
    }

    // Reads the settings of the source and their values when the sheet opens.
    fun onOpenPreferences() {
        viewModelScope.launch {
            val reading = opened() ?: return@launch
            val list = reading.sourcePreferences().valueOrNull() ?: return@launch
            mutableState.update { it.copy(preferences = list, preferenceValues = reading.preferences()) }
        }
    }

    fun onPreference(key: String, value: Any?) {
        mutableState.update { it.copy(preferenceValues = it.preferenceValues + (key to value)) }
        preferencesChanged = true
        val before = saving
        saving = viewModelScope.launch {
            before?.join()
            opened()?.setPreference(key, value)
        }
    }

    // A source can build its tabs and lists from its settings, so a change is shown once the sheet closes and the last change is saved.
    fun onClosePreferences() {
        if (!preferencesChanged) return
        preferencesChanged = false
        val last = saving
        viewModelScope.launch {
            last?.join()
            val reading = opened() ?: return@launch
            readCatalogs(reading)
            reload()
        }
    }

    fun onLoadMore() {
        val now = mutableState.value
        if (now.loading || !now.hasMore) return
        job?.cancel()
        job = viewModelScope.launch { load(page + 1) }
    }

    fun onRetry() = reload()

    // Adds an entry to the library from its cover, with its details and chapters when the source gives them, without opening it. One that is saved or being added already is left alone.
    fun onQuickAdd(entry: SourceEntry) {
        val now = mutableState.value
        if (entry.url in now.inLibrary || entry.url in now.adding) return
        mutableState.update { it.copy(adding = it.adding + entry.url) }
        viewModelScope.launch {
            val reading = opened()
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
            library.addFromSource(now.source.id, now.source.kind, full, chapters)
            mutableState.update { it.copy(adding = it.adding - entry.url) }
        }
    }

    // A source that saved titles came from is only removed after a yes that says what happens to them.
    fun onRemove() {
        val now = mutableState.value
        viewModelScope.launch {
            val titles = library.countOf(now.source.id)
            if (titles ==
                0
            ) {
                remove()
            } else {
                mutableState.update { it.copy(removing = RemovePrompt(now.source.id, now.source.name, titles)) }
            }
        }
    }

    fun onConfirmRemove() {
        mutableState.update { it.copy(removing = null) }
        remove()
    }

    fun onCancelRemove() = mutableState.update { it.copy(removing = null) }

    private fun remove() {
        job?.cancel()
        viewModelScope.launch {
            val id = mutableState.value.source.id
            manager.close(id)
            sources.remove(id)
            mutableState.update { it.copy(removed = true) }
        }
    }

    // Brings picked files into the folder of a local source and lists its titles again.
    fun onImport(files: List<PickedFile>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                importLocalFiles(
                    manager.localFolder,
                    mutableState.value.source.id,
                    files.map {
                        LocalImport(it.name, it.path, it.temporary)
                    },
                )
            }
            reload()
        }
    }

    // Opens the site for a person to pass the bot check, then asks again.
    fun onSolve() {
        val now = mutableState.value
        val url = now.challengeUrl ?: now.source.baseUrl.takeIf { it.isNotBlank() } ?: return
        job?.cancel()
        job = viewModelScope.launch {
            solver.solve(url)
            load(1)
        }
    }

    private fun reload() {
        job?.cancel()
        job = viewModelScope.launch { load(1) }
    }

    // The source of the page as it is now, started if it is not running, or null when it cannot start.
    private suspend fun opened(): Source? = manager.open(mutableState.value.source).valueOrNull()

    // The lists the source offers besides popular and latest, which it may build from its settings.
    private suspend fun readCatalogs(reading: Source) {
        if (reading.info.hasCatalogs) {
            reading.catalogs().valueOrNull()?.let { list ->
                mutableState.update {
                    it.copy(catalogs = list)
                }
            }
        }
    }

    private suspend fun load(number: Int) {
        mutableState.update {
            it.copy(
                loading = true,
                error = null,
                challengeUrl = null,
                repeated = false,
                entries = if (number ==
                    1
                ) {
                    emptyList()
                } else {
                    it.entries
                },
            )
        }
        val now = mutableState.value
        val result = when (val opened = manager.open(now.source)) {
            is SourceResult.Err -> opened
            is SourceResult.Ok -> fetch(opened.value, now, number)
        }
        when (result) {
            is SourceResult.Err -> mutableState.update {
                it.copy(
                    loading = false,
                    error = result.error.message,
                    challengeUrl = (result.error as? SourceError.Challenge)?.url,
                    hasMore = false,
                )
            }

            is SourceResult.Ok -> {
                page = number
                mutableState.update { current ->
                    val known = current.entries.mapTo(HashSet()) { it.url }
                    val fresh = result.value.filter { known.add(it.url) }
                    // A page with nothing new ends the list, which also stops a source that repeats its last page.
                    current.copy(
                        entries = current.entries + fresh,
                        loading = false,
                        hasMore = fresh.isNotEmpty(),
                        repeated = number > 1 && fresh.isEmpty() && result.value.isNotEmpty(),
                    )
                }
            }
        }
    }

    // The page of the list the tab stands for.
    private suspend fun fetch(source: Source, now: SourceBrowseState, number: Int): SourceResult<List<SourceEntry>> {
        val tab = now.tab
        val saved = now.savedTabs.firstOrNull { SavedMark + it.name == tab }
        return when {
            tab == TabLatest -> source.latest(number)
            tab == TabSearch -> source.search(now.activeQuery.orEmpty(), number, now.filters)
            tab.startsWith(CatalogMark) -> source.catalog(tab.removePrefix(CatalogMark), number)
            saved != null -> source.search(saved.query, number, saved.filters)
            else -> source.popular(number)
        }
    }
}
