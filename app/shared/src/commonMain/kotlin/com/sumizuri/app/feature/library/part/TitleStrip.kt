package com.sumizuri.app.feature.library.part

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.FeedItem
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import com.sumizuri.app.ui.kit.scroll.mouseDragScroll
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// A row of low cards under a heading, each with a small cover and the chapter it is about, the same cards the library shows what is being read with.
@Composable
internal fun TitleStrip(heading: StringResource, items: List<FeedItem>, onOpen: (Long) -> Unit) {
    if (items.isEmpty()) return
    Column {
        AppSectionLabel(stringResource(heading))
        val row = rememberLazyListState()
        LazyRow(
            Modifier.mouseDragScroll(row, horizontal = true),
            state = row,
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.title.id }) { item -> PartCard(item, 1f, onOpen) }
        }
    }
}
