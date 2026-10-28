package com.sumizuri.app.ui.controls

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import com.sumizuri.app.data.model.controls.AppShortcut

// Listens for the shortcut keys over everything under it, before the page under it hears them. The callback says whether it did the shortcut, and a key it did not use goes on to the page.
@Composable
fun Modifier.appShortcuts(onShortcut: (AppShortcut) -> Boolean): Modifier {
    val focus = remember { FocusRequester() }
    val gestures by rememberUpdatedState(LocalAppGestures.current)
    val act by rememberUpdatedState(onShortcut)
    // Keys only reach a window that holds the focus somewhere, so this holds it until a page takes it.
    LaunchedEffect(Unit) { focus.requestFocus() }
    // While the focus is only held here, the arrows of a TV remote step into the page instead of looking around a box as big as the window.
    val focusManager = LocalFocusManager.current
    val held = remember { mutableStateOf(false) }
    return this
        .mouseChords { chord -> gestures.shortcutFor(chord)?.let(act) ?: false }
        .focusRequester(focus)
        .onFocusChanged { held.value = it.isFocused }
        .focusable()
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            if (held.value && event.key in RemoteKeys) return@onPreviewKeyEvent focusManager.moveFocus(FocusDirection.Enter)
            val shortcut = event.toChord()?.let { gestures.shortcutFor(it) } ?: return@onPreviewKeyEvent false
            act(shortcut)
        }
}

private val RemoteKeys = setOf(Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight, Key.DirectionCenter)
