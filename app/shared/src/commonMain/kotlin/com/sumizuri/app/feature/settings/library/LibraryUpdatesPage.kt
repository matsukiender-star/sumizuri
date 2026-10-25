package com.sumizuri.app.feature.settings.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.settings.SettingKey
import com.sumizuri.app.data.sync.UpdatePlatform
import com.sumizuri.app.data.sync.rememberNoticePermission
import com.sumizuri.app.feature.settings.SettingsPageFrame
import com.sumizuri.app.feature.settings.SettingsViewModel
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.settings_library_updates
import com.sumizuri.app.shared.resources.settings_library_updates_hint
import com.sumizuri.app.shared.resources.shelves_title
import com.sumizuri.app.shared.resources.update_background
import com.sumizuri.app.shared.resources.update_background_hint
import com.sumizuri.app.shared.resources.update_charging
import com.sumizuri.app.shared.resources.update_charging_hint
import com.sumizuri.app.shared.resources.update_group_exclusions
import com.sumizuri.app.shared.resources.update_group_schedule
import com.sumizuri.app.shared.resources.update_interval
import com.sumizuri.app.shared.resources.update_notify
import com.sumizuri.app.shared.resources.update_notify_hint
import com.sumizuri.app.shared.resources.update_shelves_hint
import com.sumizuri.app.shared.resources.update_skip_completed
import com.sumizuri.app.shared.resources.update_skip_completed_hint
import com.sumizuri.app.shared.resources.update_skip_not_started
import com.sumizuri.app.shared.resources.update_skip_not_started_hint
import com.sumizuri.app.shared.resources.update_skip_unread
import com.sumizuri.app.shared.resources.update_skip_unread_hint
import com.sumizuri.app.shared.resources.update_smart
import com.sumizuri.app.shared.resources.update_smart_hint
import com.sumizuri.app.shared.resources.update_wifi_only
import com.sumizuri.app.shared.resources.update_wifi_only_hint
import com.sumizuri.app.ui.kit.card.ListChevron
import com.sumizuri.app.ui.kit.setting.ChoiceSetting
import com.sumizuri.app.ui.kit.setting.SettingIcon
import com.sumizuri.app.ui.kit.setting.SettingRow
import com.sumizuri.app.ui.kit.setting.SettingsSection
import com.sumizuri.app.ui.kit.setting.SwitchSetting
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// How many hours the choices of the interval are.
internal val UpdateIntervals = listOf(0, 1, 6, 12, 24, 48, 168)

// The page for when the library looks for new chapters on its own and which titles it leaves out. The shelves that are left out are set on the page of the shelves.
@Composable
fun LibraryUpdatesRoute(onBack: () -> Unit, onOpenShelves: () -> Unit) {
    val container = LocalAppContainer.current
    val settings = viewModel { SettingsViewModel(container.settings) }
    val hours by settings.updateHours.collectAsStateWithLifecycle()
    val completed by settings.skipCompleted.collectAsStateWithLifecycle()
    val notStarted by settings.skipNotStarted.collectAsStateWithLifecycle()
    val prefs = container.settings
    val scope = rememberCoroutineScope()
    val askNotices = rememberNoticePermission()
    val background by remember(
        prefs,
    ) { prefs.flag(SettingKey.UpdateInBackground, true) }.collectAsStateWithLifecycle(true)
    val wifiOnly by remember(prefs) { prefs.flag(SettingKey.UpdateWifiOnly, false) }.collectAsStateWithLifecycle(false)
    val charging by remember(prefs) { prefs.flag(SettingKey.UpdateCharging, false) }.collectAsStateWithLifecycle(false)
    val notify by remember(prefs) { prefs.flag(SettingKey.UpdateNotify, true) }.collectAsStateWithLifecycle(true)
    val smart by remember(prefs) { prefs.flag(SettingKey.UpdateSmart, true) }.collectAsStateWithLifecycle(true)
    val skipUnread by remember(
        prefs,
    ) { prefs.flag(SettingKey.UpdateSkipUnread, false) }.collectAsStateWithLifecycle(false)
    SettingsPageFrame(
        Res.string.settings_library_updates,
        onBack,
        subtitle = Res.string.settings_library_updates_hint,
        // Whether the person was asked about notices stays, so they are not asked again.
        onReset = { prefs.reset("library.update.", except = SettingKey.NoticeAsked) },
    ) {
        SettingsSection(Res.string.update_group_schedule) {
            padded {
                ChoiceSetting(Res.string.update_interval, UpdateIntervals, hours, {
                    updateIntervalLabel(it)
                }) { settings.onUpdateHours(it) }
            }
            // The check while the app is closed, only where the system can run one, and only with an interval to run on. Its conditions are greyed out while it is off.
            if (UpdatePlatform.runsInBackground && hours > 0) {
                padded {
                    SwitchSetting(Res.string.update_background, background, { on ->
                        scope.launch {
                            prefs.setFlag(SettingKey.UpdateInBackground, on)
                        }
                    }, Res.string.update_background_hint)
                }
                padded {
                    SwitchSetting(Res.string.update_wifi_only, wifiOnly, { on ->
                        scope.launch {
                            prefs.setFlag(SettingKey.UpdateWifiOnly, on)
                        }
                    }, Res.string.update_wifi_only_hint, enabled = background)
                }
                padded {
                    SwitchSetting(Res.string.update_charging, charging, { on ->
                        scope.launch {
                            prefs.setFlag(SettingKey.UpdateCharging, on)
                        }
                    }, Res.string.update_charging_hint, enabled = background)
                }
            }
            padded {
                SwitchSetting(Res.string.update_notify, notify, { on ->
                    scope.launch { prefs.setFlag(SettingKey.UpdateNotify, on) }
                    if (on) askNotices()
                }, Res.string.update_notify_hint)
            }
        }
        SettingsSection(Res.string.update_group_exclusions) {
            padded {
                SwitchSetting(Res.string.update_smart, smart, { on ->
                    scope.launch {
                        prefs.setFlag(SettingKey.UpdateSmart, on)
                    }
                }, Res.string.update_smart_hint)
            }
            padded {
                SwitchSetting(
                    Res.string.update_skip_completed,
                    completed,
                    settings::onSkipCompleted,
                    Res.string.update_skip_completed_hint,
                )
            }
            padded {
                SwitchSetting(
                    Res.string.update_skip_not_started,
                    notStarted,
                    settings::onSkipNotStarted,
                    Res.string.update_skip_not_started_hint,
                )
            }
            padded {
                SwitchSetting(Res.string.update_skip_unread, skipUnread, { on ->
                    scope.launch {
                        prefs.setFlag(SettingKey.UpdateSkipUnread, on)
                    }
                }, Res.string.update_skip_unread_hint)
            }
            row {
                SettingRow(
                    stringResource(Res.string.shelves_title),
                    subtitle = stringResource(Res.string.update_shelves_hint),
                    leading = { SettingIcon(Icons.AutoMirrored.Outlined.Label) },
                    trailing = { ListChevron() },
                    onClick = onOpenShelves,
                )
            }
        }
    }
}
