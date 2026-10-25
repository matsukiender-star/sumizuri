package com.sumizuri.app.data.sync

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.db.title.TitleKey
import com.sumizuri.app.data.db.title.TitleOrigin
import com.sumizuri.app.data.download.AutoDownloads
import com.sumizuri.app.data.library.ChapterCarry
import com.sumizuri.app.data.library.LibraryRepository
import com.sumizuri.app.data.settings.SettingKey
import com.sumizuri.app.data.settings.SettingsRepository
import com.sumizuri.app.data.sources.SourceRepository
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.data.work.BusyKind
import com.sumizuri.app.data.work.BusyWork
import com.sumizuri.app.source.SourceError
import com.sumizuri.app.source.SourceManager
import com.sumizuri.app.source.SourceResult
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.repo.InstalledSource
import com.sumizuri.app.source.valueOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlin.concurrent.Volatile

// How many titles are asked at once in all, and at most from one source.
private const val AtOnce = 6
private const val PerSource = 2

// The pause before a title that failed for a passing reason is asked again.
private const val RetryGapMs = 1_500L
private const val HourMs = 60 * 60 * 1000L

// How long a check waits for the connection to come back before it stops, and how often it tries again meanwhile.
private const val OfflineWaitMs = 60_000L
private const val OfflineProbeMs = 5_000L

// How many different sources must fail in a row with no connection before the device is taken to be offline. One site whose name no longer resolves is not enough.
private const val OfflineSources = 2

// How many titles of one source in a row may fail because its site cannot be found, while the device is online, before the rest of its titles are left out of the check. Such a site is down, gone or blocked on this network, and asking each of its titles only waits for the same failure.
private const val MissingAfter = 3

// The words of a failure when the device could not find any site at all, as opposed to one site answering badly.
private val NoConnectionWords = listOf(
    "Unable to resolve host",
    "No address associated with hostname",
    "UnknownHostException",
    "ERR_NAME_NOT_RESOLVED",
    "ERR_INTERNET_DISCONNECTED",
    "ERR_NETWORK_CHANGED",
    "ERR_ADDRESS_UNREACHABLE",
    "Network is unreachable",
)

// True when the failure looks like the device has no connection.
private fun SourceError.looksOffline(): Boolean = NoConnectionWords.any { message.contains(it, ignoreCase = true) }

// Asks a title's source again, so new chapters and changed details reach a saved title.
class TitleSync(
    private val library: LibraryRepository,
    private val sources: SourceRepository,
    private val manager: SourceManager,
    private val settings: SettingsRepository,
    private val carry: ChapterCarry,
    private val auto: AutoDownloads,
) {
    // How far a check of the whole library has got, or null when none is running.
    private val mutableProgress = MutableStateFlow<UpdateProgress?>(null)
    val progress: StateFlow<UpdateProgress?> = mutableProgress

    // The titles the last check could not ask, with what went wrong, until the person dismisses them.
    private val mutableFailures = MutableStateFlow<List<UpdateFailure>>(emptyList())
    val failures: StateFlow<List<UpdateFailure>> = mutableFailures

    // How many titles the last check left unchecked because the connection was lost and did not come back, zero when it was not.
    private val mutableUnchecked = MutableStateFlow(0)
    val unchecked: StateFlow<Int> = mutableUnchecked

    fun clearFailures() {
        mutableFailures.value = emptyList()
        mutableUnchecked.value = 0
    }

    // The titles that got new chapters since the check of the library began, with how many, for the notice after it.
    private val news = MutableStateFlow<Map<Long, Int>>(emptyMap())

    // What the last check found, the titles with the most new chapters first.
    suspend fun lastNews(): List<ChapterNews> = news.value.map { (id, added) -> ChapterNews(id, library.nameOf(id), added, library.coverOf(id)) }.sortedByDescending { it.added }

    // When each title was last asked with success, so opening it again soon does not ask the source again.
    private val freshAt = MutableStateFlow<Map<Long, Long>>(emptyMap())

    fun isFresh(titleId: Long, withinMs: Long): Boolean = freshAt.value[titleId]?.let { nowMillis() - it < withinMs } == true

    // Gives what went wrong, and null when it worked or the source is not installed or is switched off. Asked for one title, the details are always read again.
    suspend fun refresh(titleId: Long): SourceError? = refresh(titleId, sources.installed.first().associateBy { it.id }, detailsAlways = true)

    // The same with the installed sources read once for a whole check. During a check the details are only asked for when new chapters came in or the title still lacks a cover or a description, since they rarely change otherwise, which saves one request for most titles.
    private suspend fun refresh(titleId: Long, installedById: Map<String, InstalledSource>, detailsAlways: Boolean, known: TitleOrigin? = null): SourceError? {
        val origin = known ?: library.origin(titleId) ?: return null
        val installed = installedById[origin.sourceId]?.takeIf { it.enabled } ?: return null
        val source = when (val opened = manager.open(installed)) {
            is SourceResult.Err -> return opened.error
            is SourceResult.Ok -> opened.value
        }
        val listed = SourceEntry(url = origin.externalId, title = "")
        val chapters = when (val got = source.chapters(listed)) {
            is SourceResult.Err -> return got.error
            is SourceResult.Ok -> got.value
        }
        // The chapters a title brought from a backup or another source give way to the ones the source lists, with their marks moved over.
        val settling = carry.waiting(titleId)
        val added = library.addChapters(titleId, chapters).also { carry.settle(titleId, chapters) }
        if (added.isNotEmpty()) news.update { it + (titleId to (it[titleId] ?: 0) + added.size) }
        // Chapters that take the place of brought ones, and the first list of a title, are not new chapters to save.
        if (added.isNotEmpty() && !settling && added.size < chapters.size) auto.onNewChapters(titleId, added)
        if (detailsAlways || added.isNotEmpty() || library.lacksDetails(titleId)) {
            source.details(listed).valueOrNull()?.let { library.applyDetails(titleId, it) }
        }
        freshAt.update { it + (titleId to nowMillis()) }
        return null
    }

    // Set while a check runs, to ask it to stop. The titles being asked finish, and no new one is started. It is set from other threads, such as the button of the notice.
    @Volatile
    private var cancelled = false

    fun cancelRefresh() {
        cancelled = true
    }

    // True while a check that was asked to stop finishes the titles it was asking.
    val stopping: Boolean get() = cancelled && mutableProgress.value != null

    // The sources that failed with no connection since the last title that worked.
    private val offlineStreak = MutableStateFlow<Set<String>>(emptySet())

    // How many titles of each source failed in a row because its site could not be found, and the sources left out of the check for it, with the failure that showed it.
    private val missingStreak = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val missing = MutableStateFlow<Map<String, SourceError>>(emptyMap())

    // When the connection was found to be lost, or zero while it is fine.
    private val lostAt = MutableStateFlow(0L)

    // Set once the connection stayed away too long, which ends the check.
    private var gaveUp = false

    // Only one title at a time tries the connection again while it is lost. The others wait for its answer.
    private val probe = Mutex()

    // Notes a failure with no connection, and says whether the device now counts as offline.
    private fun noteOffline(sourceId: String): Boolean {
        offlineStreak.update { it + sourceId }
        if (lostAt.value == 0L && offlineStreak.value.size >= OfflineSources) {
            lostAt.value = nowMillis()
            AppLog.warn("updates", "the connection looks lost, waiting up to ${OfflineWaitMs / 1000} s for it to come back")
        }
        return lostAt.value != 0L
    }

    // Notes a source whose site could not be found while other sites answer. Once it happens a few times in a row, the rest of its titles are left out of the check with the same failure.
    private fun noteMissing(sourceId: String, name: String, problem: SourceError) {
        val count = missingStreak.updateAndGet { it + (sourceId to (it[sourceId] ?: 0) + 1) }.getValue(sourceId)
        if (count < MissingAfter || missing.getAndUpdate { it + (sourceId to problem) }[sourceId] != null) return
        AppLog.warn("updates", "$name could not be found $count times in a row while other sites answer, its other titles are left out of this check")
    }

    // The connection works: a title got through. After a loss, what was counted against the sites while it lasted is let go too.
    private fun connectionBack() {
        offlineStreak.value = emptySet()
        val lost = lostAt.value
        if (lost == 0L) return
        lostAt.value = 0L
        missingStreak.value = emptyMap()
        missing.value = emptyMap()
        AppLog.info("updates", "the connection is back after ${(nowMillis() - lost) / 1000} s")
    }

    // Waits for the connection by asking the title again every few seconds. Gives true once it works, or once another title got through meanwhile, since a site that is down or blocked fails the same way as a lost connection. Gives false when it stayed away too long, which stops the check.
    private suspend fun waitForConnection(titleId: Long): Boolean = probe.withLock {
        if (gaveUp) return@withLock false
        while (true) {
            val lost = lostAt.value
            if (lost == 0L) return@withLock true
            if (nowMillis() - lost >= OfflineWaitMs) break
            delay(OfflineProbeMs)
            if (cancelled) return@withLock false
            if (lostAt.value != 0L && refresh(titleId, sources.installed.first().associateBy { it.id }, detailsAlways = false)?.looksOffline() != true) {
                connectionBack()
                return@withLock true
            }
        }
        AppLog.warn("updates", "the connection did not come back, stopping the check")
        gaveUp = true
        cancelled = true
        false
    }

    // Asks one title during a check. A failure with no connection waits for the connection and asks again, so a dropped connection does not turn every title left into a failure. Gives what went wrong, or null when it worked or was left unchecked.
    private suspend fun checkOne(id: Long, installedById: Map<String, InstalledSource>, origin: TitleOrigin?): SourceError? {
        val problem = refresh(id, installedById, detailsAlways = false, origin) ?: run {
            connectionBack()
            origin?.let { known -> missingStreak.update { it - known.sourceId } }
            return null
        }
        if (!problem.looksOffline()) return problem
        val sourceId = origin?.sourceId ?: library.origin(id)?.sourceId ?: return problem
        if (!noteOffline(sourceId)) {
            noteMissing(sourceId, installedById[sourceId]?.name ?: sourceId, problem)
            return problem
        }
        if (!waitForConnection(id)) return null
        return refresh(id, installedById, detailsAlways = false, origin)
    }

    // Checks the titles in the library, or only the ones given, a few at a time, and gives how many could not be reached. Each source gets a few places of its own, so one slow or dead site cannot hold all of them, and the titles are taken from the sources in turn so every source starts early. Titles that failed for a passing reason are asked once more at the end. keepAwake keeps the app going in the background with a notice while the check runs. The check run by the system while the app is closed has a notice of its own.
    suspend fun refreshLibrary(only: Set<Long>? = null, keepAwake: Boolean = true): Int {
        if (mutableProgress.value != null) return 0
        // A title whose source is gone or switched off is left alone, so it does not count as a failure.
        val installedById = sources.installed.first().associateBy { it.id }
        val ready = installedById.values
            .filter { it.enabled }
            .map { it.id }
            .toSet()
        val keys = library.libraryKeys()
        val skipCompleted = settings.flag(SettingKey.UpdateSkipCompleted, false).first()
        val skipNotStarted = settings.flag(SettingKey.UpdateSkipNotStarted, false).first()
        val skipUnread = settings.flag(SettingKey.UpdateSkipUnread, false).first()
        val smart = settings.flag(SettingKey.UpdateSmart, true).first()
        // Smart update leaves out the titles not due, but a check of chosen titles asks every one of them.
        val notDue = if (smart && only == null) library.notDueForUpdate() else emptySet()
        val left = library.excludedFromUpdate() + library.leftOutOfUpdate(skipCompleted, skipNotStarted, skipUnread) + notDue
        val chosen = keys.filter { it.sourceId in ready && it.id !in left && (only == null || it.id in only) }
        if (keys.size > chosen.size) AppLog.info("updates", "${keys.size - chosen.size} titles skipped, their source is not installed or is off, or their shelf is left out, or smart update says they are not due")
        if (chosen.isEmpty()) return 0
        val ids = takenInTurn(chosen)
        val sourceOf = chosen.associate { it.id to it.sourceId }
        // Where each title is on its source, already read with the list, so no title is looked up again.
        val originOf = chosen.associate { it.id to TitleOrigin(it.sourceId, it.externalId) }
        cancelled = false
        gaveUp = false
        lostAt.value = 0L
        offlineStreak.value = emptySet()
        missingStreak.value = emptyMap()
        missing.value = emptyMap()
        mutableFailures.value = emptyList()
        news.value = emptyMap()
        mutableUnchecked.value = 0
        mutableProgress.value = UpdateProgress(0, ids.size)
        AppLog.info("updates", "checking ${ids.size} titles from ${sourceOf.values.toSet().size} sources")
        val checked = MutableStateFlow(0)
        val problems = MutableStateFlow<Map<Long, SourceError>>(emptyMap())
        val busy = if (keepAwake) BusyWork.begin(BusyKind.Library, { mutableProgress.value?.let { it.done to it.total } }, ::cancelRefresh) else null
        try {
            val gate = Semaphore(AtOnce)
            val lanes = sourceOf.values.toSet().associateWith { Semaphore(PerSource) }
            coroutineScope {
                ids
                    .map { id ->
                        async {
                            lanes.getValue(sourceOf.getValue(id)).withPermit {
                                gate.withPermit {
                                    if (cancelled) return@withPermit
                                    // A source whose site could not be found a few times in a row fails its other titles at once.
                                    val problem = missing.value[sourceOf.getValue(id)] ?: checkOne(id, installedById, originOf[id])
                                    if (gaveUp) return@withPermit
                                    checked.update { it + 1 }
                                    if (problem != null) problems.update { it + (id to problem) }
                                    mutableProgress.update { it?.copy(done = it.done + 1) }
                                }
                            }
                        }
                    }.awaitAll()
            }
            // A title that timed out or was told to come back later often works a moment after, so those are asked once more, one at a time.
            if (!cancelled) {
                val again = problems.value.filterValues { it.isPassing() }.keys
                if (again.isNotEmpty()) AppLog.info("updates", "asking ${again.size} titles again that failed for a passing reason")
                for (id in again) {
                    if (cancelled) break
                    delay(RetryGapMs)
                    if (refresh(id, installedById, detailsAlways = false, originOf[id]) == null) problems.update { it - id }
                }
            }
        } finally {
            mutableProgress.value = null
            busy?.end()
        }
        val failed = problems.value
        mutableFailures.value = failed.map { (id, problem) ->
            AppLog.warn("updates", "title $id could not be checked, ${problem.message}")
            UpdateFailure(id, library.nameOf(id), problem.message, installedById[sourceOf[id]]?.name.orEmpty())
        }
        if (gaveUp) {
            mutableUnchecked.value = ids.size - checked.value
            AppLog.warn("updates", "stopped with no connection, ${mutableUnchecked.value} titles not checked")
        } else {
            settings.setLastUpdateCheck(nowMillis())
        }
        AppLog.info("updates", "checked ${checked.value} of ${ids.size} titles, ${failed.size} could not be reached")
        return failed.size
    }

    // The check that runs on its own, as often as the person chose, which is every hour until they change it. Zero hours means only when asked. The slack lets a check planned by the system run a little before the hour is full. Says whether a check ran.
    suspend fun refreshLibraryIfDue(slackMs: Long = 0): Boolean {
        if (!isDue(slackMs)) return false
        refreshLibrary()
        return true
    }

    // Whether the check that runs on its own should run now: it is not off, none is running, and the last one is as old as the interval.
    suspend fun isDue(slackMs: Long = 0): Boolean {
        val hours = settings.number(SettingKey.UpdateHours, SettingKey.DefaultUpdateHours).first()
        return hours > 0 && mutableProgress.value == null && nowMillis() - settings.lastUpdateCheck() >= hours * HourMs - slackMs
    }
}

// The titles in an order that takes one from each source in turn, so a source with many titles does not keep the others waiting.
private fun takenInTurn(keys: List<TitleKey>): List<Long> {
    val queues = keys.groupBy { it.sourceId }.values.map { ArrayDeque(it) }
    val out = ArrayList<Long>(keys.size)
    while (out.size < keys.size) for (queue in queues) queue.removeFirstOrNull()?.let { out += it.id }
    return out
}

// The words of a failure that may well pass if asked again a moment later.
private val PassingWords = listOf("timed out", "did not answer", "Timeout", "429", "Too Many Requests", "502", "503", "504", "ERR_TIMED_OUT", "Connection reset", "ERR_CONNECTION_RESET")

private fun SourceError.isPassing(): Boolean = !looksOffline() && PassingWords.any { message.contains(it, ignoreCase = true) }
