package com.sumizuri.app.feature.title.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.SourceState
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.feature.title.TitleCallbacks
import com.sumizuri.app.feature.title.TitleState
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.migrate_title
import com.sumizuri.app.shared.resources.source_missing_notice
import com.sumizuri.app.shared.resources.source_off_notice
import com.sumizuri.app.shared.resources.source_turn_on
import com.sumizuri.app.shared.resources.title_read_progress
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.art.InkOutlinedButton
import com.sumizuri.app.ui.kit.cover.sourceName
import org.jetbrains.compose.resources.stringResource

// What stands where the continue button would be on a title whose source cannot be reached: how many chapters are read, which is what the page can still tell, a line that says why the chapters do not open, and the way out. A source that is off is turned on, and one that is not installed is replaced by moving the title to another.
@Composable
internal fun UnreachableActions(state: TitleState, title: Title, calls: TitleCallbacks, modifier: Modifier = Modifier) {
    val off = title.sourceState == SourceState.Off
    val read = state.counted.count { it.read }
    // These lines often stand over the faded cover, so they are a step brighter than the usual quiet grey.
    val quiet = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.chapters.isNotEmpty()) {
            Text(
                stringResource(Res.string.title_read_progress, read, state.counted.size),
                style = MaterialTheme.typography.labelLarge,
                color = quiet,
            )
        }
        Text(
            stringResource(
                if (off) Res.string.source_off_notice else Res.string.source_missing_notice,
                sourceName(state.title?.sourceId, state.sourceName.orEmpty()),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = quiet,
        )
        if (off) {
            InkButton(calls.onEnableSource, Modifier.fillMaxWidth()) { Text(stringResource(Res.string.source_turn_on)) }
            InkOutlinedButton(
                calls.onMigrate,
                Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.migrate_title)) }
        } else {
            InkButton(calls.onMigrate, Modifier.fillMaxWidth()) { Text(stringResource(Res.string.migrate_title)) }
        }
    }
}
