package com.sumizuri.app.feature.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.live.LiveChannel
import com.sumizuri.app.playback.AspectMode
import com.sumizuri.app.playback.LocalFullscreen
import com.sumizuri.app.playback.PlayerMedia
import com.sumizuri.app.playback.PlayerSurface
import com.sumizuri.app.playback.PlayerSystemUi
import com.sumizuri.app.playback.PlayerWindow
import com.sumizuri.app.playback.SubtitleStyle
import com.sumizuri.app.playback.rememberPlayerEngine
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.suki_live_badge
import com.sumizuri.app.shared.resources.suki_live_favorite
import com.sumizuri.app.shared.resources.suki_live_next_channel
import com.sumizuri.app.shared.resources.suki_live_player_error_hint
import com.sumizuri.app.shared.resources.suki_live_player_error_title
import com.sumizuri.app.shared.resources.suki_live_prev_channel
import com.sumizuri.app.shared.resources.suki_live_retry
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// Dedicated live stream player: no seek bar, no episode progress/history, clear error recovery and clean engine release.
@Composable
fun LivePlayerRoute(playlistId: String, initialChannelId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val repo = container.live
    val favorites by repo.favorites.collectAsStateWithLifecycle(emptySet())
    val scope = rememberCoroutineScope()

    var channels by remember(playlistId) { mutableStateOf<List<LiveChannel>>(emptyList()) }
    var currentId by remember(playlistId, initialChannelId) { mutableStateOf(initialChannelId) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var controlsShown by remember { mutableStateOf(true) }
    var aspect by remember { mutableStateOf(AspectMode.Fit) }

    LaunchedEffect(playlistId) {
        channels = repo.channelsOf(playlistId)
    }

    val currentIndex = remember(channels, currentId) { channels.indexOfFirst { it.id == currentId } }
    val channel = channels.getOrNull(currentIndex)
    val engine = rememberPlayerEngine(SubtitleStyle())
    val playerState by engine.state.collectAsStateWithLifecycle()

    DisposableEffect(engine) {
        onDispose { engine.release() }
    }

    LaunchedEffect(channel?.id, reloadNonce) {
        val target = channel ?: return@LaunchedEffect
        repo.recordRecent(target.id)
        engine.open(PlayerMedia(url = target.url, headers = target.headers()), startMs = 0L, play = true)
    }

    LaunchedEffect(controlsShown, playerState.playing, playerState.problem) {
        if (controlsShown && playerState.playing && playerState.problem == null) {
            delay(4_000)
            controlsShown = false
        }
    }

    val goNext: (() -> Unit)? = if (channels.size > 1 && currentIndex >= 0) {
        {
            val next = channels[(currentIndex + 1) % channels.size]
            currentId = next.id
            controlsShown = true
        }
    } else {
        null
    }
    val goPrev: (() -> Unit)? = if (channels.size > 1 && currentIndex >= 0) {
        {
            val prev = channels[(currentIndex - 1 + channels.size) % channels.size]
            currentId = prev.id
            controlsShown = true
        }
    } else {
        null
    }

    PlayerWindow()
    PlayerSystemUi(keepOn = playerState.playing && playerState.problem == null)

    val fullscreen = LocalFullscreen.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { controlsShown = !controlsShown },
    ) {
        PlayerSurface(engine, aspect, Modifier.fillMaxSize())

        if (playerState.buffering && playerState.problem == null) {
            InkCircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        if (playerState.problem != null) {
            LiveErrorOverlay(
                onRetry = { reloadNonce++ },
                onNext = goNext,
                onBack = onBack,
            )
        }

        AnimatedVisibility(
            visible = controlsShown && playerState.problem == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)))
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            channel?.name.orEmpty(),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                stringResource(Res.string.suki_live_badge),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    channel?.let { ch ->
                        val fav = ch.id in favorites
                        IconButton(onClick = { scope.launch { repo.toggleFavorite(ch.id) } }) {
                            Icon(
                                if (fav) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                stringResource(Res.string.suki_live_favorite),
                                tint = if (fav) MaterialTheme.colorScheme.primary else Color.White,
                            )
                        }
                    }
                    IconButton(onClick = {
                        val entries = AspectMode.entries
                        aspect = entries[(entries.indexOf(aspect) + 1) % entries.size]
                    }) {
                        Icon(Icons.Outlined.AspectRatio, null, tint = Color.White)
                    }
                    fullscreen?.let { fs ->
                        IconButton(onClick = fs.toggle) {
                            Icon(
                                if (fs.active) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                                null,
                                tint = Color.White,
                            )
                        }
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))))
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { goPrev?.invoke() }, enabled = goPrev != null) {
                        Icon(
                            Icons.Outlined.SkipPrevious,
                            stringResource(Res.string.suki_live_prev_channel),
                            Modifier.size(32.dp),
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = { if (playerState.playing) engine.pause() else engine.play() }) {
                        Icon(
                            if (playerState.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            null,
                            Modifier.size(40.dp),
                            tint = Color.White,
                        )
                    }
                    IconButton(onClick = { goNext?.invoke() }, enabled = goNext != null) {
                        Icon(
                            Icons.Outlined.SkipNext,
                            stringResource(Res.string.suki_live_next_channel),
                            Modifier.size(32.dp),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveErrorOverlay(onRetry: () -> Unit, onNext: (() -> Unit)?, onBack: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = Color.White)
        }
        Surface(
            modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.error)
                Text(
                    stringResource(Res.string.suki_live_player_error_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(Res.string.suki_live_player_error_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InkButton(onClick = onRetry) { Text(stringResource(Res.string.suki_live_retry)) }
                    if (onNext != null) {
                        OutlinedButton(onClick = onNext) { Text(stringResource(Res.string.suki_live_next_channel)) }
                    }
                }
            }
        }
    }
}
