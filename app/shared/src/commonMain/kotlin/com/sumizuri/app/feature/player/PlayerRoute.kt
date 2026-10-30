package com.sumizuri.app.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.model.player.PlayerPrefs
import com.sumizuri.app.feature.player.overlay.subtitleStyle
import com.sumizuri.app.playback.rememberPlayerEngine
import kotlinx.coroutines.flow.first

// The page that plays an episode. It waits for the choices of the person so the subtitles start with the look they chose, then makes the engine and the session. The engine lets go when the page is left, and the session keeps the place first.
@Composable
fun PlayerRoute(chapterId: Long, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val prefs by produceState<PlayerPrefs?>(null) { value = container.playerPrefs.prefs.first() }
    val loaded = prefs
    if (loaded == null) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }
    val engine = rememberPlayerEngine(remember { loaded.subtitleStyle() })
    val scope = rememberCoroutineScope()
    val session = remember(engine) {
        PlayerSession(
            engine,
            scope,
            container.library,
            container.sources,
            container.manager,
            container.solver,
            container.playerPrefs,
            container.reading,
            container.downloads,
        )
    }
    DisposableEffect(session) {
        session.start(chapterId)
        onDispose { session.dispose() }
    }
    PlayerScreen(session, engine, onBack)
}
