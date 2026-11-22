package com.sumizuri.app.feature.live

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.live.LiveChannel
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.suki_live_add_list
import com.sumizuri.app.shared.resources.suki_live_all
import com.sumizuri.app.shared.resources.suki_live_all_groups
import com.sumizuri.app.shared.resources.suki_live_empty_hint
import com.sumizuri.app.shared.resources.suki_live_empty_title
import com.sumizuri.app.shared.resources.suki_live_favorite
import com.sumizuri.app.shared.resources.suki_live_favorites
import com.sumizuri.app.shared.resources.suki_live_manage_lists
import com.sumizuri.app.shared.resources.suki_live_no_channels
import com.sumizuri.app.shared.resources.suki_live_recents
import com.sumizuri.app.shared.resources.suki_live_search_hint
import com.sumizuri.app.shared.resources.suki_live_title
import com.sumizuri.app.shared.resources.suki_live_ungrouped
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.input.AppSearchBar
import com.sumizuri.app.ui.kit.page.PageGutter
import com.sumizuri.app.ui.kit.page.PageTopBar
import org.jetbrains.compose.resources.stringResource

@Composable
fun LiveRoute(onPlay: (playlistId: String, channelId: String) -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel { LiveViewModel(container.live) }
    val state by vm.state.collectAsStateWithLifecycle()
    LiveScreen(state = state, vm = vm, onPlay = onPlay)
}

@Composable
private fun LiveScreen(
    state: LiveUiState,
    vm: LiveViewModel,
    onPlay: (playlistId: String, channelId: String) -> Unit,
) {
    var addOpen by remember { mutableStateOf(false) }
    var manageOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        PageTopBar(stringResource(Res.string.suki_live_title)) {
            if (state.playlists.isNotEmpty()) {
                IconButton(onClick = { manageOpen = true }) {
                    Icon(Icons.Outlined.PlaylistPlay, stringResource(Res.string.suki_live_manage_lists))
                }
            }
            IconButton(onClick = { addOpen = true }) {
                Icon(Icons.Outlined.Add, stringResource(Res.string.suki_live_add_list))
            }
        }

        if (state.playlists.isEmpty()) {
            LiveEmptyState(onAdd = { addOpen = true })
        } else {
            LiveCatalogContent(state = state, vm = vm, onPlay = onPlay)
        }
    }

    if (addOpen) {
        AddLivePlaylistSheet(
            onAddUrl = vm::addUrl,
            onAddFile = vm::addFile,
            onDismiss = { addOpen = false },
        )
    }
    if (manageOpen) {
        ManageLivePlaylistsSheet(
            playlists = state.playlists,
            onRefresh = vm::refresh,
            onRename = vm::rename,
            onRemove = vm::remove,
            onAddNew = {
                manageOpen = false
                addOpen = true
            },
            onDismiss = { manageOpen = false },
        )
    }
}

@Composable
private fun LiveEmptyState(onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.LiveTv,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(Res.string.suki_live_empty_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(Res.string.suki_live_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            InkButton(onClick = onAdd) {
                Text(stringResource(Res.string.suki_live_add_list))
            }
        }
    }
}

@Composable
private fun LiveCatalogContent(
    state: LiveUiState,
    vm: LiveViewModel,
    onPlay: (playlistId: String, channelId: String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = PageGutter),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppSearchBar(
                value = state.query,
                onValueChange = vm::setQuery,
                hint = stringResource(Res.string.suki_live_search_hint),
                pauseMs = 150,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "sec_all") {
                    FilterChip(
                        selected = state.section == LiveSection.All,
                        onClick = { vm.selectSection(LiveSection.All) },
                        label = { Text(stringResource(Res.string.suki_live_all)) },
                    )
                }
                item(key = "sec_fav") {
                    FilterChip(
                        selected = state.section == LiveSection.Favorites,
                        onClick = { vm.selectSection(LiveSection.Favorites) },
                        label = { Text(stringResource(Res.string.suki_live_favorites)) },
                    )
                }
                item(key = "sec_rec") {
                    FilterChip(
                        selected = state.section == LiveSection.Recents,
                        onClick = { vm.selectSection(LiveSection.Recents) },
                        label = { Text(stringResource(Res.string.suki_live_recents)) },
                    )
                }
                if (state.playlists.size > 1) {
                    items(state.playlists, key = { "pl_${it.id}" }) { playlist ->
                        FilterChip(
                            selected = state.selectedPlaylistId == playlist.id,
                            onClick = {
                                vm.selectPlaylist(if (state.selectedPlaylistId == playlist.id) null else playlist.id)
                            },
                            label = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }
            if (state.groups.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "grp_all") {
                        FilterChip(
                            selected = state.selectedGroup == null,
                            onClick = { vm.selectGroup(null) },
                            label = { Text(stringResource(Res.string.suki_live_all_groups)) },
                        )
                    }
                    items(state.groups, key = { "grp_$it" }) { group ->
                        FilterChip(
                            selected = state.selectedGroup == group,
                            onClick = { vm.selectGroup(if (state.selectedGroup == group) null else group) },
                            label = { Text(group, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                    }
                }
            }
        }

        if (state.channels.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(Res.string.suki_live_no_channels),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = PageGutter, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.channels, key = { it.id }) { channel ->
                    LiveChannelRow(
                        channel = channel,
                        favorite = channel.id in state.favorites,
                        onPlay = { onPlay(channel.playlistId, channel.id) },
                        onToggleFavorite = { vm.toggleFavorite(channel.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveChannelRow(channel: LiveChannel, favorite: Boolean, onPlay: () -> Unit, onToggleFavorite: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onPlay),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainerHigh,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.LiveTv, null, Modifier.size(22.dp), tint = colors.onSurfaceVariant)
                if (!channel.logo.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logo,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    channel.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    channel.group.ifBlank { stringResource(Res.string.suki_live_ungrouped) },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(Res.string.suki_live_favorite),
                    tint = if (favorite) colors.primary else colors.onSurfaceVariant,
                )
            }
        }
    }
}
