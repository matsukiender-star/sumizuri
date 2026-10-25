package com.sumizuri.app.feature.title.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.feature.title.TitleCallbacks
import com.sumizuri.app.feature.title.TitleState
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.migrate_title
import com.sumizuri.app.shared.resources.source_broken_notice
import com.sumizuri.app.ui.kit.art.InkOutlinedButton
import com.sumizuri.app.ui.kit.card.AppCard
import com.sumizuri.app.ui.motion.entrance
import org.jetbrains.compose.resources.stringResource

// Says that a title whose source can be reached does not work, since its address may have changed, and offers to move it. A title whose source cannot be reached has its own part of the page, so this shows nothing for it.
@Composable
internal fun SourceNotice(state: TitleState, calls: TitleCallbacks) {
    if (state.preview || !state.readable || state.error == null || state.loading) return
    AppCard(Modifier.entrance().fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.source_broken_notice), style = MaterialTheme.typography.bodyMedium)
            InkOutlinedButton(calls.onMigrate) { Text(stringResource(Res.string.migrate_title)) }
        }
    }
}
