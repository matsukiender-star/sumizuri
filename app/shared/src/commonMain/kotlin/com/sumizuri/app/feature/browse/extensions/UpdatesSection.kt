package com.sumizuri.app.feature.browse.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Upgrade
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.sources.SourceUpdate
import com.sumizuri.app.data.sources.UpdatesState
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.section_count
import com.sumizuri.app.shared.resources.sources_check
import com.sumizuri.app.shared.resources.sources_up_to_date
import com.sumizuri.app.shared.resources.sources_update_all
import com.sumizuri.app.shared.resources.sources_update_versions
import com.sumizuri.app.shared.resources.sources_updates
import com.sumizuri.app.shared.resources.update
import com.sumizuri.app.source.repo.versionText
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import com.sumizuri.app.ui.kit.art.InkTonalButton
import com.sumizuri.app.ui.kit.setting.SettingIcon
import com.sumizuri.app.ui.kit.setting.SettingRow
import com.sumizuri.app.ui.kit.setting.SettingsSection
import org.jetbrains.compose.resources.stringResource

// The sources that have a newer version in their repo, in one card with an Update on each, and under it a way to look again and one button for all of them. The check also runs by itself when the app opens.
@Composable
fun UpdatesSection(
    state: UpdatesState,
    onCheck: () -> Unit,
    onUpdate: (SourceUpdate) -> Unit,
    onUpdateAll: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val heading = stringResource(Res.string.sources_updates)
        val upToDate = state.checked && state.updates.isEmpty() && !state.checking
        if (state.updates.isNotEmpty() || upToDate) {
            SettingsSection(
                if (state.updates.isEmpty()) {
                    heading
                } else {
                    stringResource(
                        Res.string.section_count,
                        heading,
                        state.updates.size,
                    )
                },
                state.problem,
            ) {
                for (update in state.updates) {
                    row {
                        SettingRow(
                            update.name,
                            subtitle = stringResource(
                                Res.string.sources_update_versions,
                                versionText(update.have),
                                versionText(update.available.version),
                            ),
                            leading = { SettingIcon(Icons.Outlined.Upgrade) },
                            trailing = {
                                if (update.installedId in state.busy) {
                                    InkCircularProgressIndicator(Modifier.padding(8.dp).size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    InkTonalButton({ onUpdate(update) }) { Text(stringResource(Res.string.update)) }
                                }
                            },
                        )
                    }
                }
                if (upToDate) {
                    row {
                        SettingRow(
                            stringResource(Res.string.sources_up_to_date),
                            leading = { SettingIcon(Icons.Outlined.CheckCircle) },
                        )
                    }
                }
            }
        } else {
            state.problem?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onCheck, enabled = !state.checking) {
                if (state.checking) {
                    InkCircularProgressIndicator(
                        Modifier.padding(end = 8.dp).size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Outlined.Refresh, null, Modifier.padding(end = 8.dp))
                }
                Text(stringResource(Res.string.sources_check))
            }
            if (state.updates.size > 1) {
                TextButton(
                    onUpdateAll,
                    enabled = state.busy.isEmpty(),
                ) { Text(stringResource(Res.string.sources_update_all)) }
            }
        }
    }
}
