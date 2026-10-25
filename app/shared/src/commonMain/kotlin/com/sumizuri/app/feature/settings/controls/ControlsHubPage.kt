package com.sumizuri.app.feature.settings.controls

import androidx.compose.runtime.Composable
import com.sumizuri.app.feature.settings.PageRow
import com.sumizuri.app.feature.settings.SettingsPageFrame
import com.sumizuri.app.feature.settings.controls.ControlsPage
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.controls_hint
import com.sumizuri.app.shared.resources.controls_title
import com.sumizuri.app.ui.kit.setting.SettingsSection
import org.jetbrains.compose.resources.stringResource

// The one way into the pages of controls: the keys and the taps of the reader, and the shortcuts and the gestures of the whole app. The home of the settings has one row for all of them.
@Composable
fun ControlsHubRoute(onBack: () -> Unit, onOpen: (ControlsPage) -> Unit) {
    SettingsPageFrame(Res.string.controls_title, onBack, subtitle = Res.string.controls_hint) {
        SettingsSection {
            for (page in ControlsPage.entries) row { PageRow(page.icon, stringResource(page.title), stringResource(page.hint), onClick = { onOpen(page) }) }
        }
    }
}
