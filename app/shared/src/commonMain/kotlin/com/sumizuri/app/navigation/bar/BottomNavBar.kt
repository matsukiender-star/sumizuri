package com.sumizuri.app.navigation.bar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.navigation.Destination

// The bar along the bottom edge, with every tab the same width.
@Composable
internal fun BottomNavBar(tabs: List<Destination>, selected: Destination, labels: NavLabels, avatar: NavAvatar?, badges: Map<Destination, Int>, onSelect: (Destination) -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(vertical = 8.dp)) {
                for (tab in tabs) NavItem(tab, tab == selected, avatar, badges[tab] ?: 0, labels, wide = false, onClick = { onSelect(tab) }, modifier = Modifier.weight(1f))
            }
        }
    }
}
