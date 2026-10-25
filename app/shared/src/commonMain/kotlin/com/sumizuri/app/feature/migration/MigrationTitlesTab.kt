package com.sumizuri.app.feature.migration

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.migration.MigrationItem
import com.sumizuri.app.data.migration.MigrationSession
import com.sumizuri.app.data.migration.MigrationState
import com.sumizuri.app.data.migration.MigrationStatus
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.SourceState
import com.sumizuri.app.data.model.library.Category
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.all
import com.sumizuri.app.shared.resources.mig_exclude_shown
import com.sumizuri.app.shared.resources.mig_filter_from
import com.sumizuri.app.shared.resources.mig_filter_shelf
import com.sumizuri.app.shared.resources.mig_filter_title
import com.sumizuri.app.shared.resources.mig_include_shown
import com.sumizuri.app.shared.resources.mig_invert_shown
import com.sumizuri.app.shared.resources.mig_only_lost
import com.sumizuri.app.shared.resources.mig_show_all
import com.sumizuri.app.shared.resources.mig_show_excluded
import com.sumizuri.app.shared.resources.mig_show_included
import com.sumizuri.app.shared.resources.mig_titles_count
import com.sumizuri.app.shared.resources.mig_unread
import com.sumizuri.app.ui.kit.cover.CoverArt
import com.sumizuri.app.ui.kit.input.AppSearchBar
import com.sumizuri.app.ui.kit.input.MenuButton
import com.sumizuri.app.ui.kit.page.PageList
import com.sumizuri.app.ui.text.displayName
import org.jetbrains.compose.resources.stringResource

// Which titles are in the work. They are narrowed by a word, kind, source and shelf, and a tick puts one in or takes it out. The buttons put in or take out every title that is shown, turn the ticks of those around, or keep only the titles whose source is lost.
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun MigrationTitlesTab(state: MigrationState, shelves: List<Category>, session: MigrationSession) {
    var filter by remember { mutableStateOf(TitleFilter()) }
    val shown = remember(state.items, filter) { state.items.filter { filter.matches(it) } }
    val kinds = remember(state.items) { state.items.map { it.title.kind }.distinct() }
    val sources = remember(state.items) {
        state.items
            .groupingBy { it.fromName }
            .eachCount()
            .entries
            .sortedBy { it.key.lowercase() }
    }
    val usedShelves = remember(state.items, shelves) { shelves.filter { shelf -> state.items.any { shelf.id in it.shelves } } }
    val everything = stringResource(Res.string.all)
    PageList {
        item { AppSearchBar(filter.query, { filter = filter.copy(query = it) }, stringResource(Res.string.mig_filter_title)) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (kinds.size > 1) MenuButton(listOf<Kind?>(null) + kinds, filter.kind, { it?.let { kind -> stringResource(kind.label) } ?: everything }) { filter = filter.copy(kind = it) }
                MenuButton(listOf<String?>(null) + sources.map { it.key }, filter.from, { name -> name?.let { "$it (${sources.firstOrNull { entry -> entry.key == name }?.value ?: 0})" } ?: stringResource(Res.string.mig_filter_from) }) { filter = filter.copy(from = it) }
                if (usedShelves.isNotEmpty()) MenuButton(listOf<Long?>(null) + usedShelves.map { it.id }, filter.shelf, { id -> usedShelves.firstOrNull { it.id == id }?.name ?: stringResource(Res.string.mig_filter_shelf) }) { filter = filter.copy(shelf = it) }
                MenuButton(TitleFilter.Show.entries, filter.show, { stringResource(showLabel(it)) }) { filter = filter.copy(show = it) }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(Res.string.mig_titles_count, state.items.count { it.included }, state.items.size, shown.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val ids = shown.map { it.id }.toSet()
                    TextButton({ session.setIncluded(ids, true) }) { Text(stringResource(Res.string.mig_include_shown)) }
                    TextButton({ session.setIncluded(ids, false) }) { Text(stringResource(Res.string.mig_exclude_shown)) }
                    TextButton({
                        val on = shown.filter { !it.included }.map { it.id }.toSet()
                        session.setIncluded(ids - on, false)
                        session.setIncluded(on, true)
                    }) { Text(stringResource(Res.string.mig_invert_shown)) }
                    TextButton({
                        val lost = state.items
                            .filter { it.title.sourceState != SourceState.Ready }
                            .map { it.id }
                            .toSet()
                        session.setIncluded(state.items.map { it.id }.toSet() - lost, false)
                        session.setIncluded(lost, true)
                    }) { Text(stringResource(Res.string.mig_only_lost)) }
                }
            }
        }
        items(shown, key = { it.id }) { item -> TitleRow(item) { session.setIncluded(setOf(item.id), !item.included) } }
    }
}

private fun showLabel(show: TitleFilter.Show) = when (show) {
    TitleFilter.Show.All -> Res.string.mig_show_all
    TitleFilter.Show.Included -> Res.string.mig_show_included
    TitleFilter.Show.Excluded -> Res.string.mig_show_excluded
}

// One title with a tick, a small cover, its name, where it comes from and how much is unread, and its state when the work has touched it.
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TitleRow(item: MigrationItem, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onToggle), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Checkbox(item.included, null)
        CoverArt(item.title, Modifier.width(32.dp), letterSize = 12)
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(item.title.displayName(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val unread = if (item.title.unread > 0) " · " + stringResource(Res.string.mig_unread, item.title.unread) else ""
            Text(item.fromName + unread, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (item.status != MigrationStatus.Queued) Text(stringResource(item.status.label()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}
