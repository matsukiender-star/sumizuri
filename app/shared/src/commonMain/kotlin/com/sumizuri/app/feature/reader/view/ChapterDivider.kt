package com.sumizuri.app.feature.reader.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.Chapter
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.feature.reader.ReaderSegment
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.chapters_missing
import com.sumizuri.app.shared.resources.reader_end_of
import com.sumizuri.app.ui.kit.page.PageGutter
import com.sumizuri.app.ui.text.numberedUnit
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// Where the scroll goes on from one chapter into the next: a quiet line with the chapter that ended on it, the name of the one that starts here, and a warning when chapters are missing between them.
@Composable
fun ChapterDivider(
    segment: ReaderSegment,
    chapters: List<Chapter>,
    kind: Kind,
    ink: Color,
    modifier: Modifier = Modifier,
) {
    val finished = chapters.firstOrNull { it.id == segment.previousId }
    val starting = chapters.firstOrNull { it.id == segment.chapterId }
    val missing = missingBetween(finished, starting)
    Column(
        modifier.fillMaxWidth().padding(horizontal = PageGutter, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (finished != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(Modifier.weight(1f), color = ink.copy(alpha = 0.2f))
                Text(
                    stringResource(Res.string.reader_end_of, chapterName(finished, kind)),
                    style = MaterialTheme.typography.labelMedium,
                    color = ink.copy(alpha = 0.6f),
                )
                HorizontalDivider(Modifier.weight(1f), color = ink.copy(alpha = 0.2f))
            }
        } else {
            HorizontalDivider(color = ink.copy(alpha = 0.2f))
        }
        Text(
            starting?.let { chapterName(it, kind) } ?: segment.name ?: numberedUnit(kind, segment.number),
            color = ink,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (missing > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Outlined.WarningAmber, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                Text(
                    pluralStringResource(Res.plurals.chapters_missing, missing, missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
