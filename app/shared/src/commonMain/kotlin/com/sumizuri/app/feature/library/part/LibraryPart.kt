package com.sumizuri.app.feature.library.part

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.FeedItem
import com.sumizuri.app.data.model.library.LibrarySection
import com.sumizuri.app.data.model.library.ShelfStyle
import com.sumizuri.app.feature.library.LibraryCalls
import com.sumizuri.app.feature.library.LibraryState
import com.sumizuri.app.feature.library.tile.scaled
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.display_hide_row
import com.sumizuri.app.shared.resources.display_see_all
import com.sumizuri.app.ui.kit.card.AppCard
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import com.sumizuri.app.ui.kit.cover.CoverArt
import com.sumizuri.app.ui.kit.scroll.mouseDragScroll
import com.sumizuri.app.ui.motion.entrance
import com.sumizuri.app.ui.motion.feedbackScale
import com.sumizuri.app.ui.text.displayName
import com.sumizuri.app.ui.text.numberedUnit
import org.jetbrains.compose.resources.stringResource

// One part of the top of the library, with its heading, a link to the whole page and a button that hides it.
@Composable
internal fun LibraryPartOf(
    state: LibraryState,
    calls: LibraryCalls,
    section: LibrarySection,
    items: List<FeedItem>,
    onHide: (LibrarySection) -> Unit,
) {
    val prefs = state.prefs
    LibraryPart(
        section,
        items,
        prefs.styleOf(section),
        prefs.homeCover,
        prefs.nameScale,
        calls.onOpenTitle,
        onSeeAll = { calls.onSeeAll(section) },
        onHide = { onHide(section) },
        onContinue = calls.onContinue,
    )
}

// One part: its name with a link to the whole page and a button that hides it, and under them the titles in a row, in a grid or as lines.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryPart(
    section: LibrarySection,
    items: List<FeedItem>,
    style: ShelfStyle,
    cover: Int,
    scale: Float,
    onOpen: (Long) -> Unit,
    onSeeAll: () -> Unit,
    onHide: (() -> Unit)?,
    onContinue: (Long) -> Unit = onOpen,
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { AppSectionLabel(stringResource(section.label)) }
            TextButton(
                onSeeAll,
            ) { Text(stringResource(Res.string.display_see_all), style = MaterialTheme.typography.labelMedium) }
            if (onHide !=
                null
            ) {
                IconButton(onHide, Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Close, stringResource(Res.string.display_hide_row), Modifier.size(18.dp))
                }
            }
        }
        when (style) {
            ShelfStyle.Row -> {
                val row = rememberLazyListState()
                LazyRow(
                    Modifier.mouseDragScroll(row, horizontal = true),
                    state = row,
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items, key = { it.title.id }) { PartCell(it, cover, scale, onOpen) }
                }
            }

            ShelfStyle.Grid -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (item in items) PartCell(item, cover, scale, onOpen)
            }

            ShelfStyle.List -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (item in items) PartLine(item, cover, scale, onOpen)
            }

            ShelfStyle.Pages -> {
                val row = rememberLazyListState()
                LazyRow(
                    Modifier.mouseDragScroll(row, horizontal = true),
                    state = row,
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items, key = { it.title.id }) { PageCard(it, scale, onOpen, onContinue) }
                }
            }

            ShelfStyle.Cards -> {
                val row = rememberLazyListState()
                LazyRow(
                    Modifier.mouseDragScroll(row, horizontal = true),
                    state = row,
                    contentPadding = PaddingValues(0.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items, key = { it.title.id }) { PartCard(it, scale, onOpen) }
                }
            }
        }
    }
}

// A low card with a small cover, the name and the chapter, about as wide as a phone shows with the next one peeking in. The profile shows what is being read with it too.
@Composable
internal fun PartCard(item: FeedItem, scale: Float, onOpen: (Long) -> Unit) {
    val source = remember { MutableInteractionSource() }
    AppCard(Modifier.width(CardWidth).entrance().feedbackScale(source), onClick = { onOpen(item.title.id) }) {
        Row(
            Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CoverArt(item.title, Modifier.width(44.dp), letterSize = 14)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    item.title.displayName(),
                    style = MaterialTheme.typography.bodyMedium.scaled(scale),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    numberedUnit(item.title.kind, item.number),
                    style = MaterialTheme.typography.labelMedium.scaled(scale),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
        }
    }
}

private val CardWidth = 260.dp

// A cover with the name under it and the chapter it is about.
@Composable
private fun PartCell(item: FeedItem, width: Int, scale: Float, onOpen: (Long) -> Unit) {
    val source = remember { MutableInteractionSource() }
    Column(
        Modifier
            .width(width.dp)
            .entrance()
            .feedbackScale(source)
            .clickable(interactionSource = source, indication = null) { onOpen(item.title.id) },
    ) {
        CoverArt(item.title, Modifier.fillMaxWidth(), letterSize = 26)
        Text(
            item.title.displayName(),
            Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodySmall.scaled(scale),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            numberedUnit(item.title.kind, item.number),
            style = MaterialTheme.typography.labelSmall.scaled(scale),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
    }
}

// A thin line with a small cover, the name and the chapter.
@Composable
private fun PartLine(item: FeedItem, cover: Int, scale: Float, onOpen: (Long) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable {
            onOpen(item.title.id)
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoverArt(item.title, Modifier.width((cover * 0.4f).dp), letterSize = 14)
        Column(Modifier.weight(1f)) {
            Text(
                item.title.displayName(),
                style = MaterialTheme.typography.bodyMedium.scaled(scale),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                numberedUnit(item.title.kind, item.number),
                style = MaterialTheme.typography.labelSmall.scaled(scale),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}
