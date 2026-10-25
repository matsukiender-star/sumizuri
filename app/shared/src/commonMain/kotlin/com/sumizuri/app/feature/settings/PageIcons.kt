package com.sumizuri.app.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Update
import androidx.compose.ui.graphics.vector.ImageVector
import com.sumizuri.app.feature.settings.controls.ControlsPage
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.about_licenses
import com.sumizuri.app.shared.resources.appupdate_title
import com.sumizuri.app.shared.resources.chapter_settings_title
import com.sumizuri.app.shared.resources.controls_title
import com.sumizuri.app.shared.resources.display_title
import com.sumizuri.app.shared.resources.download_title
import com.sumizuri.app.shared.resources.legal_title
import com.sumizuri.app.shared.resources.perf_details
import com.sumizuri.app.shared.resources.privacy_title
import com.sumizuri.app.shared.resources.settings_about
import com.sumizuri.app.shared.resources.settings_advanced
import com.sumizuri.app.shared.resources.settings_appearance
import com.sumizuri.app.shared.resources.settings_backup
import com.sumizuri.app.shared.resources.settings_diagnostics
import com.sumizuri.app.shared.resources.settings_layout
import com.sumizuri.app.shared.resources.settings_library_updates
import com.sumizuri.app.shared.resources.settings_missing_sources
import com.sumizuri.app.shared.resources.settings_network
import com.sumizuri.app.shared.resources.settings_player
import com.sumizuri.app.shared.resources.settings_reading
import com.sumizuri.app.shared.resources.settings_security
import com.sumizuri.app.shared.resources.settings_storage
import com.sumizuri.app.shared.resources.shelves_title
import com.sumizuri.app.shared.resources.sync_title
import org.jetbrains.compose.resources.StringResource

// The icon at the top of each page of the settings, found by the name of the page.
internal val PageIcons: Map<StringResource, ImageVector> by lazy {
    mapOf(
        Res.string.appupdate_title to Icons.Outlined.SystemUpdate,
        Res.string.sync_title to Icons.Outlined.Sync,
        Res.string.settings_about to Icons.Outlined.Info,
        Res.string.settings_advanced to Icons.Outlined.Tune,
        Res.string.settings_appearance to Icons.Outlined.Palette,
        Res.string.controls_title to Icons.Outlined.TouchApp,
        Res.string.settings_diagnostics to Icons.Outlined.BugReport,
        Res.string.download_title to Icons.Outlined.Download,
        Res.string.settings_layout to Icons.Outlined.Dashboard,
        Res.string.settings_library_updates to Icons.Outlined.Update,
        Res.string.chapter_settings_title to Icons.AutoMirrored.Outlined.FormatListBulleted,
        Res.string.about_licenses to Icons.Outlined.Gavel,
        Res.string.legal_title to Icons.Outlined.Policy,
        Res.string.privacy_title to Icons.Outlined.PrivacyTip,
        Res.string.settings_missing_sources to Icons.Outlined.LinkOff,
        Res.string.perf_details to Icons.Outlined.Speed,
        Res.string.settings_player to Icons.Outlined.PlayCircle,
        Res.string.settings_reading to Icons.Outlined.AutoStories,
        Res.string.settings_security to Icons.Outlined.Lock,
        Res.string.settings_network to Icons.Outlined.Lan,
        Res.string.settings_storage to Icons.Outlined.PieChart,
        Res.string.display_title to Icons.Outlined.GridView,
        Res.string.shelves_title to Icons.AutoMirrored.Outlined.Label,
        Res.string.settings_backup to Icons.Outlined.SettingsBackupRestore,
    ) + ControlsPage.entries.associate { it.title to it.icon }
}
