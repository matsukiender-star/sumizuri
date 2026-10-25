package com.sumizuri.app

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import java.awt.Window
import java.util.concurrent.locks.LockSupport
import kotlin.concurrent.thread

// A point on the screen in real pixels, as Windows gives it.
@Structure.FieldOrder("x", "y")
class WinPoint : Structure() {
    @JvmField var x: Int = 0

    @JvmField var y: Int = 0
}

// The edges of a window on the screen in real pixels.
@Structure.FieldOrder("left", "top", "right", "bottom")
class WinRect : Structure() {
    @JvmField var left: Int = 0

    @JvmField var top: Int = 0

    @JvmField var right: Int = 0

    @JvmField var bottom: Int = 0
}

// The calls of Windows the drag needs. They can be made from any thread.
private interface User32Calls : Library {
    fun GetCursorPos(point: WinPoint): Boolean
    fun GetWindowRect(hWnd: Pointer, rect: WinRect): Boolean
    fun GetAsyncKeyState(key: Int): Short
    fun GetSystemMetrics(index: Int): Int
    fun SetWindowPos(hWnd: Pointer, after: Pointer?, x: Int, y: Int, width: Int, height: Int, flags: Int): Boolean
}

private const val VkLButton = 0x01
private const val VkRButton = 0x02
private const val SmSwapButton = 23
private const val SwpNoSize = 0x0001
private const val SwpNoZOrder = 0x0004
private const val SwpNoActivate = 0x0010
private const val SwpAsyncWindowPos = 0x4000

// How long the drag waits between two looks at the mouse.
private const val StepNanos = 1_000_000L

// Windows itself, or null on the other systems, which keep the drag of Compose.
private val user32: User32Calls? =
    if (System.getProperty("os.name").orEmpty().startsWith("Windows")) {
        runCatching {
            Native.load("user32", User32Calls::class.java)
        }.getOrNull()
    } else {
        null
    }

// True where the window can be moved this way.
internal val nativeDragAvailable: Boolean get() = user32 != null

// Where the mouse was pressed, against the corner of the window, in real pixels.
private class Grip(val dx: Int, val dy: Int)

private fun grip(calls: User32Calls, handle: Pointer): Grip? {
    val point = WinPoint()
    val rect = WinRect()
    if (!calls.GetCursorPos(point) || !calls.GetWindowRect(handle, rect)) return null
    return Grip(point.x - rect.left, point.y - rect.top)
}

// Moves the window with the mouse until the button is let go. It runs on a thread of its own that looks at the real mouse every millisecond and places the window with Windows directly, so it keeps up with a fast mouse where a move done once per frame of the app lagged behind.
private fun follow(calls: User32Calls, handle: Pointer, grip: Grip) {
    // With the buttons swapped, the main button is the right one.
    val button = if (calls.GetSystemMetrics(SmSwapButton) != 0) VkRButton else VkLButton
    thread(isDaemon = true, name = "window-drag") {
        val point = WinPoint()
        var lastX = Int.MIN_VALUE
        var lastY = Int.MIN_VALUE
        while (calls.GetAsyncKeyState(button).toInt() and 0x8000 != 0) {
            if (calls.GetCursorPos(point) && (point.x != lastX || point.y != lastY)) {
                lastX = point.x
                lastY = point.y
                calls.SetWindowPos(
                    handle,
                    null,
                    point.x - grip.dx,
                    point.y - grip.dy,
                    0,
                    0,
                    SwpNoSize or SwpNoZOrder or SwpNoActivate or SwpAsyncWindowPos,
                )
            }
            LockSupport.parkNanos(StepNanos)
        }
    }
}

// The part of the bar that moves the window. The move starts once the mouse has moved a little while pressed, so a double tap on the bar still reaches the bar, and the window keeps the spot that was pressed under the mouse.
@Composable
internal fun NativeDragArea(window: Window, enabled: Boolean, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().pointerInput(window, enabled) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                if (!enabled || down.type != PointerType.Mouse) return@awaitEachGesture
                val calls = user32 ?: return@awaitEachGesture
                val handle = Native.getComponentPointer(window) ?: return@awaitEachGesture
                val held = grip(calls, handle) ?: return@awaitEachGesture
                val slop = viewConfiguration.touchSlop
                while (true) {
                    val event = awaitPointerEvent()
                    if (!event.buttons.isPrimaryPressed) return@awaitEachGesture
                    val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                    if (!change.pressed) return@awaitEachGesture
                    if ((change.position - down.position).getDistance() > slop) {
                        follow(calls, handle, held)
                        return@awaitEachGesture
                    }
                }
            }
        },
    ) { content() }
}
