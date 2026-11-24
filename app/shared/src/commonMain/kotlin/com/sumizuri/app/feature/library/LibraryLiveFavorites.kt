package com.sumizuri.app.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.live.LiveChannel
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.suki_library_live_empty
import com.sumizuri.app.shared.resources.suki_library_live_go
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.image.AllowPlainHttp
import com.sumizuri.app.ui.kit.page.fullSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

// The favorite channels of every live list, read again when the lists or the favorites change, or null while they load.
@Composable
internal fun rememberLiveFavorites(): List<LiveChannel>? {
    val live = LocalAppContainer.current.live
    val favorites by produceState<List<LiveChannel>?>(null, live) {
        combine(live.playlists, live.favorites, ::Pair).collectLatest { (lists, ids) ->
            value =
                if (ids.isEmpty()) {
                    emptyList()
                } else {
                    withContext(Dispatchers.IO) { live.allChannels(lists) }.filter {
                        it.id in
                            ids
                    }
                }
        }
    }
    return favorites
}

// The favorite channels as tiles in the grid of the library, where a tap plays one. With none yet, a note and a way to the live tab.
internal fun LazyGridScope.liveFavorites(
    channels: List<LiveChannel>?,
    onPlay: (String, String) -> Unit,
    onOpenLive: () -> Unit,
) {
    if (channels == null) return
    if (channels.isEmpty()) {
        fullSpan {
            Column(Modifier.fillMaxWidth().padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(Res.string.suki_library_live_empty), color = MaterialTheme.colorScheme.outline)
                InkButton(onClick = onOpenLive) { Text(stringResource(Res.string.suki_library_live_go)) }
            }
        }
        return
    }
    items(
        channels,
        key = { "live:" + it.id },
    ) { channel -> LiveTile(channel) { onPlay(channel.playlistId, channel.id) } }
}

@Composable
private fun LiveTile(channel: LiveChannel, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.LiveTv, null, Modifier.size(36.dp), tint = colors.onSurfaceVariant)
            if (!channel.logo.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(channel.logo)
                        .apply {
                            extras[AllowPlainHttp] =
                                true
                        }.build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Text(
            channel.name,
            Modifier.padding(horizontal = 2.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
