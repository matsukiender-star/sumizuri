package com.sumizuri.app.feature.reader.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.Chapter
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.chapters_missing
import com.sumizuri.app.shared.resources.reader_caught_up
import com.sumizuri.app.shared.resources.reader_end_of
import com.sumizuri.app.shared.resources.reader_next
import com.sumizuri.app.shared.resources.reader_previous
import com.sumizuri.app.ui.kit.page.PageGutter
import com.sumizuri.app.ui.text.numberedUnit
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.floor

// The widest the cards at the end of a chapter grow.
private val EndWidth = 480.dp

// The end of a chapter: which chapter just ended, a large card for the next one with a warning when chapters are missing between them, or a word that the reader is caught up, and a small way back to the chapter before.
@Composable
fun ChapterEnd(
    chapters: List<Chapter>,
    kind: Kind,
    finishedId: Long,
    previousId: Long?,
    nextId: Long?,
    ink: Color,
    onOpenChapter: (Long) -> Unit,
) {
    val finished = chapters.firstOrNull { it.id == finishedId }
    val next = chapters.firstOrNull { it.id == nextId }
    Column(
        Modifier.fillMaxWidth().padding(PageGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (finished != null) {
            Text(
                stringResource(Res.string.reader_end_of, chapterName(finished, kind)),
                style = MaterialTheme.typography.titleMedium,
                color = ink.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
        }
        if (nextId != null) {
            NextCard(next, nextId, missingBetween(finished, next), kind, onOpenChapter)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(Res.string.reader_caught_up),
                    style = MaterialTheme.typography.titleSmall,
                    color = ink,
                )
            }
        }
        previousId?.let { id ->
            val before = chapters.firstOrNull { it.id == id }
            TextButton({ onOpenChapter(id) }) {
                Icon(Icons.Outlined.SkipPrevious, null, Modifier.size(18.dp))
                Text(
                    before?.let { "${stringResource(Res.string.reader_previous)}: ${chapterName(it, kind)}" }
                        ?: stringResource(Res.string.reader_previous),
                    Modifier.padding(start = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// The card that opens the next chapter: a small word that it is next, its name, and a warning when chapters are missing before it.
@Composable
internal fun NextCard(next: Chapter?, nextId: Long, missing: Int, kind: Kind, onOpenChapter: (Long) -> Unit) {
    Surface(
        { onOpenChapter(nextId) },
        Modifier.widthIn(max = EndWidth).fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(Res.string.reader_next),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    next?.let { chapterName(it, kind) } ?: stringResource(Res.string.reader_next),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (missing > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Outlined.WarningAmber,
                            null,
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            pluralStringResource(Res.plurals.chapters_missing, missing, missing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// The number and the name of a chapter, or the number alone when it has no name of its own.
@Composable
internal fun chapterName(chapter: Chapter, kind: Kind): String {
    val numbered = numberedUnit(kind, chapter.number)
    val name = chapter.name?.takeIf { it.isNotBlank() } ?: return numbered
    return if (name.contains(numbered, ignoreCase = true)) name else "$numbered · $name"
}

// How many whole numbers are skipped between two chapters, such as 3 when chapter 5 follows chapter 1. A part such as 5.5 counts as no gap.
internal fun missingBetween(before: Chapter?, after: Chapter?): Int {
    if (before == null || after == null || before.number < 0 || after.number < 0) return 0
    return (floor(after.number) - floor(before.number) - 1).toInt().coerceAtLeast(0)
}
