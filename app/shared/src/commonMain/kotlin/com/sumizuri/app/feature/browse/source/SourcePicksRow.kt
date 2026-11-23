package com.sumizuri.app.feature.browse.source

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.feature.browse.asTitle
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.listing_popular
import com.sumizuri.app.shared.resources.suki_browse_picks_read
import com.sumizuri.app.shared.resources.suki_browse_picks_watched
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import com.sumizuri.app.ui.kit.card.ListChevron
import com.sumizuri.app.ui.kit.cover.CoverPick
import com.sumizuri.app.ui.kit.cover.CoverTile
import com.sumizuri.app.ui.kit.scroll.ArrowRow
import com.sumizuri.app.ui.kit.scroll.mouseDragScroll
import org.jetbrains.compose.resources.stringResource

private val PickWidth = 116.dp

// More of the genre of the last title seen here, with a heading that opens all of that genre, over the popular list.
@Composable
internal fun SourcePicksRow(
    picks: SourcePicks,
    state: SourceBrowseState,
    actions: SourceBrowseActions,
    // The covers picked on the page, which a long press here adds to as well.
    picked: Set<String>,
    saving: Set<String>,
    onPick: (SourceEntry) -> Unit,
) {
    val because = if (state.source.kind ==
        Kind.Anime
    ) {
        Res.string.suki_browse_picks_watched
    } else {
        Res.string.suki_browse_picks_read
    }
    val row = rememberLazyListState()
    Column {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button) { actions.onOpenGenre(picks.filters) },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppSectionLabel(stringResource(because, picks.base, picks.genre), Modifier.weight(1f))
            ListChevron()
        }
        ArrowRow(row) {
            LazyRow(
                Modifier.mouseDragScroll(row, horizontal = true),
                state = row,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(picks.entries, key = { it.url }) { entry ->
                    val saved = entry.url in state.inLibrary || entry.url in saving
                    CoverTile(
                        entry.asTitle(state.source.kind, state.source.id),
                        onClick = { if (picked.isEmpty()) actions.onOpenEntry(entry) else onPick(entry) },
                        modifier = Modifier.width(PickWidth),
                        saved = saved,
                        pick = CoverPick(picked.isNotEmpty(), entry.url in picked),
                        onLongClick = { onPick(entry) },
                    )
                }
            }
        }
        AppSectionLabel(Res.string.listing_popular)
    }
}
