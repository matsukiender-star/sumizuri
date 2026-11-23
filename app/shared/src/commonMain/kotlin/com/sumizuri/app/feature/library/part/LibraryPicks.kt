package com.sumizuri.app.feature.library.part

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.data.db.title.TitleDao
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.settings.ReaderSettingsRepository
import com.sumizuri.app.data.settings.SettingsRepository
import com.sumizuri.app.feature.browse.asTitle
import com.sumizuri.app.feature.browse.source.PickBase
import com.sumizuri.app.feature.browse.source.SourcePicks
import com.sumizuri.app.feature.browse.source.picksFor
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.display_hide_row
import com.sumizuri.app.shared.resources.suki_library_picks_read
import com.sumizuri.app.shared.resources.suki_library_picks_watched
import com.sumizuri.app.source.Source
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.repo.InstalledSource
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import com.sumizuri.app.ui.kit.card.ListChevron
import com.sumizuri.app.ui.kit.cover.CoverTile
import com.sumizuri.app.ui.kit.scroll.ArrowRow
import com.sumizuri.app.ui.kit.scroll.mouseDragScroll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// Whether the library takes recommendations only from the pinned sources, and not also from those used lately.
const val PicksOnlyPinnedKey = "layout.sukiPicksOnlyPinned"

// The settings every title starts from, which hold incognito.
private const val AnyTitle = 0L

private val PickWidth = 116.dp

// The sources the library takes recommendations from, in the order it asks them: the pinned ones, then those used lately unless only the pinned are wanted, the newest first in each. A source that is off, never opened, or with nothing seen that has genres is left out.
fun picksSources(
    installed: List<InstalledSource>,
    pinned: Set<String>,
    onlyPinned: Boolean,
    genresSeen: Map<String, List<String>>,
): List<InstalledSource> {
    val usable = installed.filter { source ->
        source.enabled && genresSeen[source.id].orEmpty().any { it.isNotBlank() }
    }
    val first = usable.filter { it.id in pinned }.sortedByDescending { it.lastUsedAt }
    val then = if (onlyPinned) {
        emptyList()
    } else {
        usable
            .filter {
                it.id !in pinned && it.lastUsedAt > 0
            }.sortedByDescending { it.lastUsedAt }
    }
    return first + then
}

// One row of recommendations in the library: the source it comes from and what it found there.
data class LibraryPick(val source: InstalledSource, val picks: SourcePicks)

// What the rows hang on: each source asked in order with the title its row is named after, and how many rows are wanted.
private data class PickPlan(val order: List<Pair<InstalledSource, PickBase>>, val rows: Int)

// The rows of recommendations of the library. It asks one source at a time until the rows are full, and only while the library is in sight.
class LibraryPicksModel(
    installed: Flow<List<InstalledSource>>,
    pinned: Flow<Set<String>>,
    rows: Flow<Int>,
    private val titles: TitleDao,
    settings: SettingsRepository,
    reading: ReaderSettingsRepository,
    private val open: suspend (InstalledSource) -> Source?,
) : ViewModel() {
    private val mutablePicks = MutableStateFlow<List<LibraryPick>>(emptyList())
    val picks: StateFlow<List<LibraryPick>> = mutablePicks

    init {
        viewModelScope.launch {
            combine(
                installed,
                pinned,
                settings.flag(PicksOnlyPinnedKey, false),
                rows,
                // Incognito hides the rows, since they name what was watched.
                reading.observe(AnyTitle).map { it.incognito },
            ) { list, pins, onlyPinned, wanted, incognito ->
                if (incognito || wanted <= 0) return@combine PickPlan(emptyList(), 0)
                val bases = list
                    .filter { it.enabled }
                    .mapNotNull { source ->
                        titles.observeLastSeen(source.id).first()?.let {
                            source.id to
                                PickBase(it.name, it.genres.split("\n"))
                        }
                    }.toMap()
                val order = picksSources(list, pins, onlyPinned, bases.mapValues { it.value.genres })
                PickPlan(order.map { it to bases.getValue(it.id) }, wanted)
            }.distinctUntilChanged { a, b ->
                a.rows == b.rows &&
                    a.order.map { it.first.id to it.second } == b.order.map { it.first.id to it.second }
            }.collectLatest { plan ->
                mutablePicks.value = emptyList()
                if (plan.rows <= 0) return@collectLatest
                // The model outlives the page, so it asks the sites only while someone looks.
                mutablePicks.subscriptionCount.first { it > 0 }
                val found = ArrayList<LibraryPick>()
                for ((installedSource, base) in plan.order) {
                    if (found.size >= plan.rows) break
                    val row = try {
                        val source = open(installedSource) ?: continue
                        picksFor(source, base, titles.seenKeys(installedSource.id).toSet())
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        null
                    } ?: continue
                    found += LibraryPick(installedSource, row)
                    mutablePicks.value = found.toList()
                }
            }
        }
    }
}

// A row of recommendations under a heading that names the title and the source it comes from, which opens all of that genre in the source.
@Composable
internal fun LibraryPicksRow(
    pick: LibraryPick,
    onOpenEntry: (InstalledSource, SourceEntry) -> Unit,
    onOpenGenre: (InstalledSource, Map<String, Any?>) -> Unit,
    // The cross that hides every row, which only the first one shows.
    onHide: (() -> Unit)?,
) {
    val source = pick.source
    val because = if (source.kind ==
        Kind.Anime
    ) {
        Res.string.suki_library_picks_watched
    } else {
        Res.string.suki_library_picks_read
    }
    val row = rememberLazyListState()
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { onOpenGenre(source, pick.picks.filters) },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppSectionLabel(
                stringResource(because, pick.picks.base, source.name, pick.picks.genre),
                Modifier.weight(1f),
            )
            ListChevron()
            if (onHide != null) {
                IconButton(onHide, Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Close, stringResource(Res.string.display_hide_row), Modifier.size(18.dp))
                }
            }
        }
        ArrowRow(row) {
            LazyRow(
                Modifier.mouseDragScroll(row, horizontal = true),
                state = row,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(pick.picks.entries, key = { it.url }) { entry ->
                    CoverTile(entry.asTitle(source.kind, source.id), onClick = {
                        onOpenEntry(source, entry)
                    }, modifier = Modifier.width(PickWidth))
                }
            }
        }
    }
}
