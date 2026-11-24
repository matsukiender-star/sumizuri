package com.sumizuri.app.feature.browse.source

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.data.db.title.TitleDao
import com.sumizuri.app.data.migration.MatchScoring
import com.sumizuri.app.data.settings.ReaderSettingsRepository
import com.sumizuri.app.data.settings.SettingsRepository
import com.sumizuri.app.source.Source
import com.sumizuri.app.source.model.FilterGroup
import com.sumizuri.app.source.model.FilterType
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.valueOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Whether Popular shows more of the genre of the last title seen in that source.
const val PicksKey = "layout.sukiPicks"

// A genre asked for from outside the page of a source, such as from a row of the library, which the page of that source opens with once.
object GenreRequest {
    private var pending: Pair<String, Map<String, Any?>>? = null

    fun ask(sourceId: String, filters: Map<String, Any?>) {
        pending = sourceId to filters
    }

    fun take(sourceId: String): Map<String, Any?>? = pending?.takeIf { it.first == sourceId }?.second?.also {
        pending =
            null
    }
}

// The settings every title starts from, which hold incognito.
private const val AnyTitle = 0L

// The words a source may head its list of genres with, as MatchScoring folds them.
private val GenreWords = listOf("genre", "genero", "жанр")

// A row of titles that share a genre with the one it is named after, and the filters that list all of them.
data class SourcePicks(
    val base: String,
    val genre: String,
    val filters: Map<String, Any?>,
    val entries: List<SourceEntry>,
)

internal data class PickBase(val name: String, val genres: List<String>)

// The recommendations of one source, read apart from its lists so a failure here never shows on them.
class SourcePicksViewModel(
    sourceId: String,
    titles: TitleDao,
    settings: SettingsRepository,
    reading: ReaderSettingsRepository,
    open: suspend () -> Source?,
) : ViewModel() {
    private val mutablePicks = MutableStateFlow<SourcePicks?>(null)
    val picks: StateFlow<SourcePicks?> = mutablePicks

    // Waits for Popular, so a site with a bot check is asked by one request and not by two at once.
    private val ready = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            combine(
                ready,
                settings.flag(PicksKey, true),
                // Incognito hides the row, since it names what was watched.
                reading.observe(AnyTitle).map { it.incognito },
                titles.observeLastSeen(sourceId).map { row ->
                    row?.let { PickBase(it.name, it.genres.split("\n")) }
                },
            ) { loaded, on, incognito, base -> base.takeIf { loaded && on && !incognito } }
                .distinctUntilChanged()
                .collectLatest { base ->
                    mutablePicks.value = null
                    if (base == null) return@collectLatest
                    // The model outlives its page, so it asks the site only while someone looks.
                    mutablePicks.subscriptionCount.first { it > 0 }
                    val source = open() ?: return@collectLatest
                    mutablePicks.value = picksFor(source, base, titles.seenKeys(sourceId).toSet())
                }
        }
    }

    fun onPopularShown() {
        ready.value = true
    }
}

internal suspend fun picksFor(source: Source, base: PickBase, seen: Set<String>): SourcePicks? {
    val tags = source
        .filters()
        .valueOrNull()
        .orEmpty()
        .filter { it.key == "tags" || it.key.startsWith("tags.") }
    // A section named for genres wins, so a word such as Series is not taken from a list of formats.
    val groups = tags.filter { group -> GenreWords.any { it in MatchScoring.normalize(group.name) } }.ifEmpty { tags }
    val (group, option) = matchGenre(base.genres, groups) ?: return null
    val filters = mapOf(group.key to if (group.type == FilterType.Select) option.value else listOf(option.value))
    val found = source.search("", 1, filters).valueOrNull().orEmpty()
    val entries = found.filter { it.url !in seen }.distinctBy { it.url }
    return if (entries.isEmpty()) null else SourcePicks(base.name, option.label, filters, entries)
}

// The first genre of the title that the source can filter by, in the order the title has them.
private fun matchGenre(genres: List<String>, groups: List<FilterGroup>) = genres.firstNotNullOfOrNull { genre ->
    val wanted = MatchScoring.normalize(genre)
    if (wanted.isEmpty()) return@firstNotNullOfOrNull null
    groups.firstNotNullOfOrNull { group ->
        group.options
            .firstOrNull { it.value.isNotEmpty() && MatchScoring.normalize(it.label) == wanted }
            ?.let { group to it }
    }
}
