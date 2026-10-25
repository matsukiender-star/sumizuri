package com.sumizuri.app.ui.kit.scroll

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import kotlinx.coroutines.delay

// How long a pull waits for the work it asked for to start before it gives up, for work that has nothing to do.
private const val StartWaitMs = 800L

// How far the mouse has to pull a page down, in dp, to refresh it.
private const val PullToReach = 90

// A page that is pulled down to refresh it. The spinner stays while the work runs and goes when it stops, and it also goes when the work never began, such as a check with no title to check. A finger pulls the page with the material box. A mouse drags the page down past its top, and the spinner follows the drag.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRefresh(
    running: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val pulled = remember { mutableStateOf(false) }
    val started = remember { mutableStateOf(false) }
    LaunchedEffect(running) {
        if (!pulled.value) return@LaunchedEffect
        if (running) {
            started.value = true
        } else if (started.value) {
            pulled.value = false
        }
    }
    LaunchedEffect(pulled.value) {
        if (!pulled.value) return@LaunchedEffect
        delay(StartWaitMs)
        if (!started.value) pulled.value = false
    }
    val latestRunning = rememberUpdatedState(running)
    val latestRefresh = rememberUpdatedState(onRefresh)
    val refresh = {
        pulled.value = true
        started.value = latestRunning.value
        latestRefresh.value()
    }
    if (pullsToRefresh) {
        PullToRefreshBox(isRefreshing = pulled.value, onRefresh = refresh, modifier = modifier, content = content)
        return
    }
    // The wheel cannot let go of a pull, so on a desktop only a drag of the mouse pulls.
    val pull = remember { mutableStateOf(0f) }
    val reach = with(LocalDensity.current) { PullToReach.dp.toPx() }
    val latestAsk = rememberUpdatedState(refresh)
    val listener = remember(reach) { MousePull(reach, pull, pulled) { latestAsk.value() } }
    Box(modifier) {
        CompositionLocalProvider(LocalDragPull provides listener) { content() }
        if (pulled.value || pull.value > 0f) {
            Surface(
                Modifier.align(Alignment.TopCenter).graphicsLayer {
                    translationY =
                        (if (pulled.value) reach else pull.value) * 0.5f
                },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                shadowElevation = 4.dp,
            ) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    if (pulled.value) {
                        InkCircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        InkCircularProgressIndicator(progress = {
                            (pull.value / reach).coerceIn(0f, 1f)
                        }, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}

// The pull of a mouse as the page keeps it: how far it is, and a refresh when it is let go past the mark and no refresh is on.
private class MousePull(
    private val reach: Float,
    private val pull: MutableState<Float>,
    private val busy: MutableState<Boolean>,
    private val refresh: () -> Unit,
) : DragPull {
    override fun moved(pull: Float) {
        this.pull.value = pull
    }

    override fun released() {
        if (pull.value >= reach && !busy.value) refresh()
        pull.value = 0f
    }
}

// Hears how far a page is pulled down past its top by the mouse, and when the mouse lets go, so the page can be refreshed with a drag as it is with a finger.
interface DragPull {
    fun moved(pull: Float)

    fun released()
}

// Who is listening for a pull past the top, set by a page that refreshes.
val LocalDragPull = compositionLocalOf<DragPull?> { null }
