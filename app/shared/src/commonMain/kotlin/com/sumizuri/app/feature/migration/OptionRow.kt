package com.sumizuri.app.feature.migration

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.migration.MigrationOption
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.mig_option_detail
import com.sumizuri.app.ui.kit.cover.shownName
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

// One place a title could move to: the name it has there, the source, how many chapters and who wrote it, and how well it fits as a percent. The dot says whether it is the one chosen.
@Composable
internal fun OptionRow(option: MigrationOption, chosen: Boolean, onChoose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onChoose),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RadioButton(chosen, null)
        Column(Modifier.weight(1f)) {
            Text(
                option.entry.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // The count of chapters is left out when the search did not read them.
            val where =
                option.chapterCount?.let { stringResource(Res.string.mig_option_detail, option.source.shownName(), it) }
                    ?: option.source.shownName()
            Text(
                where + (
                    option.entry.author
                        ?.takeIf { it.isNotBlank() }
                        ?.let { " · $it" } ?: ""
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            "${(option.score.total * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
