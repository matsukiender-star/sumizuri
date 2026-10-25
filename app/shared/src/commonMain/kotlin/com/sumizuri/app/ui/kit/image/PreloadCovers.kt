package com.sumizuri.app.ui.kit.image

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.data.net.SiteKinds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

// How many covers past the last one on the screen are loaded ahead.
private const val Ahead = 24

// How long the grid must rest on a row before the covers past it are asked for, so a quick fling does not ask for all it passes.
private const val RestMs = 180L

// Loads the covers a little past the bottom of a grid of titles, at the size its cells show them, so they are in the memory when they scroll in. The cells of the titles are found by their keys, which are the ids of the titles.
@Composable
fun PreloadCovers(state: LazyGridState, titles: List<Title>) {
    val context = LocalPlatformContext.current
    LaunchedEffect(state, titles) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull { it.key is Long } }
            .distinctUntilChanged { old, new -> old?.key == new?.key }
            .collectLatest { last ->
                val id = last?.key as? Long ?: return@collectLatest
                val from = titles.indexOfFirst { it.id == id }
                if (from < 0) return@collectLatest
                delay(RestMs)
                val loader = SingletonImageLoader.get(context)
                for (title in titles.drop(from + 1).take(Ahead)) {
                    val url = title.coverUrl?.takeIf { it.isNotBlank() } ?: continue
                    SiteKinds.markFor(url, title.sourceId)
                    val referer = SourceReferers.of(title.sourceId)?.let { mapOf("Referer" to it) }.orEmpty()
                    val request = buildImageRequest(
                        context,
                        url,
                        referer,
                    ).newBuilder().size(last.size.width, last.size.height).build()
                    loader.enqueue(request)
                }
            }
    }
}
