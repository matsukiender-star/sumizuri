package com.sumizuri.app.feature.migration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.migration.MigrationSession
import com.sumizuri.app.data.migration.MigrationState
import com.sumizuri.app.data.migration.MigrationStatus
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.mig_bar_move
import com.sumizuri.app.shared.resources.mig_bar_moving
import com.sumizuri.app.shared.resources.mig_bar_searching
import com.sumizuri.app.shared.resources.mig_bar_source_of
import com.sumizuri.app.shared.resources.mig_bar_start
import com.sumizuri.app.shared.resources.mig_bar_stop
import com.sumizuri.app.shared.resources.mig_bar_summary
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.art.InkLinearProgressIndicator
import com.sumizuri.app.ui.kit.art.InkOutlinedButton
import com.sumizuri.app.ui.kit.art.InkTonalButton
import com.sumizuri.app.ui.text.progressTimeText
import org.jetbrains.compose.resources.stringResource

// The bar under the page: what the work is doing and how far it has got, or a count of what was found, with the buttons to start or stop a search and to move what was found.
@Composable
internal fun MigrationBar(state: MigrationState, session: MigrationSession, onStarted: () -> Unit) {
    val open = state.items.count {
        it.included &&
            (
                it.status == MigrationStatus.Queued || it.status == MigrationStatus.NotFound ||
                    it.status == MigrationStatus.Failed
                )
    }
    val found = state.count(MigrationStatus.Found)
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.busy &&
                state.total > 0
            ) {
                InkLinearProgressIndicator(
                    progress = { state.processed.toFloat() / state.total },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val note = when {
                    state.running -> stringResource(
                        Res.string.mig_bar_searching,
                        state.currentSource.orEmpty(),
                        state.processed,
                        state.total,
                    )

                    state.moving -> stringResource(Res.string.mig_bar_moving, state.processed, state.total)

                    else -> stringResource(
                        Res.string.mig_bar_summary,
                        found,
                        state.count(MigrationStatus.Review),
                        state.count(MigrationStatus.NotFound),
                        open,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(note, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    // Which source of how many, and how long this one has run and has left, guessed from how fast it went so far.
                    if (state.busy) {
                        val time = progressTimeText(state.processed, state.total)
                        val where = if (state.running &&
                            state.sourceCount > 1
                        ) {
                            stringResource(Res.string.mig_bar_source_of, state.sourceIndex, state.sourceCount)
                        } else {
                            null
                        }
                        listOfNotNull(where, time).takeIf { it.isNotEmpty() }?.let {
                            Text(
                                it.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                            )
                        }
                    }
                }
                if (state.busy) {
                    InkOutlinedButton(session::stop) { Text(stringResource(Res.string.mig_bar_stop)) }
                } else {
                    InkTonalButton(
                        { session.moveFound() },
                        enabled = found > 0,
                    ) { Text(stringResource(Res.string.mig_bar_move, found)) }
                    InkButton({
                        session.start()
                        onStarted()
                    }, enabled = open > 0 && state.sources.any { it.included }) {
                        Text(
                            stringResource(Res.string.mig_bar_start, open),
                        )
                    }
                }
            }
        }
    }
}
