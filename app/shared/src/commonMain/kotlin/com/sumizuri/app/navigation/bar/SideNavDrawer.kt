package com.sumizuri.app.navigation.bar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumizuri.app.navigation.Destination
import com.sumizuri.app.ui.motion.AppMotion
import com.sumizuri.app.ui.motion.feedbackScale
import com.sumizuri.app.ui.motion.motionMs
import org.jetbrains.compose.resources.stringResource

// The wide panel down the side, with the name of every tab beside its icon.
@Composable
internal fun SideNavDrawer(
    tabs: List<Destination>,
    selected: Destination,
    avatar: NavAvatar?,
    badges: Map<Destination, Int>,
    onSelect: (Destination) -> Unit,
) {
    Surface(Modifier.fillMaxHeight().width(232.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            for (tab in tabs) DrawerRow(tab, tab == selected, avatar, badges[tab] ?: 0) { onSelect(tab) }
        }
    }
}

@Composable
private fun DrawerRow(
    destination: Destination,
    selected: Boolean,
    avatar: NavAvatar?,
    badge: Int,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = CircleShape
    val source = remember { MutableInteractionSource() }
    val ms = motionMs(AppMotion.Fast)
    val back by animateColorAsState(
        if (selected) colors.secondaryContainer else colors.secondaryContainer.copy(alpha = 0f),
        tween(ms),
        label = "row",
    )
    val tint by animateColorAsState(
        if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
        tween(ms),
        label = "tint",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .feedbackScale(source)
            .background(back, shape)
            .clickable(interactionSource = source, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NavIcon(destination, selected, avatar, tint, badge = badge)
        Text(
            stringResource(destination.label),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
        )
    }
}
