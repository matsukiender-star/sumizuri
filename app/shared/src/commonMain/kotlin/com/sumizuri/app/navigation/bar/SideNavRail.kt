package com.sumizuri.app.navigation.bar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.navigation.Destination

// The narrow column of tabs down the side of a wide window. The tabs start at the top, level with the name of the page, and the profile stands apart at the bottom, as the account does in most desktop apps.
@Composable
internal fun SideNavRail(
    tabs: List<Destination>,
    selected: Destination,
    labels: NavLabels,
    avatar: NavAvatar?,
    badges: Map<Destination, Int>,
    onSelect: (Destination) -> Unit,
) {
    Surface(Modifier.fillMaxHeight().width(84.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier.fillMaxHeight().windowInsetsPadding(WindowInsets.safeDrawing).padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            for (tab in tabs) {
                if (tab == Destination.Profile) Spacer(Modifier.weight(1f))
                NavItem(
                    tab,
                    tab == selected,
                    avatar,
                    badges[tab] ?: 0,
                    labels,
                    wide = true,
                    onClick = { onSelect(tab) },
                )
            }
        }
    }
}
