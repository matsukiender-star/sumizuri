package com.sumizuri.app.feature.migration.single

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.backup.ImportedSources
import com.sumizuri.app.data.library.LibraryRepository
import com.sumizuri.app.data.migration.TitleMigration
import com.sumizuri.app.data.sources.SourceRepository
import com.sumizuri.app.source.SourceManager
import com.sumizuri.app.source.SourceResult
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.repo.InstalledSource
import com.sumizuri.app.source.repo.originIdOf
import com.sumizuri.app.ui.kit.cover.localSourceName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.compose.resources.getString

private const val SearchDelayMs = 500L
private const val AtOnce = 3
private const val PerSource = 8

// Looks for a saved title in other sources, and moves it to the entry the person picks.
class MigrateViewModel(
    private val titleId: Long,
    private val library: LibraryRepository,
    private val sources: SourceRepository,
    private val manager: SourceManager,
    private val migration: TitleMigration,
) : ViewModel() {
    private val mutableState = MutableStateFlow(MigrateState())
    val state: StateFlow<MigrateState> = mutableState

    private var searchJob: Job? = null
    private var pickJob: Job? = null
    private var ownSourceId = ""
    private var ownUrl = ""

    init {
        viewModelScope.launch {
            val title = library.title(titleId).first()
            val origin = library.origin(titleId)
            val installed = sources.installed.first()
            ownSourceId = origin?.sourceId.orEmpty()
            ownUrl = origin?.externalId.orEmpty()
            val candidates = installed.filter { it.enabled && it.kind == title?.kind }.sortedBy { it.name.lowercase() }
            mutableState.update {
                it.copy(
                    loaded = true,
                    title = title,
                    from =
                    installed.firstOrNull { source -> source.id == ownSourceId }?.let { source ->
                        localSourceName(source.id)?.let { getString(it) }
                            ?: source.name
                    }
                        ?: (ImportedSources.labelOf(ownSourceId) ?: originIdOf(ownSourceId)),
                    query = title?.name.orEmpty(),
                    candidates = candidates,
                    chosen = candidates.map { source -> source.id }.toSet(),
                )
            }
            if (title != null) restart(0)
        }
    }

    fun onQuery(text: String) {
        mutableState.update { it.copy(query = text) }
        restart(SearchDelayMs)
    }

    fun onToggleSource(id: String) {
        mutableState.update { it.copy(chosen = if (id in it.chosen) it.chosen - id else it.chosen + id) }
        restart(SearchDelayMs)
    }

    fun onChooseAll(all: Boolean) {
        mutableState.update {
            it.copy(
                chosen = if (all) it.candidates.map { source -> source.id }.toSet() else emptySet(),
            )
        }
        restart(SearchDelayMs)
    }

    fun onToggleKeep() = mutableState.update { it.copy(keepOld = !it.keepOld) }

    // Reads the chapters of the entry, only so the person can see what would carry over before they agree.
    fun onPick(source: InstalledSource, entry: SourceEntry) {
        pickJob?.cancel()
        mutableState.update { it.copy(picked = MigrateChoice(source, entry), problem = null) }
        pickJob = viewModelScope.launch {
            val opened = when (val result = manager.open(source)) {
                is SourceResult.Err -> return@launch failPick(result.error.message)
                is SourceResult.Ok -> result.value
            }
            when (val result = opened.chapters(entry)) {
                is SourceResult.Err -> failPick(result.error.message)

                is SourceResult.Ok -> {
                    val plan = migration.plan(titleId, result.value)
                    mutableState.update { it.copy(picked = it.picked?.copy(plan = plan)) }
                }
            }
        }
    }

    fun onDismissPick() {
        pickJob?.cancel()
        mutableState.update { it.copy(picked = null) }
    }

    // Moves the title, then tells the caller the id of the title to open.
    fun onConfirm(onMoved: (Long) -> Unit) {
        val now = mutableState.value
        val choice = now.picked ?: return
        if (now.working || choice.plan == null) return
        mutableState.update { it.copy(working = true, problem = null) }
        viewModelScope.launch {
            try {
                // The chapters read for the plan are not written: the title brings its marked chapters, and the page of the title that opens next asks the source for its chapters and details.
                val id = migration.migrate(
                    titleId,
                    choice.source.id,
                    choice.source.kind,
                    choice.entry,
                    null,
                    now.keepOld,
                )
                AppLog.info("migrate", "title $titleId moved to ${choice.source.id} as title $id")
                onMoved(id)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                AppLog.error("migrate", "title $titleId could not be moved", error)
                mutableState.update { it.copy(working = false, problem = error.message.orEmpty()) }
            }
        }
    }

    private fun failPick(message: String) = mutableState.update { it.copy(picked = it.picked?.copy(error = message)) }

    // Searches every ticked source for the name, a few at a time, and shows each answer as it comes.
    private fun restart(delayMs: Long) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(delayMs)
            val now = mutableState.value
            val query = now.query.trim()
            val wanted = now.candidates.filter { it.id in now.chosen }
            if (query.isEmpty() || wanted.isEmpty()) {
                mutableState.update { it.copy(results = emptyList()) }
                return@launch
            }
            mutableState.update { it.copy(results = wanted.map { source -> MigrateResult(source) }) }
            val gate = Semaphore(AtOnce)
            coroutineScope {
                wanted.map { source -> async { gate.withPermit { settle(searchOne(source, query)) } } }.awaitAll()
            }
        }
    }

    private suspend fun searchOne(source: InstalledSource, query: String): MigrateResult {
        val opened = when (val result = manager.open(source)) {
            is SourceResult.Err -> return MigrateResult(source, loading = false, error = result.error.message)
            is SourceResult.Ok -> result.value
        }
        return when (val found = opened.search(query, 1)) {
            is SourceResult.Err -> MigrateResult(source, loading = false, error = found.error.message)

            is SourceResult.Ok -> MigrateResult(
                source,
                entries = found.value.filterNot { source.id == ownSourceId && it.url == ownUrl }.take(PerSource),
                loading = false,
            )
        }
    }

    private fun settle(result: MigrateResult) = mutableState.update { now ->
        now.copy(results = now.results.map { if (it.source.id == result.source.id) result else it })
    }
}
