package com.sumizuri.app.feature.settings

import com.sumizuri.app.data.model.library.LibraryPrefs
import com.sumizuri.app.data.model.reader.ReaderSettings
import com.sumizuri.app.feature.settings.controls.ControlsPage
import com.sumizuri.app.navigation.bar.NavStyle
import com.sumizuri.app.ui.theme.ThemeMode

// What the pages of the settings open, so the index does not know about navigation.
internal class SettingsOpens(val target: (SettingsTarget) -> Unit, val controls: (ControlsPage) -> Unit)

// What the choices are set to now, as far as the home of the settings tells it under the name of a page.
internal class SettingsNow(
    val themeMode: ThemeMode,
    val themeName: String,
    val language: String,
    val library: LibraryPrefs,
    val reader: ReaderSettings,
    val updateHours: Int,
    val unreachable: Int,
    val navStyle: NavStyle,
    val shelves: Int,
)
