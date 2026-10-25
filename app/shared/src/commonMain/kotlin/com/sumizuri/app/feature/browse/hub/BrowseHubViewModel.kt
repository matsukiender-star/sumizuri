package com.sumizuri.app.feature.browse.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.data.library.LibraryRepository
import com.sumizuri.app.data.local.LocalSources
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.Repo
import com.sumizuri.app.data.settings.SessionRepository
import com.sumizuri.app.data.sources.RepoRepository
import com.sumizuri.app.data.sources.SourceRepository
import com.sumizuri.app.data.sources.SourceUpdates
import com.sumizuri.app.data.text.enumNamed
import com.sumizuri.app.feature.browse.repo.RemovePrompt
import com.sumizuri.app.source.SourceManager
import com.sumizuri.app.source.repo.InstalledSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// The row of shortcuts shows once there are this many sources, or as soon as one is pinned, and holds this many used lately.
private const val JumpFrom = 6
private const val RecentMax = 6

// The keys of the groups that are not a repo.
private const val HandKey = "hand"
private const val DeviceKey = "device"

// What the person picked on Browse, kept together so the page is made from one flow.
private class Choices(
    val tab: BrowseTab,
    val kind: Kind?,
    val grouping: SourceGrouping,
    val query: String,
    val removing: RemovePrompt?,
)

class BrowseHubViewModel(
    private val sources: SourceRepository,
    repos: RepoRepository,
    private val session: SessionRepository,
    private val updates: SourceUpdates,
    private val library: LibraryRepository,
    private val manager: SourceManager,
) : ViewModel() {
    private val tab = MutableStateFlow(BrowseTab.Sources)
    private val chosenKind = MutableStateFlow<Kind?>(null)
    private val grouping = MutableStateFlow(SourceGrouping.Repo)
    private val query = MutableStateFlow("")
    private val removing = MutableStateFlow<RemovePrompt?>(null)
    private val choices =
        combine(
            tab,
            chosenKind,
            grouping,
            query,
            removing,
        ) { tab, kind, grouping, query, removing -> Choices(tab, kind, grouping, query, removing) }
    private val installedWithRepos = combine(sources.installed, repos.repos) { installed, list -> installed to list }

    val state: StateFlow<BrowseHubState> = combine(
        installedWithRepos,
        choices,
        session.browsePinned(),
        updates.state,
    ) { (installed, repoList), choice, pinned, pending ->
        val kinds = Kind.entries.filter { kind -> installed.any { it.kind == kind } }.toSet()
        // A chosen kind with no source left shows every kind again.
        val kind = choice.kind?.takeIf { it in kinds }
        val words = choice.query.trim().lowercase()
        val ofKind = installed.filter { kind == null || it.kind == kind }
        val matching = if (words.isEmpty()) {
            ofKind
        } else {
            ofKind.filter {
                words in it.name.lowercase() ||
                    words in it.baseUrl.lowercase()
            }
        }
        val on = matching.filter { it.enabled }
        val byName = compareBy<InstalledSource> { it.name.lowercase() }
        val showJump = words.isEmpty() && (on.size >= JumpFrom || on.any { it.id in pinned })
        val pinnedOn = on.filter { it.id in pinned }.sortedWith(byName)
        val recent = on.filter { it.id !in pinned && it.lastUsedAt > 0 }.sortedByDescending { it.lastUsedAt }.take(
            RecentMax,
        )
        BrowseHubState(
            loaded = true,
            tab = choice.tab,
            kinds = kinds,
            kind = kind,
            grouping = choice.grouping,
            query = choice.query,
            jump = if (showJump) pinnedOn + recent else emptyList(),
            pinned = pinned,
            groups = groupsOf(on, choice.grouping, repoList),
            off = matching.filter { !it.enabled }.sortedWith(byName),
            sourceCount = ofKind.count { it.enabled },
            updateIds = pending.updates.map { it.installedId }.toSet(),
            updating = pending.busy.isNotEmpty(),
            removing = choice.removing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowseHubState())

    init {
        viewModelScope.launch {
            chosenKind.value = session.browseKind()?.takeIf { it >= 0 }?.let { Kind.fromCode(it) }
            grouping.value = enumNamed<SourceGrouping>(session.browseGrouping()) ?: SourceGrouping.Repo
        }
    }

    fun onTab(value: BrowseTab) {
        tab.value = value
    }

    fun onKind(value: Kind?) {
        chosenKind.value = value
        viewModelScope.launch { session.setBrowseKind(value?.code ?: -1) }
    }

    fun onGrouping(value: SourceGrouping) {
        grouping.value = value
        viewModelScope.launch { session.setBrowseGrouping(value.name) }
    }

    fun onQuery(value: String) {
        query.value = value
    }

    fun onPin(source: InstalledSource) {
        viewModelScope.launch {
            val now = session.browsePinned().first()
            session.setBrowsePinned(if (source.id in now) now - source.id else now + source.id)
        }
    }

    fun onEnabled(source: InstalledSource, enabled: Boolean) {
        viewModelScope.launch {
            if (!enabled) manager.close(source.id)
            sources.setEnabled(source.id, enabled)
        }
    }

    fun onUpdateAll() {
        viewModelScope.launch { updates.updateAll() }
    }

    // A source that saved titles came from is only removed after a yes that says what happens to them.
    fun onRemove(source: InstalledSource) {
        viewModelScope.launch {
            val titles = library.countOf(source.id)
            if (titles == 0) remove(source.id) else removing.value = RemovePrompt(source.id, source.name, titles)
        }
    }

    fun onConfirmRemove() {
        val prompt = removing.value ?: return
        removing.value = null
        viewModelScope.launch { remove(prompt.id) }
    }

    fun onCancelRemove() = removing.update { null }

    private suspend fun remove(id: String) {
        manager.close(id)
        sources.remove(id)
        val pinned = session.browsePinned().first()
        if (id in pinned) session.setBrowsePinned(pinned - id)
    }

    // The headings and the sources under each, by name. By repo, the repos come in the order they were added, then a repo that was removed, then the sources added by hand, then the files on the device. By language, the biggest comes first.
    private fun groupsOf(shown: List<InstalledSource>, grouping: SourceGrouping, repos: List<Repo>): List<SourceGroup> {
        val byName = compareBy<InstalledSource> { it.name.lowercase() }
        return when (grouping) {
            SourceGrouping.None -> if (shown.isEmpty()) {
                emptyList()
            } else {
                listOf(
                    SourceGroup("all", GroupName.All, shown.sortedWith(byName)),
                )
            }

            SourceGrouping.Language ->
                shown
                    .groupBy { it.lang }
                    .map { (language, list) ->
                        SourceGroup(
                            "lang:$language",
                            GroupName.Language(language),
                            list.sortedWith(byName),
                        )
                    }.sortedWith(compareByDescending<SourceGroup> { it.sources.size }.thenBy { it.key })

            SourceGrouping.Repo -> {
                val order = repos.map { it.url }
                val names = repos.associate { it.url to it.name }
                shown
                    .groupBy { source ->
                        when {
                            LocalSources.isLocal(source.id) -> DeviceKey
                            source.repoUrl == null -> HandKey
                            else -> source.repoUrl
                        }
                    }.map { (key, list) ->
                        val name = when (key) {
                            DeviceKey -> GroupName.Device
                            HandKey -> GroupName.ByHand
                            else -> GroupName.Repo(names[key] ?: key.substringAfter("://").substringBefore('/'))
                        }
                        SourceGroup(key, name, list.sortedWith(byName))
                    }.sortedBy { group ->
                        when (group.key) {
                            DeviceKey -> Int.MAX_VALUE
                            HandKey -> Int.MAX_VALUE - 1
                            else -> order.indexOf(group.key).let { if (it < 0) order.size else it }
                        }
                    }
            }
        }
    }
}
