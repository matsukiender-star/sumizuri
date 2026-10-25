package com.sumizuri.app.feature.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumizuri.app.ui.kit.art.StretchDot
import com.sumizuri.app.ui.motion.AppMotion
import com.sumizuri.app.ui.motion.motionMs
import org.jetbrains.compose.resources.stringResource

// The narrowest a tab gets before the row scrolls, enough for its name.
private val SmallestTab = 52.dp

// The tabs of the editor as icons with their names, side by side across the whole width like the bar at the bottom of the app, so every tab is in sight at once when there is room.
@Composable
internal fun SectionTabs(selected: EditorSection, onSelect: (EditorSection) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // The tabs share the width, and a width too narrow for all of them at their smallest scrolls sideways instead.
        val each = maxOf(maxWidth / EditorSection.entries.size, SmallestTab)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            for (section in EditorSection.entries) {
                val on = section == selected
                val requester = remember { BringIntoViewRequester() }
                // The open tab scrolls into sight on a screen too narrow for all of them.
                LaunchedEffect(on) { if (on) requester.bringIntoView() }
                val tint by animateColorAsState(
                    if (on) colors.primary else colors.onSurfaceVariant,
                    tween(motionMs(AppMotion.Fast)),
                    label = "section tint",
                )
                Column(
                    Modifier
                        .width(each)
                        .bringIntoViewRequester(requester)
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(on, role = Role.Tab) { onSelect(section) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(section.icon, null, Modifier.size(22.dp), tint = tint)
                    Text(
                        stringResource(section.label),
                        Modifier.padding(horizontal = 2.dp),
                        color = tint,
                        style = MaterialTheme.typography.labelSmall,
                        // Without the usual spacing between letters, the longer names still fit a narrow tab.
                        letterSpacing = 0.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    StretchDot(on, idle = 0.dp, wide = 16.dp, height = 3.dp)
                }
            }
        }
    }
}
