package com.sumizuri.app.feature.reader.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.sumizuri.app.ui.kit.image.PageImages
import com.sumizuri.app.ui.kit.image.buildImageRequest

// How big each small page is, and the room between them.
private val ThumbWidth = 46.dp
private val ThumbHeight = 64.dp
private val ThumbGap = 6.dp

// A row of small pictures of the pages, the one in view outlined in the accent and the rest a little faded, each with its number. A tap goes to that page, and the row keeps the page in view near its middle. Only the pictures in sight are loaded, at their small size.
@Composable
fun PageStrip(
    page: Int,
    total: Int,
    preview: (Int) -> PagePreview?,
    onJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val list = rememberLazyListState(initialFirstVisibleItemIndex = (page - 3).coerceAtLeast(0))
    val step = with(LocalDensity.current) { (ThumbWidth + ThumbGap).roundToPx() }
    LaunchedEffect(page) {
        val room = list.layoutInfo.viewportSize.width
        val middle = ((room - step) / 2).coerceAtLeast(0)
        list.animateScrollToItem(page, -middle)
    }
    val context = LocalPlatformContext.current
    val accent = MaterialTheme.colorScheme.primary
    LazyRow(
        modifier.fillMaxWidth().height(ThumbHeight),
        state = list,
        horizontalArrangement = Arrangement.spacedBy(ThumbGap),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        items(total, key = { it }) { at ->
            val here = at == page
            val shape = MaterialTheme.shapes.small
            Box(
                Modifier
                    .width(ThumbWidth)
                    .height(ThumbHeight)
                    .alpha(if (here) 1f else 0.72f)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .then(if (here) Modifier.border(2.dp, accent, shape) else Modifier)
                    .clickable { onJump(at) },
            ) {
                val shown = preview(at)
                if (shown != null) {
                    val request = remember(shown.url) { buildImageRequest(context, shown.url, shown.headers) }
                    AsyncImage(
                        request,
                        null,
                        PageImages.get(context),
                        Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Text(
                    (at + 1).toString(),
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .background(Color.Black.copy(alpha = 0.5f), MaterialTheme.shapes.extraSmall)
                        .padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (here) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White,
                )
            }
        }
    }
}
