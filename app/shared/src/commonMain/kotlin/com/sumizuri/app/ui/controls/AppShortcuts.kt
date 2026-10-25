package com.sumizuri.app.ui.controls

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.sumizuri.app.data.model.controls.AppShortcut

// Listens for the shortcut keys over everything under it, before the page under it hears them. The callback says whether it did the shortcut, and a key it did not use goes on to the page.
@Composable
fun Modifier.appShortcuts(onShortcut: (AppShortcut) -> Boolean): Modifier {
    val focus = remember { FocusRequester() }
    val gestures by rememberUpdatedState(LocalAppGestures.current)
    val act by rememberUpdatedState(onShortcut)
    // Keys only reach a window that holds the focus somewhere, so this holds it until a page takes it.
    LaunchedEffect(Unit) { focus.requestFocus() }
    return this
        .mouseChords { chord -> gestures.shortcutFor(chord)?.let(act) ?: false }
        .focusRequester(focus)
        .focusable()
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val shortcut = event.toChord()?.let { gestures.shortcutFor(it) } ?: return@onPreviewKeyEvent false
            act(shortcut)
        }
}
