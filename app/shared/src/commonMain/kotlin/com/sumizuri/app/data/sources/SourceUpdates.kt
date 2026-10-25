package com.sumizuri.app.data.sources

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.settings.SettingsRepository
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.data.work.BusyKind
import com.sumizuri.app.data.work.BusyWork
import com.sumizuri.app.source.SourceResult
import com.sumizuri.app.source.repo.RepoClient
import com.sumizuri.app.source.repo.RepoSource
import com.sumizuri.app.source.repo.originId
import com.sumizuri.app.source.repo.updatesAvailable
import com.sumizuri.app.source.valueOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

// The repos are asked at most this often when the app opens, in milliseconds.
private const val CheckEveryMs = 6 * 60 * 60 * 1000L

// Finds the installed sources that have a newer version in the repo they came from, and updates one of them or all of them.
class SourceUpdates(
    private val sources: SourceRepository,
    private val repos: RepoRepository,
    private val client: RepoClient,
    private val settings: SettingsRepository,
) {
    private val found = MutableStateFlow<List<SourceUpdate>>(emptyList())
    private val checking = MutableStateFlow(false)
    private val busy = MutableStateFlow<Set<String>>(emptySet())
    private val problem = MutableStateFlow<String?>(null)
    private val checked = MutableStateFlow(false)

    // An update leaves the list as soon as the installed version has caught up, whoever installed it.
    private val progress = combine(checking, busy, problem, checked) { now, working, trouble, done ->
        UpdatesState(emptyList(), now, working, trouble, done)
    }

    val state: Flow<UpdatesState> = combine(found, sources.installed, progress) { list, installed, base ->
        val current = list.filter { update ->
            installed.firstOrNull { it.id == update.installedId }?.let { it.version < update.available.version } == true
        }
        base.copy(updates = current)
    }

    // Asks every repo that was added about the sources installed from it.
    suspend fun check() {
        if (checking.value) return
        checking.value = true
        problem.value = null
        try {
            val installed = sources.installed.first()
            val list = mutableListOf<SourceUpdate>()
            val unanswered = mutableListOf<String>()
            for (repo in repos.repos.first()) {
                val index = client.fetchIndex(repo.url).valueOrNull()
                if (index == null) {
                    // A repo that does not answer keeps what was found in it before, and does not count as checked.
                    unanswered += repo.url
                    list += found.value.filter { it.repoUrl == repo.url }
                    continue
                }
                for (newer in updatesAvailable(installed, index, repo.url)) {
                    val have = installed.firstOrNull { it.repoUrl == repo.url && it.originId == newer.id } ?: continue
                    list += SourceUpdate(have.id, have.name, repo.url, have.version, newer)
                }
            }
            found.value = list
            checked.value = true
            if (unanswered.isEmpty()) {
                settings.setLastSourceCheck(nowMillis())
                AppLog.info("updates", "source check done, ${list.size} updates")
            } else {
                // Not marked as done, so the check is tried again soon instead of in six hours.
                AppLog.warn(
                    "updates",
                    "source check could not reach ${unanswered.size} repos, will try again: ${unanswered.joinToString()}",
                )
            }
        } finally {
            checking.value = false
        }
    }

    // The check that runs when the app opens.
    suspend fun checkIfDue() {
        if (nowMillis() - settings.lastSourceCheck() >= CheckEveryMs) check()
    }

    suspend fun update(update: SourceUpdate) {
        if (update.installedId in busy.value) return
        busy.update { it + update.installedId }
        try {
            when (val result = client.fetchSource(update.available, update.repoUrl)) {
                is SourceResult.Err -> problem.value = "${update.name}: ${result.error.message}"
                is SourceResult.Ok -> sources.save(result.value.copy(id = update.installedId))
            }
        } finally {
            busy.update { it - update.installedId }
        }
    }

    // Updates every source that has a newer version, one after another, going on while the app is out of sight. Stop leaves the rest as they are.
    suspend fun updateAll() {
        problem.value = null
        val all = found.value
        var done = 0
        var stopped = false
        BusyWork.keep(BusyKind.Sources, { done to all.size }, { stopped = true }) {
            for (update in all) {
                if (stopped) break
                update(update)
                done++
            }
        }
    }
}

// A source that is installed, and the newer version its repo lists.
data class SourceUpdate(
    val installedId: String,
    val name: String,
    val repoUrl: String,
    val have: Int,
    val available: RepoSource,
)

// What the screens show about updates. Checked is true once a check has finished, so "up to date" is not said before anyone looked.
data class UpdatesState(
    val updates: List<SourceUpdate> = emptyList(),
    val checking: Boolean = false,
    val busy: Set<String> = emptySet(),
    val problem: String? = null,
    val checked: Boolean = false,
)
