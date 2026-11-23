package com.sumizuri.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.ui.graphics.vector.ImageVector
import com.sumizuri.app.data.model.library.LibraryPrefs
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.nav_browse
import com.sumizuri.app.shared.resources.nav_history
import com.sumizuri.app.shared.resources.nav_library
import com.sumizuri.app.shared.resources.nav_profile
import com.sumizuri.app.shared.resources.nav_updates
import com.sumizuri.app.shared.resources.suki_live_tab
import org.jetbrains.compose.resources.StringResource

enum class Destination(val label: StringResource, val icon: ImageVector, val selectedIcon: ImageVector) {
    Library(Res.string.nav_library, Icons.Outlined.CollectionsBookmark, Icons.Filled.CollectionsBookmark),
    Updates(Res.string.nav_updates, Icons.Outlined.NewReleases, Icons.Filled.NewReleases),
    History(Res.string.nav_history, Icons.Outlined.History, Icons.Filled.History),
    Browse(Res.string.nav_browse, Icons.Outlined.Explore, Icons.Filled.Explore),
    Live(Res.string.suki_live_tab, Icons.Outlined.LiveTv, Icons.Filled.LiveTv),
    Profile(Res.string.nav_profile, Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle),
}

// The tabs that show: the updates, history and live tabs can be hidden in settings.
fun shownTabs(prefs: LibraryPrefs, showLive: Boolean): List<Destination> = Destination.entries.filter {
    (it != Destination.Updates || prefs.showUpdatesTab) && (it != Destination.History || prefs.showHistoryTab) &&
        (it != Destination.Live || showLive)
}
