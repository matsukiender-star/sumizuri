package com.sumizuri.app.ui.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isForwardPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import com.sumizuri.app.playback.touchPlayer
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.mouse_back
import com.sumizuri.app.shared.resources.mouse_forward
import com.sumizuri.app.shared.resources.mouse_middle
import org.jetbrains.compose.resources.StringResource

// The side buttons and the wheel button of a mouse are bound like keys, by names of their own, so they are saved, listed and checked for clashes the same way.
private const val Back = "Mouse4"
private const val Forward = "Mouse5"
private const val Middle = "MouseMiddle"

private val MouseLabels = mapOf(
    Back to Res.string.mouse_back,
    Forward to Res.string.mouse_forward,
    Middle to Res.string.mouse_middle,
)

// The words for a mouse button name, or null when the name is not one.
internal fun labelOfMouse(name: String): StringResource? = MouseLabels[name]

internal fun isMouseKey(name: String): Boolean = name in MouseLabels

// Which of the bindable buttons were down.
private class Held(val back: Boolean, val forward: Boolean, val middle: Boolean)

private fun PointerEvent.held() = Held(buttons.isBackPressed, buttons.isForwardPressed, buttons.isTertiaryPressed)

// The chord of a bindable button that this event pressed, or null for any other event and for the buttons that click and open menus.
private fun PointerEvent.pressed(before: Held): KeyChord? {
    if (type != PointerEventType.Press) return null
    val name = when {
        buttons.isBackPressed && !before.back -> Back
        buttons.isForwardPressed && !before.forward -> Forward
        buttons.isTertiaryPressed && !before.middle -> Middle
        else -> return null
    }
    return KeyChord(
        name,
        ctrl = keyboardModifiers.isCtrlPressed,
        shift = keyboardModifiers.isShiftPressed,
        alt = keyboardModifiers.isAltPressed,
        meta = keyboardModifiers.isMetaPressed,
    )
}

// Listens for the mouse buttons that can be bound, over everything under it and before it. The callback says whether it did something, and a press it did not use goes on to what is under.
@Composable
fun Modifier.mouseChords(onChord: (KeyChord) -> Boolean): Modifier {
    val act by rememberUpdatedState(onChord)
    // A phone has no side buttons to listen for, so it listens to nothing and leaves every touch alone.
    if (touchPlayer) return this
    return pointerInput(Unit) {
        awaitPointerEventScope {
            var before = Held(false, false, false)
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val chord = event.pressed(before)
                before = event.held()
                if (chord != null && act(chord)) event.changes.forEach { it.consume() }
            }
        }
    }
}
