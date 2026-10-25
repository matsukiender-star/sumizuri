package com.sumizuri.app.feature.reader.controls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.Chapter
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.text.numberText
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.all
import com.sumizuri.app.shared.resources.bookmark_remove
import com.sumizuri.app.shared.resources.filter_unread
import com.sumizuri.app.shared.resources.reader_chapter_read
import com.sumizuri.app.shared.resources.reader_chapters
import com.sumizuri.app.shared.resources.reader_chapters_read
import com.sumizuri.app.shared.resources.reader_filter_bookmarked
import com.sumizuri.app.shared.resources.reader_reading_now
import com.sumizuri.app.ui.kit.art.InkLinearProgressIndicator
import com.sumizuri.app.ui.kit.card.CardPlace
import com.sumizuri.app.ui.kit.card.cardRow
import com.sumizuri.app.ui.kit.card.cardRowShape
import com.sumizuri.app.ui.kit.dock.PanelHead
import com.sumizuri.app.ui.kit.page.PageGutter
import com.sumizuri.app.ui.kit.setting.PartTabs
import com.sumizuri.app.ui.text.agoText
import com.sumizuri.app.ui.text.numberedUnit
import org.jetbrains.compose.resources.stringResource

// Which chapters the list shows.
private enum class ChapterShow { All, Unread, Bookmarked }

// Every chapter of the title as a panel the dock grows to hold, the latest first in the order the title page uses, and opened at the one being read. The head says how far the title is read with a bar under it, tabs narrow the list to the unread or the bookmarked, and each row leads with its number so the eye can run down them. A tap goes straight to that chapter.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersPanel(chapters: List<Chapter>, kind: Kind, currentId: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    var show by rememberSaveable { mutableStateOf(ChapterShow.All) }
    val all = remember(chapters) { chapters.asReversed() }
    val rows = remember(all, show) {
        when (show) {
            ChapterShow.All -> all
            ChapterShow.Unread -> all.filter { !it.read || it.id == currentId }
            ChapterShow.Bookmarked -> all.filter { it.bookmarked }
        }
    }
    val list =
        rememberLazyListState(
            initialFirstVisibleItemIndex = (rows.indexOfFirst { it.id == currentId } - 2).coerceAtLeast(0),
        )
    val read = chapters.count { it.read }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PanelHead(stringResource(Res.string.reader_chapters), onDismiss, trailing = {
            Text(
                stringResource(Res.string.reader_chapters_read, read, chapters.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        })
        Column(Modifier.padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InkLinearProgressIndicator(
                { if (chapters.isEmpty()) 0f else read.toFloat() / chapters.size },
                Modifier.fillMaxWidth().height(4.dp).clip(MaterialTheme.shapes.small),
            )
            PartTabs(
                ChapterShow.entries,
                show,
                {
                    stringResource(
                        when (it) {
                            ChapterShow.All -> Res.string.all
                            ChapterShow.Unread -> Res.string.filter_unread
                            ChapterShow.Bookmarked -> Res.string.reader_filter_bookmarked
                        },
                    )
                },
                Modifier.padding(top = 6.dp),
            ) { show = it }
        }
        LazyColumn(
            Modifier.weight(1f, fill = false),
            state = list,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        ) {
            itemsIndexed(rows, key = { _, chapter -> chapter.id }) { index, chapter ->
                ChapterChoice(chapter, kind, now = chapter.id == currentId, CardPlace(index, rows.size)) {
                    onDismiss()
                    if (chapter.id != currentId) onPick(chapter.id)
                }
            }
        }
    }
}

// One chapter: its number in a column of its own, its name with a note under it, and its bookmark. The open chapter is tinted in the accent, and read ones are quieter.
@Composable
private fun ChapterChoice(chapter: Chapter, kind: Kind, now: Boolean, place: CardPlace, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val meta = listOfNotNull(
        when {
            now -> stringResource(Res.string.reader_reading_now)
            chapter.read -> stringResource(Res.string.reader_chapter_read)
            else -> null
        },
        chapter.publishedAt.takeIf { it > 0 }?.let { agoText(it) },
    ).joinToString("  ·  ").ifEmpty { null }
    val muted = chapter.read && !now
    val ink = when {
        now -> colors.onPrimaryContainer
        muted -> colors.outline
        else -> colors.onSurface
    }
    Row(
        Modifier
            .fillMaxWidth()
            .cardRow(place, tint = if (now) colors.primaryContainer else null)
            .clip(cardRowShape(place))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            chapter.number.takeIf { it >= 0 }?.numberText() ?: "·",
            Modifier.widthIn(min = 40.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (now) {
                colors.onPrimaryContainer
            } else if (muted) {
                colors.outline
            } else {
                colors.primary
            },
        )
        Column(Modifier.weight(1f)) {
            Text(
                chapter.name?.takeIf { it.isNotBlank() } ?: numberedUnit(kind, chapter.number),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (now) FontWeight.Bold else FontWeight.Medium,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta != null) {
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (now) colors.onPrimaryContainer else colors.outline,
                    maxLines = 1,
                )
            }
        }
        if (chapter.bookmarked) {
            Icon(
                Icons.Filled.Bookmark,
                stringResource(Res.string.bookmark_remove),
                Modifier.size(20.dp),
                tint = colors.primary,
            )
        }
    }
}
