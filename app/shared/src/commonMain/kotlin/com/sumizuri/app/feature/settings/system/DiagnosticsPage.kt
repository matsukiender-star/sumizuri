package com.sumizuri.app.feature.settings.system

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.sumizuri.app.BuildInfo
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.storage.StorageUse
import com.sumizuri.app.feature.settings.SettingsPageFrame
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.app_name
import com.sumizuri.app.shared.resources.diagnostics_copy
import com.sumizuri.app.shared.resources.diagnostics_copy_hint
import com.sumizuri.app.shared.resources.diagnostics_group_app
import com.sumizuri.app.shared.resources.diagnostics_group_tools
import com.sumizuri.app.shared.resources.diagnostics_library
import com.sumizuri.app.shared.resources.diagnostics_sources
import com.sumizuri.app.shared.resources.settings_diagnostics
import com.sumizuri.app.shared.resources.settings_diagnostics_hint
import com.sumizuri.app.shared.resources.settings_logs
import com.sumizuri.app.shared.resources.settings_logs_hint
import com.sumizuri.app.shared.resources.settings_version
import com.sumizuri.app.ui.kit.card.ListChevron
import com.sumizuri.app.ui.kit.input.rememberCopyText
import com.sumizuri.app.ui.kit.setting.SettingIcon
import com.sumizuri.app.ui.kit.setting.SettingRow
import com.sumizuri.app.ui.kit.setting.SettingsSection
import com.sumizuri.app.ui.text.byteSizeText
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource

// The page of what a bug report needs: the version, how big the library is, how many sources are installed, a button that copies all of it, and a way to the log.
@Composable
fun DiagnosticsRoute(onBack: () -> Unit, onOpenLogs: () -> Unit) {
    val container = LocalAppContainer.current
    val copy = rememberCopyText()
    val use by produceState(StorageUse()) { value = container.storage.use() }
    val sources by produceState(0) {
        value = container.sources.installed
            .first()
            .size
    }
    val name = stringResource(Res.string.app_name)
    val version = stringResource(Res.string.settings_version, BuildInfo.Version)
    val library =
        stringResource(Res.string.diagnostics_library, use.titles, use.chapters, byteSizeText(use.databaseBytes))
    val installed = stringResource(Res.string.diagnostics_sources, sources)
    SettingsPageFrame(Res.string.settings_diagnostics, onBack, subtitle = Res.string.settings_diagnostics_hint) {
        SettingsSection(Res.string.diagnostics_group_app) {
            row { SettingRow(name, subtitle = version, leading = { SettingIcon(Icons.Outlined.Info) }) }
            row { SettingRow(library, subtitle = installed, leading = { SettingIcon(Icons.Outlined.Storage) }) }
        }
        SettingsSection(Res.string.diagnostics_group_tools) {
            row {
                SettingRow(
                    stringResource(Res.string.diagnostics_copy),
                    leading = { SettingIcon(Icons.Outlined.ContentCopy) },
                    subtitle = stringResource(Res.string.diagnostics_copy_hint),
                    onClick = { copy("$name\n$version\n$library\n$installed") },
                )
            }
            row {
                SettingRow(
                    stringResource(Res.string.settings_logs),
                    leading = { SettingIcon(Icons.AutoMirrored.Outlined.Article) },
                    subtitle = stringResource(Res.string.settings_logs_hint),
                    trailing = { ListChevron() },
                    onClick = onOpenLogs,
                )
            }
        }
    }
}
