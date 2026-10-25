package com.sumizuri.app.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.profile.Profile
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.profile_active
import com.sumizuri.app.shared.resources.profile_edit
import com.sumizuri.app.shared.resources.profile_new
import com.sumizuri.app.shared.resources.profile_not_found
import com.sumizuri.app.shared.resources.profile_search_hint
import com.sumizuri.app.shared.resources.profile_switch
import com.sumizuri.app.ui.kit.card.AppListRow
import com.sumizuri.app.ui.kit.input.AppSearchBar
import com.sumizuri.app.ui.kit.page.AppSheet
import org.jetbrains.compose.resources.stringResource

// Everyone who uses this device, each as a card. A tap switches to one, and its pencil edits it. A search shows when there are many.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileSwitchSheet(
    profiles: List<Profile>,
    active: Profile?,
    onSwitch: (Long) -> Unit,
    onEdit: (Profile) -> Unit,
    onNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val shown = profiles.filter { it.name.contains(query.trim(), ignoreCase = true) }
    AppSheet(onDismiss, spacing = 8.dp, title = stringResource(Res.string.profile_switch)) {
        if (profiles.size > 4) AppSearchBar(query, { query = it }, stringResource(Res.string.profile_search_hint))
        if (shown.isEmpty()) {
            Text(
                stringResource(Res.string.profile_not_found),
                Modifier.fillMaxWidth().padding(vertical = 24.dp),
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
        }
        for (profile in shown) {
            ProfileCard(profile, profile.id == active?.id, onPick = {
                if (profile.id != active?.id) onSwitch(profile.id)
                onDismiss()
            }, onEdit = { onEdit(profile) })
        }
        AppListRow(
            stringResource(Res.string.profile_new),
            leading = { Icon(Icons.Outlined.PersonAdd, null, tint = MaterialTheme.colorScheme.primary) },
            onClick = onNew,
        )
    }
}

@Composable
private fun ProfileCard(profile: Profile, isActive: Boolean, onPick: () -> Unit, onEdit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onPick,
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainerHigh,
        border = BorderStroke(1.dp, if (isActive) colors.primary.copy(alpha = 0.6f) else colors.outlineVariant),
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProfileAvatar(profile.name, profile.avatar, 42.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    profile.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isActive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(14.dp), tint = colors.primary)
                        Text(
                            stringResource(Res.string.profile_active),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                        )
                    }
                }
            }
            IconButton(onEdit) {
                Icon(Icons.Outlined.Edit, stringResource(Res.string.profile_edit), tint = colors.onSurfaceVariant)
            }
        }
    }
}
