package com.sumizuri.app.feature.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.data.live.LiveChannel
import com.sumizuri.app.data.live.LiveLoadResult
import com.sumizuri.app.data.live.LivePlaylist
import com.sumizuri.app.data.live.LiveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okio.Path

enum class LiveSection { All, Favorites, Recents }

data class LiveUiState(
    val playlists: List<LivePlaylist> = emptyList(),
    val selectedPlaylistId: String? = null,
    val section: LiveSection = LiveSection.All,
    val groups: List<String> = emptyList(),
    val selectedGroup: String? = null,
    val query: String = "",
    val channels: List<LiveChannel> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val loading: Boolean = false,
)

// Holds the live catalog state and filters large channel lists off the UI thread.
@OptIn(ExperimentalCoroutinesApi::class)
class LiveViewModel(private val repo: LiveRepository) : ViewModel() {
    private val selectedPlaylistId = MutableStateFlow<String?>(null)
    private val section = MutableStateFlow(LiveSection.All)
    private val selectedGroup = MutableStateFlow<String?>(null)
    private val query = MutableStateFlow("")
    private val loading = MutableStateFlow(false)

    private val loadedChannels = combine(repo.playlists, selectedPlaylistId) { lists, picked ->
        val valid = picked?.takeIf { id -> lists.any { it.id == id } }
        lists to valid
    }.flatMapLatest { (lists, validId) ->
        flow {
            val loaded = if (validId != null) repo.channelsOf(validId) else repo.allChannels(lists)
            emit(Triple(lists, validId, loaded))
        }
    }.flowOn(Dispatchers.IO)

    private val filters = combine(section, selectedGroup, query, loading) { sec, grp, q, busy ->
        FilterSpec(sec, grp, q, busy)
    }

    val state: StateFlow<LiveUiState> = combine(
        loadedChannels,
        repo.favorites,
        repo.recents,
        filters,
    ) { (lists, validId, channels), favs, recents, spec ->
        val bySection = when (spec.section) {
            LiveSection.All -> channels

            LiveSection.Favorites -> channels.filter { it.id in favs }

            LiveSection.Recents -> {
                val byId = channels.associateBy { it.id }
                recents.mapNotNull { byId[it] }
            }
        }
        val groups = bySection
            .map { it.group }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        val activeGroup = spec.group?.takeIf { it in groups }
        val q = spec.query.trim()
        val filtered = bySection.filter { ch ->
            (activeGroup == null || ch.group == activeGroup) &&
                (q.isEmpty() || ch.name.contains(q, ignoreCase = true) || ch.group.contains(q, ignoreCase = true))
        }
        LiveUiState(
            playlists = lists,
            selectedPlaylistId = validId,
            section = spec.section,
            groups = groups,
            selectedGroup = activeGroup,
            query = spec.query,
            channels = filtered,
            favorites = favs,
            loading = spec.loading,
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveUiState())

    fun selectPlaylist(id: String?) {
        selectedPlaylistId.value = id
        selectedGroup.value = null
    }

    fun selectSection(next: LiveSection) {
        section.value = next
        selectedGroup.value = null
    }

    fun selectGroup(group: String?) {
        selectedGroup.value = group
    }

    fun setQuery(text: String) {
        query.value = text
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch { repo.toggleFavorite(channelId) }
    }

    suspend fun addUrl(name: String, url: String): LiveLoadResult {
        loading.value = true
        return try {
            repo.addFromUrl(name, url)
        } finally {
            loading.value = false
        }
    }

    suspend fun addFile(name: String, path: Path): LiveLoadResult {
        loading.value = true
        return try {
            repo.addFromFile(name, path)
        } finally {
            loading.value = false
        }
    }

    suspend fun refresh(playlistId: String): LiveLoadResult {
        loading.value = true
        return try {
            repo.refresh(playlistId)
        } finally {
            loading.value = false
        }
    }

    fun rename(playlistId: String, name: String) {
        viewModelScope.launch { repo.rename(playlistId, name) }
    }

    fun remove(playlistId: String) {
        viewModelScope.launch { repo.remove(playlistId) }
    }

    private data class FilterSpec(
        val section: LiveSection,
        val group: String?,
        val query: String,
        val loading: Boolean,
    )
}
