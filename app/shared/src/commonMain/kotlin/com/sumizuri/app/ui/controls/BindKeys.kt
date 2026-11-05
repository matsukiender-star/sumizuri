package com.sumizuri.app.ui.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.input.key.Key
import com.sumizuri.app.data.model.controls.AppGestures
import com.sumizuri.app.data.model.controls.ReaderControls
import org.jetbrains.compose.resources.stringResource

// One key that can be bound: the name it is saved by, the key itself and the words shown for it.
private class BindKey(val name: String, val key: Key, val label: String)

private val Letters = listOf(
    Key.A, Key.B, Key.C, Key.D, Key.E, Key.F, Key.G, Key.H, Key.I, Key.J, Key.K, Key.L, Key.M,
    Key.N, Key.O, Key.P, Key.Q, Key.R, Key.S, Key.T, Key.U, Key.V, Key.W, Key.X, Key.Y, Key.Z,
).mapIndexed { index, key -> ('A' + index).toString().let { BindKey(it, key, it) } }

private val Digits = listOf(Key.Zero, Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)
    .mapIndexed { index, key -> BindKey("Digit$index", key, "$index") }

private val Functions = listOf(Key.F1, Key.F2, Key.F3, Key.F4, Key.F5, Key.F6, Key.F7, Key.F8, Key.F9, Key.F10, Key.F11, Key.F12)
    .mapIndexed { index, key -> BindKey("F${index + 1}", key, "F${index + 1}") }

private val Others = listOf(
    BindKey("Space", Key.Spacebar, "Space"),
    BindKey("Enter", Key.Enter, "Enter"),
    BindKey("Escape", Key.Escape, "Esc"),
    BindKey("Tab", Key.Tab, "Tab"),
    BindKey("Backspace", Key.Backspace, "Backspace"),
    BindKey("Delete", Key.Delete, "Delete"),
    BindKey("PageUp", Key.PageUp, "Page Up"),
    BindKey("PageDown", Key.PageDown, "Page Down"),
    BindKey("Home", Key.MoveHome, "Home"),
    BindKey("End", Key.MoveEnd, "End"),
    BindKey("Left", Key.DirectionLeft, "Left"),
    BindKey("Right", Key.DirectionRight, "Right"),
    BindKey("Up", Key.DirectionUp, "Up"),
    BindKey("Down", Key.DirectionDown, "Down"),
    BindKey("Center", Key.DirectionCenter, "OK"),
    BindKey("Comma", Key.Comma, ","),
    BindKey("Period", Key.Period, "."),
    BindKey("Slash", Key.Slash, "/"),
    BindKey("Semicolon", Key.Semicolon, ";"),
    BindKey("Minus", Key.Minus, "-"),
    BindKey("Equals", Key.Equals, "="),
    BindKey("LeftBracket", Key.LeftBracket, "["),
    BindKey("RightBracket", Key.RightBracket, "]"),
)

private val All = Letters + Digits + Functions + Others
private val ByKey = All.associateBy { it.key }
private val ByName = All.associateBy { it.name }

// The name a key is saved by, or null for a key that cannot be bound, such as a modifier.
internal fun nameOfKey(key: Key): String? = ByKey[key]?.name

// The words for a saved name, which are the name itself for one that is not known.
@Composable
internal fun labelOfKey(name: String): String = ByName[name]?.label ?: labelOfMouse(name)?.let { stringResource(it) } ?: name

// The keys and taps of the reader as the person set them, for every viewer.
val LocalReaderControls = compositionLocalOf { ReaderControls() }

// The gestures and the shortcut keys that work across the app.
val LocalAppGestures = compositionLocalOf { AppGestures() }
