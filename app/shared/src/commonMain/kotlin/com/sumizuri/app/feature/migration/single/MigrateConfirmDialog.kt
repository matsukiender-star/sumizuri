package com.sumizuri.app.feature.migration.single

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.migrate_checking
import com.sumizuri.app.shared.resources.migrate_confirm_title
import com.sumizuri.app.shared.resources.migrate_counts
import com.sumizuri.app.shared.resources.migrate_failed
import com.sumizuri.app.shared.resources.migrate_keep_old
import com.sumizuri.app.shared.resources.migrate_move
import com.sumizuri.app.shared.resources.migrate_plan
import com.sumizuri.app.shared.resources.migrate_plan_lost
import com.sumizuri.app.shared.resources.migrate_plan_none
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import com.sumizuri.app.ui.kit.cover.shownName
import com.sumizuri.app.ui.kit.dialog.AppDialog
import com.sumizuri.app.ui.kit.dialog.CancelButton
import org.jetbrains.compose.resources.stringResource

// Says what moving to the picked entry carries over, and asks for a yes.
@Composable
fun MigrateConfirmDialog(state: MigrateState, onToggleKeep: () -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val choice = state.picked ?: return
    val plan = choice.plan
    AppDialog(
        onDismissRequest = { if (!state.working) onDismiss() },
        title = { Text(stringResource(Res.string.migrate_confirm_title, choice.source.shownName())) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(choice.entry.title, fontWeight = FontWeight.SemiBold)
                when {
                    choice.error != null -> Text(choice.error, color = MaterialTheme.colorScheme.error)

                    plan == null -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        InkCircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(stringResource(Res.string.migrate_checking))
                    }

                    else -> {
                        val carried = if (plan.marked ==
                            0
                        ) {
                            stringResource(Res.string.migrate_plan_none)
                        } else {
                            stringResource(Res.string.migrate_plan, plan.marked, plan.matched)
                        }
                        Text(carried)
                        if (plan.matched < plan.marked) {
                            Text(
                                stringResource(Res.string.migrate_plan_lost, plan.marked - plan.matched),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Text(
                            stringResource(Res.string.migrate_counts, plan.newChapters, plan.oldChapters),
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                Row(Modifier.clickable(onClick = onToggleKeep), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(state.keepOld, { onToggleKeep() })
                    Text(stringResource(Res.string.migrate_keep_old), Modifier.padding(start = 4.dp))
                }
                state.problem?.let {
                    Text(
                        stringResource(Res.string.migrate_failed, it),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            InkButton(
                onConfirm,
                enabled = plan != null && !state.working,
            ) { Text(stringResource(Res.string.migrate_move)) }
        },
        dismissButton = { CancelButton(onDismiss, enabled = !state.working) },
    )
}
