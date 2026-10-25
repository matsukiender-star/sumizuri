package com.sumizuri.app.feature.browse.extensions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.Repo
import com.sumizuri.app.data.sources.SourceUpdate
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.ext_edit_new_row
import com.sumizuri.app.shared.resources.ext_edit_new_row_hint
import com.sumizuri.app.shared.resources.extensions_add_file
import com.sumizuri.app.shared.resources.extensions_add_file_hint
import com.sumizuri.app.shared.resources.extensions_add_repo
import com.sumizuri.app.shared.resources.extensions_empty
import com.sumizuri.app.shared.resources.extensions_repos
import com.sumizuri.app.shared.resources.remove
import com.sumizuri.app.shared.resources.repo_move_button
import com.sumizuri.app.ui.kit.card.ListChevron
import com.sumizuri.app.ui.kit.input.rememberFilePicker
import com.sumizuri.app.ui.kit.page.PageList
import com.sumizuri.app.ui.kit.setting.SettingIcon
import com.sumizuri.app.ui.kit.setting.SettingRow
import com.sumizuri.app.ui.kit.setting.SettingsSection
import okio.Path
import org.jetbrains.compose.resources.stringResource

// The repos the user added. Sumizuri has no repo of its own, so with none added this says how to get sources.
@Composable
fun ExtensionsScreen(
    state: ExtensionsState,
    onOpenRepo: (Repo) -> Unit,
    onCheck: () -> Unit,
    onUpdate: (SourceUpdate) -> Unit,
    onUpdateAll: () -> Unit,
    onAddOpen: () -> Unit,
    onAddress: (String) -> Unit,
    onAddConfirm: () -> Unit,
    onAddDismiss: () -> Unit,
    onRemoveAsk: (Repo) -> Unit,
    onRemoveConfirm: () -> Unit,
    onRemoveDismiss: () -> Unit,
    onImportPicked: (Path) -> Unit,
    onImportName: (String) -> Unit,
    onImportKind: (Kind) -> Unit,
    onImportConfirm: () -> Unit,
    onImportDismiss: () -> Unit,
    onNewExtension: () -> Unit,
    onMoveAsk: (Repo) -> Unit,
    onMoveTarget: (Repo) -> Unit,
    onMoveRemoveOld: (Boolean) -> Unit,
    onMoveConfirm: () -> Unit,
    onMoveDismiss: () -> Unit,
) {
    val pickFile = rememberFilePicker(onPicked = onImportPicked)
    PageList(gap = 14.dp) {
        item { UpdatesSection(state.updates, onCheck, onUpdate, onUpdateAll) }
        // The repos in one card, with the way to add one at its end. With none added, the line under it says how to get sources.
        item {
            SettingsSection(
                stringResource(Res.string.extensions_repos),
                if (state.loaded &&
                    state.repos.isEmpty()
                ) {
                    stringResource(Res.string.extensions_empty)
                } else {
                    null
                },
            ) {
                for (repo in state.repos) {
                    row {
                        SettingRow(
                            repo.name,
                            subtitle = repo.url,
                            subtitleLines = 1,
                            leading = { SettingIcon(Icons.Outlined.Storefront) },
                            trailing = {
                                // With another repo added, the sources of this one can move to it.
                                if (state.repos.size > 1) {
                                    IconButton({ onMoveAsk(repo) }) {
                                        Icon(
                                            Icons.Outlined.SwapHoriz,
                                            stringResource(Res.string.repo_move_button),
                                            tint = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                }
                                IconButton({
                                    onRemoveAsk(repo)
                                }) {
                                    Icon(
                                        Icons.Outlined.DeleteOutline,
                                        stringResource(Res.string.remove),
                                        tint = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            },
                            onClick = { onOpenRepo(repo) },
                        )
                    }
                }
                row {
                    SettingRow(stringResource(Res.string.extensions_add_repo), leading = {
                        SettingIcon(Icons.Outlined.Add)
                    }, onClick = onAddOpen)
                }
            }
        }
        // The other ways to get a source: a file of one, or one written here.
        item {
            SettingsSection {
                row {
                    SettingRow(
                        stringResource(Res.string.extensions_add_file),
                        subtitle = stringResource(Res.string.extensions_add_file_hint),
                        leading = { SettingIcon(Icons.Outlined.FileOpen) },
                        trailing = { ListChevron() },
                        onClick = pickFile,
                    )
                }
                row {
                    SettingRow(
                        stringResource(Res.string.ext_edit_new_row),
                        subtitle = stringResource(Res.string.ext_edit_new_row_hint),
                        leading = { SettingIcon(Icons.Outlined.Code) },
                        trailing = { ListChevron() },
                        onClick = onNewExtension,
                    )
                }
            }
        }
    }
    state.adding?.let { AddRepoDialog(it, onAddress, onAddConfirm, onAddDismiss) }
    state.removing?.let { RemoveRepoDialog(it, onRemoveConfirm, onRemoveDismiss) }
    state.importing?.let { ImportSourceDialog(it, onImportName, onImportKind, onImportConfirm, onImportDismiss) }
    state.moving?.let { moving ->
        val others = state.repos.filter { it.url != moving.from.url }
        RepoMoveDialog(moving, others, onMoveTarget, onMoveRemoveOld, onMoveConfirm, onMoveDismiss)
    }
}
