package com.sumizuri.app.data.migration

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.backup.ImportedSources
import com.sumizuri.app.data.library.LibraryRepository
import com.sumizuri.app.data.model.SourceState
import com.sumizuri.app.data.sources.SourceRepository
import com.sumizuri.app.data.work.BusyKind
import com.sumizuri.app.data.work.BusyWork
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.busy_migration_moving
import com.sumizuri.app.shared.resources.busy_migration_source
import com.sumizuri.app.source.SourceManager
import com.sumizuri.app.source.SourceResult
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.repo.InstalledSource
import com.sumizuri.app.source.repo.originIdOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.jetbrains.compose.resources.getString
import kotlin.concurrent.Volatile

// The work of moving titles to other sources, kept for as long as the app runs so a search goes on while the person looks at something else. It holds the titles, the sources and the rules, searches the sources one after another for the titles that are still open, and moves the matches that were chosen.
class MigrationSession(
    private val library: LibraryRepository,
    private val sourcesRepository: SourceRepository,
    private val manager: SourceManager,
    private val migration: TitleMigration,
    private val prefs: MigrationPrefsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutable = MutableStateFlow(MigrationState())
    val state: StateFlow<MigrationState> = mutable

    private var job: Job? = null

    @Volatile
    private var stopped = false
    private var loadedKey: String? = null

    // What is known of each title for the search, read once per load. Several searches at once read and fill it, so it is only touched under its lock.
    private val subjects = HashMap<Long, MigrationSubject>()
    private val subjectsLock = Mutex()

    // A run or a move is under way, including one that was started and has not yet said so in the state.
    private val working: Boolean get() = mutable.value.busy || job?.isActive == true

    init {
        scope.launch { prefs.rules.collect { rules -> mutable.update { it.copy(rules = rules) } } }
    }

    // Takes the titles with these ids, or the whole library with none, unless that is what is loaded already. A title of the whole library starts out included only when its source cannot be reached.
    suspend fun load(ids: List<Long>) {
        val key = if (ids.isEmpty()) "library" else ids.sorted().joinToString(",")
        if (working || (key == loadedKey && mutable.value.loaded)) return
        loadedKey = key
        val installed = sourcesRepository.installed.first()
        val names = installed.associate { it.id to it.name }
        val wanted = ids.toSet()
        val entries = library.entries().first().filter { wanted.isEmpty() || it.title.id in wanted }
        val items = entries.sortedBy { it.title.name.lowercase() }.map { entry ->
            val title = entry.title
            MigrationItem(
                title,
                names[title.sourceId] ?: ImportedSources.labelOf(title.sourceId) ?: originIdOf(title.sourceId),
                entry.categories,
                included = wanted.isNotEmpty() || title.sourceState != SourceState.Ready,
            )
        }
        subjectsLock.withLock { subjects.clear() }
        val kinds = items.map { it.title.kind }.toSet()
        val off = prefs.off()
        val order = prefs.order()
        val usable = installed.filter { it.kind in kinds }.sortedWith(compareBy({ order.indexOf(it.id).let { at -> if (at < 0) Int.MAX_VALUE else at } }, { it.name.lowercase() }))
        mutable.update { it.copy(loaded = true, items = items, sources = usable.map { source -> SourceChoice(source, included = source.enabled && source.id !in off) }, processed = 0, total = 0, currentSource = null) }
    }

    fun setRules(rules: MigrationRules) {
        mutable.update { it.copy(rules = rules) }
        scope.launch { prefs.setRules(rules) }
    }

    // Which titles are in the work.
    fun setIncluded(ids: Set<Long>, included: Boolean) = changeItems { if (it.id in ids && it.status != MigrationStatus.Moved) it.copy(included = included) else it }

    fun setSourceIncluded(id: String, included: Boolean) {
        mutable.update { now -> now.copy(sources = now.sources.map { if (it.source.id == id) it.copy(included = included) else it }) }
        saveSources()
    }

    fun setSourcesIncluded(ids: Set<String>, included: Boolean) {
        mutable.update { now -> now.copy(sources = now.sources.map { if (it.source.id in ids) it.copy(included = included) else it }) }
        saveSources()
    }

    // Puts the sources in the order of the ids, which is the order they are tried in.
    fun setSourceOrder(ids: List<String>) {
        mutable.update { now -> now.copy(sources = ids.mapNotNull { id -> now.sources.firstOrNull { it.source.id == id } } + now.sources.filter { it.source.id !in ids }) }
        saveSources()
    }

    private fun saveSources() {
        val now = mutable.value.sources
        scope.launch {
            prefs.setOrder(now.map { it.source.id })
            prefs.setOff(now.filter { !it.included }.map { it.source.id }.toSet())
        }
    }

    // Starts a run: each source in order is asked about the titles that are still open, and a source that keeps failing is left out.
    fun start() {
        if (working) return
        stopped = false
        job = scope.launch { BusyWork.keep(BusyKind.Migration, ::progressNow, ::stop, ::detailNow) { run() } }
    }

    // How far the run got, for the notice that keeps it going while the app is out of sight.
    private fun progressNow(): Pair<Int, Int> = mutable.value.let { it.processed to it.total }

    // The source a search is on, or that titles are being moved, as the second line of the notice.
    private suspend fun detailNow(): String? = mutable.value.let { now ->
        when {
            now.moving -> getString(Res.string.busy_migration_moving)
            now.currentSource != null -> getString(Res.string.busy_migration_source, now.currentSource, now.sourceIndex, now.sourceCount)
            else -> null
        }
    }

    // Stops the run or the move at once. The searches that were waiting on a source are dropped and their titles go back in the queue, and a title that was being moved stays where it was.
    fun stop() {
        stopped = true
        job?.cancel()
    }

    private suspend fun run() {
        mutable.update { it.copy(running = true, processed = 0, total = 0, sources = it.sources.map { choice -> choice.copy(asked = 0, matched = 0, failures = 0, gaveUp = false) }) }
        try {
            val chosen = mutable.value.sources.filter { it.included }
            for ((index, choice) in chosen.withIndex()) {
                if (stopped) break
                val todo = pendingFor(choice.source)
                if (todo.isEmpty()) continue
                mutable.update { it.copy(currentSource = choice.source.name, sourceIndex = index + 1, sourceCount = chosen.size, processed = 0, total = todo.size) }
                pass(choice.source, todo)
            }
        } finally {
            mutable.update { now -> now.copy(running = false, currentSource = null, sourceIndex = 0, sourceCount = 0, items = now.items.map { if (it.status == MigrationStatus.Searching) it.copy(status = MigrationStatus.Queued) else it }) }
        }
    }

    // The titles a source should be asked about: the ones of its kind that are included and still open, and with the best match of all kept, the ones that have a match too.
    private fun pendingFor(source: InstalledSource): List<Long> {
        val rules = mutable.value.rules
        return mutable.value.items
            .filter { item ->
                item.included && item.title.kind == source.kind && when (item.status) {
                    MigrationStatus.Queued, MigrationStatus.NotFound, MigrationStatus.Failed -> true
                    MigrationStatus.Found, MigrationStatus.Review -> rules.compareAll
                    else -> false
                }
            }.map { it.id }
    }

    private suspend fun pass(installed: InstalledSource, todo: List<Long>) {
        val opened = when (val result = manager.open(installed)) {
            is SourceResult.Err -> {
                for (id in todo) setItem(id) { it.copy(status = MigrationStatus.Failed, error = result.error.message) }
                countSource(installed.id) { it.copy(failures = it.failures + todo.size, gaveUp = true) }
                mutable.update { it.copy(processed = it.total) }
                return
            }

            is SourceResult.Ok -> result.value
        }
        val rules = mutable.value.rules
        val gate = Semaphore(rules.parallel.coerceIn(1, 4))
        val lock = Mutex()
        var streak = 0
        var gaveUp = false
        coroutineScope {
            todo
                .map { id ->
                    async {
                        gate.withPermit {
                            if (stopped || gaveUp) {
                                mutable.update { it.copy(processed = it.processed + 1) }
                                return@withPermit
                            }
                            val failed = searchOne(MigrationSearch(installed, opened, mutable.value.rules) { stopped }, installed, id)
                            lock.withLock {
                                streak = if (failed) streak + 1 else 0
                                if (streak >= rules.giveUpAfter.coerceAtLeast(1) && !gaveUp) {
                                    gaveUp = true
                                    countSource(installed.id) { it.copy(gaveUp = true) }
                                }
                            }
                            mutable.update { it.copy(processed = it.processed + 1) }
                        }
                    }
                }.awaitAll()
        }
    }

    // Searches one source for one title, and gives true when the source could not be searched.
    private suspend fun searchOne(search: MigrationSearch, installed: InstalledSource, id: Long): Boolean {
        val item = mutable.value.items.firstOrNull { it.id == id } ?: return false
        setItem(id) { it.copy(status = MigrationStatus.Searching) }
        val subject = subjectOf(id)
        if (subject == null) {
            setItem(id) { it.copy(status = MigrationStatus.Failed, error = null) }
            return false
        }
        val found = try {
            search.find(subject, library.origin(id)?.externalId.orEmpty())
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AppLog.error("migrate", "title $id could not be searched on ${installed.id}", error)
            MigrationFound(MigrationVerdict.NotFound, emptyList(), 0, error.message.orEmpty().ifBlank { "Error" })
        }
        val rules = mutable.value.rules
        val combined = (if (rules.compareAll) item.options else emptyList())
            .plus(found.options)
            .distinctBy { it.source.id to it.entry.url }
            .sortedByDescending { it.score.total }
            .take(5)
        val verdict = if (rules.compareAll && combined.isNotEmpty()) MigrationRanker.verdictOf(combined.map { it.score.total }, rules) else found.verdict
        val unreachable = found.failure != null && combined.isEmpty()
        setItem(id) {
            it.copy(
                status = when {
                    unreachable -> MigrationStatus.Failed
                    verdict == MigrationVerdict.Found -> MigrationStatus.Found
                    verdict == MigrationVerdict.Review -> MigrationStatus.Review
                    else -> MigrationStatus.NotFound
                },
                options = combined,
                chosen = if (verdict == MigrationVerdict.Found) combined.firstOrNull() else null,
                error = if (unreachable) found.failure else null,
                ruledOut = found.ruledOut,
                asked = it.asked + 1,
            )
        }
        countSource(installed.id) { it.copy(asked = it.asked + 1, matched = it.matched + if (found.options.isNotEmpty()) 1 else 0, failures = if (unreachable) it.failures + 1 else it.failures) }
        return unreachable
    }

    // The subject of a title, read from the database the first time it is needed. The read happens outside the lock, so the searches do not wait on one another for it.
    private suspend fun subjectOf(id: Long): MigrationSubject? {
        subjectsLock.withLock { subjects[id] }?.let { return it }
        val read = migration.subjectOf(id) ?: return null
        return subjectsLock.withLock { subjects.getOrPut(id) { read } }
    }

    private fun changeItems(change: (MigrationItem) -> MigrationItem) = mutable.update { it.copy(items = it.items.map(change)) }

    private fun setItem(id: Long, change: (MigrationItem) -> MigrationItem) = mutable.update { now -> now.copy(items = now.items.map { if (it.id == id) change(it) else it }) }

    private fun countSource(id: String, change: (SourceChoice) -> SourceChoice) = mutable.update { now -> now.copy(sources = now.sources.map { if (it.source.id == id) change(it) else it }) }

    // Chooses one of the options of a title as the place it moves to.
    fun choose(id: Long, option: MigrationOption) = setItem(id) { it.copy(status = MigrationStatus.Found, chosen = option, error = null, problem = null) }

    // Says no match is right for the title, so it is open to the next search.
    fun reject(id: Long) = setItem(id) { it.copy(status = MigrationStatus.NotFound, chosen = null, options = emptyList(), error = null, problem = null) }

    // Puts titles back in the queue, such as the ones that failed.
    fun requeue(ids: Set<Long>) = changeItems { if (it.id in ids && it.status != MigrationStatus.Moved) it.copy(status = MigrationStatus.Queued, error = null, problem = null) else it }

    // Takes the best match of every title that has matches to review.
    fun acceptBest() = changeItems { item ->
        if (item.included && item.status == MigrationStatus.Review && item.options.isNotEmpty()) item.copy(status = MigrationStatus.Found, chosen = item.options.first()) else item
    }

    // Takes the titles that were moved out of the list.
    fun clearMoved() = mutable.update { it.copy(items = it.items.filter { item -> item.status != MigrationStatus.Moved }) }

    // Searches one source by hand, for a title the search did not find.
    suspend fun manualSearch(installed: InstalledSource, query: String): SourceResult<List<SourceEntry>> = when (val opened = manager.open(installed)) {
        is SourceResult.Err -> opened
        is SourceResult.Ok -> opened.value.search(query, 1)
    }

    // Takes an entry found by hand as the place a title moves to. Its chapters are not read here, they come when the title is first opened after the move. Gives a message when it could not be taken, which is never now.
    suspend fun manualMatch(id: Long, installed: InstalledSource, entry: SourceEntry): String? {
        choose(id, MigrationOption(installed, entry, MigrationScore(1.0, 1.0, null), null))
        return null
    }

    // Moves every included title that has a chosen match, one after another.
    fun moveFound() {
        if (working) return
        stopped = false
        job = scope.launch {
            BusyWork.keep(BusyKind.Migration, ::progressNow, ::stop, ::detailNow) {
                moveAll(
                    mutable.value.items
                        .filter { it.included && it.status == MigrationStatus.Found && it.chosen != null }
                        .map { it.id },
                )
            }
        }
    }

    fun moveOne(id: Long) {
        if (working) return
        stopped = false
        job = scope.launch { BusyWork.keep(BusyKind.Migration, ::progressNow, ::stop, ::detailNow) { moveAll(listOf(id)) } }
    }

    private suspend fun moveAll(ids: List<Long>) {
        mutable.update { it.copy(moving = true, processed = 0, total = ids.size) }
        try {
            for (id in ids) {
                if (stopped) break
                move(id)
                mutable.update { it.copy(processed = it.processed + 1) }
            }
        } finally {
            mutable.update { it.copy(moving = false) }
        }
    }

    private suspend fun move(id: Long) {
        val item = mutable.value.items.firstOrNull { it.id == id } ?: return
        val choice = item.chosen ?: return
        val rules = mutable.value.rules
        // Only a title that is in the library stands in the way. One that was saved only to read a chapter of it is taken over by the move.
        val savedId = library.libraryId(choice.source.id, choice.entry)
        if (savedId != null && savedId != id && !rules.allowMerge) return refuse(id, MoveProblem.AlreadyInLibrary)
        // Nothing more is asked of the source here, which keeps a move of many titles quick. The title brings the chapters it has marks on, and its details and the source's own chapters come when it is first opened or checked.
        try {
            val newId = migration.migrate(id, choice.source.id, choice.source.kind, choice.entry, null, rules.keepOld, rules.keepShelves, rules.keepFavorite)
            AppLog.info("migrate", "title $id moved to ${choice.source.id} as title $newId")
            setItem(id) { it.copy(status = MigrationStatus.Moved, movedTo = newId, error = null, problem = null) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AppLog.error("migrate", "title $id could not be moved", error)
            setItem(id) { it.copy(status = MigrationStatus.Failed, error = error.message.orEmpty().ifBlank { "Error" }) }
        }
    }

    private fun refuse(id: Long, problem: MoveProblem, message: String? = null) = setItem(id) { it.copy(status = MigrationStatus.Failed, problem = problem, error = message) }
}
