package com.sumizuri.app.feature.player.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.kit.card.CardPlace
import com.sumizuri.app.ui.kit.card.cardRow
import com.sumizuri.app.ui.kit.card.cardRowShape

// One line of a choice, as a row of the card the lines share: an icon in the color of the theme, the name and a note under it, and on the right a tick when it is the one chosen, or the value it is set to with an arrow for a line that opens more choices. The chosen line is tinted.
@Composable
internal fun PickerRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    place: CardPlace,
    subtitle: String? = null,
    selected: Boolean = false,
    value: String? = null,
    opens: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .cardRow(place, tint = if (selected) scheme.primary.copy(alpha = 0.12f) else null)
            .clip(cardRowShape(place))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = scheme.primary)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else null,
                color = if (selected) scheme.primary else scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.outline)
        }
        if (value !=
            null
        ) {
            Text(
                value,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 120.dp),
            )
        }
        if (opens) Icon(Icons.Outlined.ChevronRight, null, tint = scheme.onSurfaceVariant)
        if (selected && !opens) Icon(Icons.Outlined.Check, null, tint = scheme.primary)
    }
}
