package com.sumizuri.app.feature.browse.repo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sumizuri.app.feature.browse.hub.UpdateNotice
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.all
import com.sumizuri.app.shared.resources.cancel
import com.sumizuri.app.shared.resources.remove
import com.sumizuri.app.shared.resources.repo_copy_address
import com.sumizuri.app.shared.resources.repo_empty
import com.sumizuri.app.shared.resources.repo_filtered_empty
import com.sumizuri.app.shared.resources.repo_obsolete
import com.sumizuri.app.shared.resources.repo_obsolete_hint
import com.sumizuri.app.shared.resources.repo_search_hint
import com.sumizuri.app.shared.resources.repo_show_nsfw
import com.sumizuri.app.shared.resources.section_count
import com.sumizuri.app.shared.resources.sources_update_all
import com.sumizuri.app.shared.resources.sources_updates_notice
import com.sumizuri.app.shared.resources.suki_repo_all_kinds
import com.sumizuri.app.shared.resources.suki_repo_all_languages
import com.sumizuri.app.shared.resources.suki_repo_dismiss
import com.sumizuri.app.shared.resources.suki_repo_install_all
import com.sumizuri.app.shared.resources.suki_repo_install_all_message
import com.sumizuri.app.shared.resources.suki_repo_install_all_notice
import com.sumizuri.app.shared.resources.suki_repo_install_all_title
import com.sumizuri.app.shared.resources.suki_repo_install_cancelled
import com.sumizuri.app.shared.resources.suki_repo_install_done
import com.sumizuri.app.shared.resources.suki_repo_install_done_failed
import com.sumizuri.app.shared.resources.suki_repo_installing_progress
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.card.AppListRow
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import com.sumizuri.app.ui.kit.card.CardPlace
import com.sumizuri.app.ui.kit.card.SoftGap
import com.sumizuri.app.ui.kit.card.cardRow
import com.sumizuri.app.ui.kit.card.softCard
import com.sumizuri.app.ui.kit.chips.ChipRow
import com.sumizuri.app.ui.kit.chips.ChipSeparator
import com.sumizuri.app.ui.kit.chips.ChoiceChip
import com.sumizuri.app.ui.kit.chips.ToggleChip
import com.sumizuri.app.ui.kit.cover.SourceMark
import com.sumizuri.app.ui.kit.dialog.AppDialog
import com.sumizuri.app.ui.kit.dialog.CancelButton
import com.sumizuri.app.ui.kit.input.AppSearchBar
import com.sumizuri.app.ui.kit.page.LoadStatus
import com.sumizuri.app.ui.kit.page.PageGrid
import com.sumizuri.app.ui.kit.page.PageList
import com.sumizuri.app.ui.kit.page.PageTopBar
import com.sumizuri.app.ui.kit.page.ReadableColumn
import com.sumizuri.app.ui.kit.page.TileWidth
import com.sumizuri.app.ui.kit.page.fullSpan
import com.sumizuri.app.ui.kit.page.readableLine
import com.sumizuri.app.ui.kit.setting.SettingRow
import com.sumizuri.app.ui.motion.entrance
import com.sumizuri.app.ui.text.languageName
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// The sources one repo lists, under a heading for each language, with a choice of kind, narrowed by typing, each tile with the one thing to do with it. The tiles use the whole width, while what stands above them keeps a readable one.
@Composable
fun RepoScreen(state: RepoState, actions: RepoActions) {
    val kind = state.kind
    val groups = state.groups
    val updates = state.updates
    val missingVisible = state.missingVisible
    var confirmInstallAll by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        PageTopBar(state.repo.name, subtitle = state.repo.url, onBack = actions.onBack) {
            IconButton(
                actions.onCopyAddress,
            ) { Icon(Icons.Outlined.ContentCopy, stringResource(Res.string.repo_copy_address)) }
        }
        // A tile here also holds a button, so it needs a little more room than a tile of Browse.
        PageGrid(
            Modifier.weight(1f),
            gap = SoftGap + 2.dp,
            rowGap = SoftGap + 2.dp,
            columns = GridCells.Adaptive(TileWidth + 60.dp),
        ) {
            readableLine("status") { LoadStatus(state.loading, state.error, actions.onLoad) }
            if (state.available.isNotEmpty()) {
                readableLine(
                    "search",
                ) { AppSearchBar(state.query, actions.onQuery, stringResource(Res.string.repo_search_hint)) }
                // The chips use the whole width, so a long row of languages does not need a sideways scroll on a wide window.
                fullSpan("chips") {
                    Column(Modifier.animateItem(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChipRow {
                            if (state.kinds.size > 1) {
                                ChoiceChip(stringResource(Res.string.all), kind == null) { actions.onKind(null) }
                                for (each in state.kinds) {
                                    ChoiceChip(stringResource(each.label), each == kind) { actions.onKind(each) }
                                }
                                if (state.hasNsfw) ChipSeparator()
                            }
                            if (state.hasNsfw) {
                                ToggleChip(stringResource(Res.string.repo_show_nsfw), state.showNsfw, {
                                    actions.onShowNsfw(!state.showNsfw)
                                })
                            }
                        }
                        // A repo with sources in several languages can be narrowed to one of them.
                        val languages = state.languages
                        if (languages.size > 1) {
                            // A wide window shows every language at once, a phone scrolls them sideways.
                            BoxWithConstraints { ChipRow(wrap = maxWidth >= 600.dp) {
                                ChoiceChip(
                                    stringResource(Res.string.all),
                                    state.language == null,
                                ) { actions.onLanguage(null) }
                                for (each in languages) {
                                    ChoiceChip(languageName(each), each == state.language) { actions.onLanguage(each) }
                                }
                            } }
                        }
                    }
                }
                if (updates.isNotEmpty()) {
                    readableLine("updates") { UpdateNotice(updates.size, state.busy.isEmpty(), actions.onUpdateAll) }
                }
                readableLine("batch") {
                    BatchInstallRow(
                        progress = state.batchInstall,
                        missingCount = missingVisible.size,
                        enabled = state.busy.isEmpty(),
                        onAskInstallAll = { confirmInstallAll = true },
                        onCancel = actions.onCancelBatchInstall,
                        onDismiss = actions.onDismissBatchResult,
                    )
                }
            }
            if (!state.loading && state.error == null && state.available.isEmpty()) {
                readableLine("empty") { Message(Res.string.repo_empty) }
            } else if (state.available.isNotEmpty() && groups.isEmpty()) {
                readableLine("filtered") { Message(Res.string.repo_filtered_empty) }
            }
            for (group in groups) {
                fullSpan("label:${group.language}") {
                    AppSectionLabel(
                        stringResource(Res.string.section_count, languageName(group.language), group.sources.size),
                        Modifier.animateItem(),
                    )
                }
                items(group.sources, key = { source -> "${group.language}:${source.id}" }) { source ->
                    val have = state.installed[source.id]
                    RepoSourceRow(
                        source,
                        have,
                        busy = source.id in state.busy || have?.id in state.busy,
                        onInstall = { actions.onInstall(source) },
                        onRemove = { have?.let { actions.onRemove(it.id, source.name) } },
                        modifier = Modifier.animateItem().entrance(),
                    )
                }
            }
            val obsolete = state.obsolete
            if (obsolete.isNotEmpty()) {
                fullSpan("label:obsolete") {
                    AppSectionLabel(
                        stringResource(
                            Res.string.section_count,
                            stringResource(Res.string.repo_obsolete),
                            obsolete.size,
                        ),
                        Modifier.animateItem(),
                    )
                }
                items(obsolete, key = { have -> "obsolete:${have.id}" }) { have ->
                    SettingRow(
                        have.name,
                        modifier = Modifier.animateItem().entrance().softCard(),
                        subtitle = stringResource(Res.string.repo_obsolete_hint),
                        leading = { SourceMark(have.iconUrl, size = 40.dp) },
                        trailing = {
                            IconButton({
                                actions.onRemove(have.id, have.name)
                            }) { Icon(Icons.Outlined.DeleteOutline, stringResource(Res.string.remove)) }
                        },
                    )
                }
            }
        }
    }
    if (confirmInstallAll && missingVisible.isNotEmpty()) {
        val count = missingVisible.size
        AppDialog(
            onDismissRequest = { confirmInstallAll = false },
            title = { Text(stringResource(Res.string.suki_repo_install_all_title, count)) },
            text = {
                // Says what the filters picked, since the count alone hides a kind or a language left on.
                val kindName = stringResource(kind?.label ?: Res.string.suki_repo_all_kinds)
                val languageText =
                    state.language?.let { languageName(it) } ?: stringResource(Res.string.suki_repo_all_languages)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$kindName · $languageText", style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(Res.string.suki_repo_install_all_message, count))
                }
            },
            confirmButton = {
                InkButton(onClick = {
                    confirmInstallAll = false
                    actions.onInstallAllVisible()
                }) { Text(stringResource(Res.string.suki_repo_install_all)) }
            },
            dismissButton = { CancelButton({ confirmInstallAll = false }) },
        )
    }
}

@Composable
private fun BatchInstallRow(
    progress: BatchInstallProgress?,
    missingCount: Int,
    enabled: Boolean,
    onAskInstallAll: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    when {
        progress != null && !progress.finished -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppListRow(
                    title = stringResource(
                        Res.string.suki_repo_installing_progress,
                        (progress.done + 1).coerceAtMost(progress.total),
                        progress.total,
                        progress.currentName,
                    ),
                    leading = { Icon(Icons.Outlined.Download, null, tint = MaterialTheme.colorScheme.primary) },
                    trailing = { TextButton(onCancel) { Text(stringResource(Res.string.cancel)) } },
                )
                LinearProgressIndicator(
                    progress = { if (progress.total > 0) progress.done.toFloat() / progress.total else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        progress != null && progress.finished -> {
            val summary = when {
                progress.cancelled -> stringResource(
                    Res.string.suki_repo_install_cancelled,
                    progress.succeeded,
                    progress.total,
                    progress.failed,
                )

                progress.failed > 0 -> stringResource(
                    Res.string.suki_repo_install_done_failed,
                    progress.succeeded,
                    progress.total,
                    progress.failed,
                )

                else -> stringResource(Res.string.suki_repo_install_done, progress.succeeded, progress.total)
            }
            AppListRow(
                title = summary,
                leading = { Icon(Icons.Outlined.Download, null, tint = MaterialTheme.colorScheme.primary) },
                trailing = { TextButton(onDismiss) { Text(stringResource(Res.string.suki_repo_dismiss)) } },
            )
        }

        missingCount > 0 -> {
            AppListRow(
                title = stringResource(Res.string.suki_repo_install_all_notice, missingCount),
                leading = { Icon(Icons.Outlined.Download, null, tint = MaterialTheme.colorScheme.primary) },
                trailing = {
                    TextButton(onAskInstallAll, enabled = enabled) {
                        Text(stringResource(Res.string.suki_repo_install_all))
                    }
                },
            )
        }
    }
}

@Composable
private fun Message(text: StringResource) {
    Text(
        stringResource(text),
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
    )
}
