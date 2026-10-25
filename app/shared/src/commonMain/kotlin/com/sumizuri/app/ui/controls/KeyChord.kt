package com.sumizuri.app.ui.controls

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key

// A key with the modifier keys held down with it. The key is kept by a name that is the same on every system, since the codes of keys are not.
data class KeyChord(
    val key: String,
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
    val meta: Boolean = false,
)

// The chord a key press makes, or null when the key cannot be bound, as a key that is a modifier on its own.
fun KeyEvent.toChord(): KeyChord? {
    val name = nameOfKey(key) ?: return null
    return KeyChord(name, ctrl = isCtrlPressed, shift = isShiftPressed, alt = isAltPressed, meta = isMetaPressed)
}

// The chord as words, such as Ctrl + Shift + F. The modifier and key names stay as they are printed on keyboards, and the mouse buttons are in the language of the app.
@Composable
fun KeyChord.text(): String = buildList {
    if (ctrl) add("Ctrl")
    if (alt) add("Alt")
    if (shift) add("Shift")
    if (meta) add("Meta")
    add(labelOfKey(key))
}.joinToString(" + ")
