package com.sumizuri.app.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.settings_new
import com.sumizuri.app.ui.kit.card.ListChevron
import com.sumizuri.app.ui.kit.setting.SettingsSection
import com.sumizuri.app.ui.theme.LocalThemeSpec
import org.jetbrains.compose.resources.stringResource

// The corners of a row of the list, round like the rows of the panels of the reader.
private val RowShape = RoundedCornerShape(16.dp)

// The list of the settings in the way of the panels of the reader: each group under a quiet name, and each page a plain row with its icon, its name and what it is set to, the open one lit. Beside an open page on a wide window there are no arrows, as the page opens there, and alone on a phone an arrow shows a row opens a page when the theme has them.
@Composable
internal fun NavGroups(pages: List<SettingsPage>, selected: SettingsTarget?, arrows: Boolean = false) {
    // Alone on a phone each group stands in a card, as the groups of the pages it opens do, so the list and its pages look the same.
    if (arrows) {
        for (group in SettingsGroup.entries) {
            val here = pages.filter { it.group == group }
            if (here.isEmpty()) continue
            SettingsSection(group.label) {
                for (page in here) {
                    row { NavRow(page.icon, page.title, page.summary, false, page.fresh, true, page.open) }
                }
            }
        }
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (group in SettingsGroup.entries) {
            val here = pages.filter { it.group == group }
            if (here.isEmpty()) continue
            Text(
                stringResource(group.label),
                Modifier.padding(start = 12.dp, top = 14.dp, bottom = 4.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            for (page in here) {
                NavRow(
                    page.icon,
                    page.title,
                    page.summary,
                    page.target == selected,
                    page.fresh,
                    arrows,
                    page.open,
                )
            }
        }
    }
}

@Composable
private fun NavRow(
    icon: ImageVector,
    title: String,
    summary: String,
    on: Boolean,
    fresh: Boolean,
    arrow: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (on) colors.primary.copy(alpha = 0.16f) else Color.Transparent)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(fill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = if (on) colors.primary else colors.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                color = if (on) colors.onSurface else colors.onSurface.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = colors.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (fresh) {
            Text(
                stringResource(Res.string.settings_new),
                Modifier.background(colors.primary, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimary,
            )
        }
        if (arrow && LocalThemeSpec.current.options.components.listChevron) ListChevron()
    }
}
