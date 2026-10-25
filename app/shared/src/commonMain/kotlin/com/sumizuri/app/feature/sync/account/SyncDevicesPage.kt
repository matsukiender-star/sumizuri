package com.sumizuri.app.feature.sync.account

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumizuri.app.feature.settings.SettingsPageFrame
import com.sumizuri.app.feature.sync.Problem
import com.sumizuri.app.feature.sync.Working
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.sync_device_line
import com.sumizuri.app.shared.resources.sync_device_sign_out
import com.sumizuri.app.shared.resources.sync_device_sign_out_message
import com.sumizuri.app.shared.resources.sync_device_sign_out_title
import com.sumizuri.app.shared.resources.sync_device_this
import com.sumizuri.app.shared.resources.sync_devices_hint
import com.sumizuri.app.shared.resources.sync_devices_section
import com.sumizuri.app.shared.resources.sync_sign_out_others
import com.sumizuri.app.shared.resources.sync_sign_out_others_message
import com.sumizuri.app.ui.kit.art.InkOutlinedButton
import com.sumizuri.app.ui.kit.dialog.ConfirmDialog
import com.sumizuri.app.ui.kit.setting.SettingIcon
import com.sumizuri.app.ui.kit.setting.SettingRow
import com.sumizuri.app.ui.kit.setting.SettingsSection
import com.sumizuri.app.ui.text.agoText
import com.sumizuri.sync.core.DeviceEntry
import com.sumizuri.sync.core.DeviceType
import org.jetbrains.compose.resources.stringResource

// Every device signed in to the account. Any but this one can be signed out, such as a lost phone.
@Composable
internal fun SyncDevicesRoute(onBack: () -> Unit) {
    SettingsPageFrame(Res.string.sync_devices_section, onBack, subtitle = Res.string.sync_devices_hint) {
        SignedIn { account ->
            val model = rememberSyncHome(account)
            val home by model.home.collectAsStateWithLifecycle()
            var removing by remember { mutableStateOf<DeviceEntry?>(null) }
            var others by remember { mutableStateOf(false) }
            SettingsSection {
                for (device in home.devices.orEmpty()) {
                    row {
                        SettingRow(
                            if (device.thisOne) stringResource(Res.string.sync_device_this) else device.name,
                            subtitle = stringResource(
                                Res.string.sync_device_line,
                                if (device.thisOne) device.name else device.appVersion,
                                agoText(device.lastSeen),
                            ),
                            leading = { SettingIcon(deviceIcon(device.type)) },
                            trailing = if (device.thisOne) {
                                null
                            } else {
                                (
                                    {
                                        TextButton(
                                            { removing = device },
                                            enabled = !home.busy,
                                        ) { Text(stringResource(Res.string.sync_device_sign_out)) }
                                    }
                                    )
                            },
                        )
                    }
                }
                // Every other device at once, such as when one was lost and the password changes anyway.
                if (home.devices.orEmpty().count { !it.thisOne } >
                    1
                ) {
                    padded {
                        InkOutlinedButton(
                            { others = true },
                            enabled = !home.busy,
                        ) { Text(stringResource(Res.string.sync_sign_out_others)) }
                    }
                }
            }
            if (home.busy) Working()
            home.problem?.let { Problem(it) }
            if (others) {
                ConfirmDialog(
                    stringResource(Res.string.sync_sign_out_others),
                    stringResource(Res.string.sync_sign_out_others_message),
                    stringResource(Res.string.sync_device_sign_out),
                    onConfirm = {
                        others = false
                        model.signOutOthers()
                    },
                    onDismiss = { others = false },
                )
            }
            removing?.let { device ->
                ConfirmDialog(
                    stringResource(Res.string.sync_device_sign_out_title, device.name),
                    stringResource(Res.string.sync_device_sign_out_message),
                    stringResource(Res.string.sync_device_sign_out),
                    onConfirm = {
                        removing = null
                        model.removeDevice(device.id)
                    },
                    onDismiss = { removing = null },
                )
            }
        }
    }
}

internal fun deviceIcon(type: DeviceType) = when (type) {
    DeviceType.Phone -> Icons.Outlined.PhoneAndroid
    DeviceType.Tablet -> Icons.Outlined.TabletAndroid
    DeviceType.Desktop -> Icons.Outlined.Computer
}
