package com.sumizuri.app.feature.settings.controls

import androidx.compose.runtime.Composable
import com.sumizuri.app.feature.settings.SettingsPageFrame

// The frame every controls page has, the same as the other pages of the settings: the bar with the back arrow, the header with the icon of the page and a line about it, and the content in a column that scrolls.
@Composable
internal fun ControlsFrame(page: ControlsPage, onBack: () -> Unit, content: @Composable () -> Unit) {
    SettingsPageFrame(page.title, onBack, subtitle = page.hint) { content() }
}
