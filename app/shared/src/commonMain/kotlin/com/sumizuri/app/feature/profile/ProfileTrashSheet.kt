package com.sumizuri.app.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.profile.Profile
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.suki_trash_days
import com.sumizuri.app.shared.resources.suki_trash_delete_now
import com.sumizuri.app.shared.resources.suki_trash_delete_now_message
import com.sumizuri.app.shared.resources.suki_trash_delete_now_title
import com.sumizuri.app.shared.resources.suki_trash_deleting
import com.sumizuri.app.shared.resources.suki_trash_restore
import com.sumizuri.app.shared.resources.suki_trash_title
import com.sumizuri.app.shared.resources.suki_trash_today
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.dialog.AppDialog
import com.sumizuri.app.ui.kit.dialog.CancelButton
import com.sumizuri.app.ui.kit.page.AppSheet
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// The deleted profiles with the days each has left. It closes by itself once none is left.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileTrashSheet(
    trash: List<Profile>,
    onRestore: (Long) -> Unit,
    onDeleteNow: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var confirming by remember { mutableStateOf<Profile?>(null) }
    val now = remember(trash) { nowMillis() }
    LaunchedEffect(trash.isEmpty()) { if (trash.isEmpty()) onDismiss() }
    AppSheet(onDismiss, spacing = 8.dp, title = stringResource(Res.string.suki_trash_title)) {
        for (profile in trash.sortedBy { it.deletedAt }) {
            TrashCard(profile, now, onRestore = { onRestore(profile.id) }, onDelete = { confirming = profile })
        }
    }
    confirming?.let { doomed ->
        AppDialog(
            onDismissRequest = { confirming = null },
            title = { Text(stringResource(Res.string.suki_trash_delete_now_title, doomed.name)) },
            text = { Text(stringResource(Res.string.suki_trash_delete_now_message)) },
            confirmButton = {
                InkButton({
                    confirming = null
                    onDeleteNow(doomed.id)
                }) { Text(stringResource(Res.string.suki_trash_delete_now), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { CancelButton({ confirming = null }) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrashCard(profile: Profile, now: Long, onRestore: () -> Unit, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ended = profile.trashEnded(now)
    val left = profile.trashDaysLeft(now)
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainerHigh,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(start = 12.dp, top = 10.dp, end = 4.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ProfileAvatar(profile.name, profile.avatar, 42.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        when {
                            ended -> stringResource(Res.string.suki_trash_deleting)
                            left == 0 -> stringResource(Res.string.suki_trash_today)
                            else -> pluralStringResource(Res.plurals.suki_trash_days, left, left)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ended || left == 0) colors.error else colors.onSurfaceVariant,
                    )
                }
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (!ended) TextButton(onRestore) { Text(stringResource(Res.string.suki_trash_restore)) }
                TextButton(onDelete) { Text(stringResource(Res.string.suki_trash_delete_now), color = colors.error) }
            }
        }
    }
}
