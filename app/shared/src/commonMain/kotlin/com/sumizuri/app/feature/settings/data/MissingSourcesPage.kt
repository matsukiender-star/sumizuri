package com.sumizuri.app.feature.settings.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.backup.ImportedSources
import com.sumizuri.app.data.model.SourceState
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.feature.settings.SettingsPageFrame
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.missing_detect
import com.sumizuri.app.shared.resources.missing_detect_done
import com.sumizuri.app.shared.resources.missing_detect_hint
import com.sumizuri.app.shared.resources.missing_detect_running
import com.sumizuri.app.shared.resources.missing_filter_hint
import com.sumizuri.app.shared.resources.missing_migrate_all
import com.sumizuri.app.shared.resources.missing_migrate_group
import com.sumizuri.app.shared.resources.missing_show_all
import com.sumizuri.app.shared.resources.missing_sources_none
import com.sumizuri.app.shared.resources.missing_summary_body
import com.sumizuri.app.shared.resources.missing_summary_title
import com.sumizuri.app.shared.resources.settings_missing_sources
import com.sumizuri.app.shared.resources.settings_missing_sources_hint
import com.sumizuri.app.shared.resources.source_missing_state
import com.sumizuri.app.shared.resources.source_off_state
import com.sumizuri.app.shared.resources.title_action_move
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.art.InkLinearProgressIndicator
import com.sumizuri.app.ui.kit.art.InkOutlinedButton
import com.sumizuri.app.ui.kit.card.AppCard
import com.sumizuri.app.ui.kit.input.AppSearchBar
import com.sumizuri.app.ui.kit.setting.SettingRow
import com.sumizuri.app.ui.kit.setting.SettingsSection
import com.sumizuri.app.ui.text.displayName
import com.sumizuri.app.ui.text.progressTimeText
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// A long list of titles is narrowed by a word typed only past this many.
private const val FilterFrom = 8

// How many titles of a source show before the rest are asked for.
private const val Peek = 3

// The page that mends the titles of the library whose source is not installed or is switched off. On top it says how many there are and offers the two ways out: finding them again on the installed sources by site or name, or moving them all to sources the person picks. Under it each missing source has a card with a button that moves its titles, and its first few titles each with a button to move that one alone, the rest a tap away. A word typed narrows the list, and the buttons then take only what is shown.
@Composable
fun MissingSourcesRoute(onBack: () -> Unit, onMove: (Long) -> Unit, onMigrate: (List<Long>) -> Unit) {
    val library = LocalAppContainer.current.library
    val titles by remember(library) { library.unreachableTitles() }.collectAsStateWithLifecycle(emptyList<Title>())
    var query by remember { mutableStateOf("") }
    val matcher = LocalAppContainer.current.matcher
    // How far the look for sources has come, and how many titles found one. The look belongs to the app, so it goes on when the page is left.
    val run by matcher.linking.collectAsStateWithLifecycle()
    val checking = run.checking
    val found = run.found
    val shown = titles.filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
    SettingsPageFrame(
        Res.string.settings_missing_sources,
        onBack,
        subtitle = Res.string.settings_missing_sources_hint,
    ) {
        if (titles.isEmpty() &&
            found == null
        ) {
            Text(stringResource(Res.string.missing_sources_none), color = MaterialTheme.colorScheme.outline)
        }
        if (titles.isNotEmpty() || found != null) {
            Summary(
                count = titles.size,
                shown = shown.size,
                checking = checking,
                found = found,
                onDetect = { if (checking == null) matcher.linkAll(shown) },
                onMigrateAll = { onMigrate(shown.map { it.id }) },
            )
        }
        if (titles.size >
            FilterFrom
        ) {
            AppSearchBar(query, { query = it }, stringResource(Res.string.missing_filter_hint))
        }
        for ((source, group) in shown.groupBy { it.sourceId }) {
            SourceGroup(source, group, filtered = query.isNotBlank(), onMove = onMove, onMigrate = onMigrate)
        }
    }
}

// The head of the page: how many titles need a source and why, with the two ways to give them one. While the look runs it shows how far it got, and after it how many found a source.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Summary(
    count: Int,
    shown: Int,
    checking: Pair<Int, Int>?,
    found: Pair<Int, Int>?,
    onDetect: () -> Unit,
    onMigrateAll: () -> Unit,
) {
    AppCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (count >
                0
            ) {
                Text(
                    pluralStringResource(Res.plurals.missing_summary_title, count, count),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                stringResource(Res.string.missing_summary_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val status =
                checking?.let { (done, total) -> stringResource(Res.string.missing_detect_running, done, total) }
                    ?: found?.let { (hits, total) -> stringResource(Res.string.missing_detect_done, hits, total) }
            if (checking != null) {
                InkLinearProgressIndicator(
                    progress = { if (checking.second == 0) 0f else checking.first.toFloat() / checking.second },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
            }
            if (status !=
                null
            ) {
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            if (checking !=
                null
            ) {
                progressTimeText(checking.first, checking.second)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            if (shown > 0) {
                FlowRow(
                    Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    InkButton(onDetect, enabled = checking == null) { Text(stringResource(Res.string.missing_detect)) }
                    InkOutlinedButton(onMigrateAll, enabled = checking == null) {
                        Icon(Icons.Outlined.SwapHoriz, null, Modifier.padding(end = 8.dp))
                        Text(pluralStringResource(Res.plurals.missing_migrate_all, shown, shown))
                    }
                }
                Text(
                    stringResource(Res.string.missing_detect_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

// One missing source: its name and how many titles it held, a row that moves them together, and the titles, the first few at once and the rest when asked. A narrowed list shows all its matches at once, since there are few.
@Composable
private fun SourceGroup(
    source: String,
    group: List<Title>,
    filtered: Boolean,
    onMove: (Long) -> Unit,
    onMigrate: (List<Long>) -> Unit,
) {
    var open by rememberSaveable(source) { mutableStateOf(false) }
    val visible = if (open || filtered) group else group.take(Peek)
    val move = stringResource(Res.string.title_action_move)
    SettingsSection("${ImportedSources.labelOf(source) ?: source.ifBlank { "?" }} · ${group.size}") {
        if (group.size > 1) {
            row {
                SettingRow(
                    pluralStringResource(Res.plurals.missing_migrate_group, group.size, group.size),
                    leading = { Icon(Icons.Outlined.SwapHoriz, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { onMigrate(group.map { it.id }) },
                )
            }
        }
        for (title in visible) {
            row {
                SettingRow(
                    title.displayName(),
                    subtitle = stringResource(
                        if (title.sourceState ==
                            SourceState.Off
                        ) {
                            Res.string.source_off_state
                        } else {
                            Res.string.source_missing_state
                        },
                    ),
                    trailing = { IconButton({ onMove(title.id) }) { Icon(Icons.Outlined.SwapHoriz, move) } },
                    onClick = { onMove(title.id) },
                )
            }
        }
        if (visible.size < group.size) {
            row {
                SettingRow(
                    stringResource(Res.string.missing_show_all, group.size),
                    leading = { Icon(Icons.Outlined.ExpandMore, null, tint = MaterialTheme.colorScheme.outline) },
                    onClick = { open = true },
                )
            }
        }
    }
}
