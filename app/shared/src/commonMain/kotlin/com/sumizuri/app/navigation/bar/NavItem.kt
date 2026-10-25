package com.sumizuri.app.navigation.bar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sumizuri.app.navigation.Destination
import com.sumizuri.app.ui.kit.art.StretchDot
import com.sumizuri.app.ui.motion.AppMotion
import com.sumizuri.app.ui.motion.feedbackScale
import com.sumizuri.app.ui.motion.motionMs
import org.jetbrains.compose.resources.stringResource

// One tab of the bottom bar or the rail: the icon, which takes the accent and lifts when the tab is chosen, and under it the name with a small bar that stretches out.
@Composable
internal fun NavItem(
    destination: Destination,
    selected: Boolean,
    avatar: NavAvatar?,
    badge: Int,
    labels: NavLabels,
    wide: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val ms = motionMs(AppMotion.Fast)
    val lift by animateFloatAsState(if (selected) 1.1f else 1f, tween(ms, easing = AppMotion.Entrance), label = "lift")
    // The chosen tab takes the accent colour and lifts a little, with a small bar stretching out under it.
    val tint by animateColorAsState(
        if (selected) colors.primary else colors.onSurfaceVariant,
        tween(ms),
        label = "tint",
    )
    Column(
        modifier
            .feedbackScale(
                source,
            ).clickable(interactionSource = source, indication = null, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.width(if (wide) 56.dp else 64.dp).height(32.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.graphicsLayer {
                    scaleX = lift
                    scaleY = lift
                },
            ) { NavIcon(destination, selected, avatar, tint, badge = badge) }
        }
        if (labels == NavLabels.Always || (labels == NavLabels.Selected && selected)) {
            NavLabel(
                stringResource(destination.label),
                selected,
                if (selected) colors.primary else colors.onSurfaceVariant,
            )
        } else {
            // Without a name, the bar sits under the icon.
            StretchDot(selected, idle = 0.dp, wide = 18.dp, height = 4.dp)
        }
    }
}
