package com.sumizuri.app.feature.browse.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.browse_all_sources
import com.sumizuri.app.shared.resources.browse_by_hand
import com.sumizuri.app.shared.resources.browse_by_language
import com.sumizuri.app.shared.resources.browse_by_repo
import com.sumizuri.app.shared.resources.browse_empty_sources
import com.sumizuri.app.shared.resources.browse_find_source
import com.sumizuri.app.shared.resources.browse_jump_back
import com.sumizuri.app.shared.resources.browse_no_groups
import com.sumizuri.app.shared.resources.browse_no_match
import com.sumizuri.app.shared.resources.browse_search_all_for
import com.sumizuri.app.shared.resources.browse_turned_off
import com.sumizuri.app.shared.resources.extensions_add_file
import com.sumizuri.app.shared.resources.extensions_add_repo
import com.sumizuri.app.shared.resources.local_on_device
import com.sumizuri.app.shared.resources.section_count
import com.sumizuri.app.shared.resources.sources_update_all
import com.sumizuri.app.shared.resources.sources_updates_notice
import com.sumizuri.app.ui.kit.card.AppCard
import com.sumizuri.app.ui.kit.card.AppListRow
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import com.sumizuri.app.ui.kit.chips.ChipRow
import com.sumizuri.app.ui.kit.chips.ChipSeparator
import com.sumizuri.app.ui.kit.chips.KindChips
import com.sumizuri.app.ui.kit.input.ActionButton
import com.sumizuri.app.ui.kit.input.AppSearchBar
import com.sumizuri.app.ui.kit.page.PageList
import com.sumizuri.app.ui.text.languageName
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// The installed sources: a field that finds one by name, the kind and the grouping as chips, a notice of updates, a row of pinned and lately used ones, then each group as a grid of tiles, and the turned off ones at the end.
@Composable
internal fun SourcesTab(state: BrowseHubState, actions: BrowseHubActions) {
    val searching = state.query.isNotBlank()
    PageList(gap = 12.dp) {
        item(key = "search") { AppSearchBar(state.query, actions.onQuery, stringResource(Res.string.browse_find_source)) }
        // The words typed can also be looked for inside every source.
        if (searching) {
            item(key = "search_all") {
                AppListRow(
                    stringResource(Res.string.browse_search_all_for, state.query.trim()),
                    leading = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { actions.onOpenSearch(state.query.trim()) },
                )
            }
        }
        item(key = "chips") { Chips(state, actions) }
        if (state.updateCount > 0 && !searching) item(key = "updates") { UpdatesBanner(state, actions) }
        if (state.loaded && state.sourceCount == 0 && state.off.isEmpty() && !searching) item(key = "none") { NoSources(actions) }
        if (searching && state.groups.isEmpty() && state.off.isEmpty()) {
            item(key = "no_match") { Text(stringResource(Res.string.browse_no_match), Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline) }
        }
        if (state.jump.isNotEmpty()) {
            item(key = "jump") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppSectionLabel(Res.string.browse_jump_back)
                    JumpRow(state.jump, state.pinned, actions.onOpenSource)
                }
            }
        }
        for (group in state.groups) {
            item(key = "group:${group.key}") {
                Column(Modifier.animateItem(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppSectionLabel(stringResource(Res.string.section_count, groupTitle(group.name), group.sources.size))
                    SourceGrid(group.sources, state, actions, showLanguage = state.grouping != SourceGrouping.Language && group.sources.mapTo(HashSet()) { it.lang }.size > 1)
                }
            }
        }
        if (state.off.isNotEmpty()) {
            item(key = "off") {
                Column(Modifier.animateItem(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppSectionLabel(stringResource(Res.string.section_count, stringResource(Res.string.browse_turned_off), state.off.size))
                    SourceGrid(state.off, state, actions, showLanguage = false)
                }
            }
        }
    }
}

// The words of a heading of the sources.
@Composable
private fun groupTitle(name: GroupName): String = when (name) {
    is GroupName.Repo -> name.name
    is GroupName.Language -> languageName(name.code)
    GroupName.ByHand -> stringResource(Res.string.browse_by_hand)
    GroupName.Device -> stringResource(Res.string.local_on_device)
    GroupName.All -> stringResource(Res.string.browse_all_sources)
}

// The kinds as chips when there is more than one, then one chip that says how the sources are grouped and opens the choices.
@Composable
private fun Chips(state: BrowseHubState, actions: BrowseHubActions) {
    ChipRow {
        if (state.kinds.size > 1) {
            KindChips(state.kind, state.kinds, actions.onKind)
            ChipSeparator()
        }
        GroupChip(state.grouping, actions.onGrouping)
    }
}

@Composable
private fun GroupChip(grouping: SourceGrouping, onGrouping: (SourceGrouping) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { open = true },
            label = { Text(groupingLabel(grouping)) },
            leadingIcon = { Icon(groupingIcon(grouping), null, Modifier.size(18.dp)) },
            trailingIcon = { Icon(Icons.Outlined.ArrowDropDown, null, Modifier.size(18.dp)) },
        )
        DropdownMenu(open, { open = false }) {
            for (each in SourceGrouping.entries) {
                DropdownMenuItem(
                    text = { Text(groupingLabel(each)) },
                    leadingIcon = { Icon(groupingIcon(each), null, Modifier.size(20.dp)) },
                    trailingIcon = { if (each == grouping) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) },
                    onClick = {
                        open = false
                        onGrouping(each)
                    },
                )
            }
        }
    }
}

@Composable
private fun groupingLabel(grouping: SourceGrouping): String = stringResource(
    when (grouping) {
        SourceGrouping.Repo -> Res.string.browse_by_repo
        SourceGrouping.Language -> Res.string.browse_by_language
        SourceGrouping.None -> Res.string.browse_no_groups
    },
)

private fun groupingIcon(grouping: SourceGrouping) = when (grouping) {
    SourceGrouping.Repo -> Icons.Outlined.Storefront
    SourceGrouping.Language -> Icons.Outlined.Translate
    SourceGrouping.None -> Icons.AutoMirrored.Outlined.ViewList
}

// How many sources have a newer version, with a button that updates them all here.
@Composable
private fun UpdatesBanner(state: BrowseHubState, actions: BrowseHubActions) {
    AppCard(Modifier.fillMaxWidth(), onClick = { actions.onTab(BrowseTab.Extensions) }) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.SystemUpdateAlt, null, tint = MaterialTheme.colorScheme.primary)
            Text(pluralStringResource(Res.plurals.sources_updates_notice, state.updateCount, state.updateCount), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            TextButton(actions.onUpdateAll, enabled = !state.updating) { Text(stringResource(Res.string.sources_update_all)) }
        }
    }
}

// With no source yet, the ways to get one.
@Composable
private fun NoSources(actions: BrowseHubActions) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.browse_empty_sources), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(Res.string.extensions_add_repo, actions.onAddRepo)
            ActionButton(Res.string.extensions_add_file, actions.onAddFile)
        }
    }
}
