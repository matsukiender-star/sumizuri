package com.sumizuri.app

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.sumizuri.app.ui.controls.RemoteKeysDirect

// How long the pointer waits still before it hides, and how far one press moves it at first and at most while an arrow is held.
private const val HideAfterMs = 8000L
private const val StepDp = 10f
private const val MaxStepDp = 48f

// The pointer a TV remote moves with its arrows, as the browsers of TVs do. OK taps where it rests, holding OK holds the finger down, the arrows while OK is held drag, and pushing past an edge scrolls the page under it. The player and the reader take the keys themselves, so the pointer hides while they show.
internal class RemotePointer(private val activity: Activity) : View(activity) {
    private val density = resources.displayMetrics.density
    private var at = Pair(-1f, -1f)
    private var downTime = 0L
    private val hide = Runnable { visibility = INVISIBLE }
    private val arrow = Path().apply {
        moveTo(0f, 0f)
        lineTo(0f, 22f * density)
        lineTo(6f * density, 17f * density)
        lineTo(10f * density, 26f * density)
        lineTo(14f * density, 24f * density)
        lineTo(10f * density, 15.5f * density)
        lineTo(17f * density, 15.5f * density)
        close()
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(32, 32, 32)
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        strokeJoin = Paint.Join.ROUND
    }

    init {
        isFocusable = false
        isClickable = false
        visibility = INVISIBLE
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    // Says whether the pointer used the key, so the app does not hear it as well.
    fun handle(event: KeyEvent): Boolean {
        if (RemoteKeysDirect.active) {
            if (downTime != 0L) touch(MotionEvent.ACTION_CANCEL)
            visibility = INVISIBLE
            return false
        }
        val down = event.action == KeyEvent.ACTION_DOWN
        val (dx, dy) = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> -1 to 0
            KeyEvent.KEYCODE_DPAD_RIGHT -> 1 to 0
            KeyEvent.KEYCODE_DPAD_UP -> 0 to -1
            KeyEvent.KEYCODE_DPAD_DOWN -> 0 to 1
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                // The first key after the pointer hid only brings it back.
                if (down && event.repeatCount == 0 && !show()) {
                    downTime = SystemClock.uptimeMillis()
                    touch(MotionEvent.ACTION_DOWN)
                } else if (!down && downTime != 0L) {
                    touch(MotionEvent.ACTION_UP)
                }
                return true
            }
            else -> return false
        }
        if (down && !show()) move(dx, dy, event.repeatCount)
        return true
    }

    // Shows the pointer, in the middle the first time, and says whether it was hidden.
    private fun show(): Boolean {
        removeCallbacks(hide)
        postDelayed(hide, HideAfterMs)
        if (at.first < 0) at = width / 2f to height / 2f
        if (visibility == VISIBLE) return false
        visibility = VISIBLE
        invalidate()
        return true
    }

    private fun move(dx: Int, dy: Int, repeat: Int) {
        val step = (StepDp + repeat * 4f).coerceAtMost(MaxStepDp) * density
        val wantX = at.first + dx * step
        val wantY = at.second + dy * step
        val x = wantX.coerceIn(0f, width - 1f)
        val y = wantY.coerceIn(0f, height - 1f)
        at = x to y
        invalidate()
        when {
            downTime != 0L -> touch(MotionEvent.ACTION_MOVE)
            // Pushing past an edge scrolls what is under the pointer, faster the longer the arrow is held.
            x != wantX || y != wantY -> scroll(-dx, -dy, 1f + (repeat / 4).coerceAtMost(4))
            else -> hover()
        }
    }

    private fun touch(action: Int) {
        val ev = pointerEvent(action, MotionEvent.TOOL_TYPE_FINGER, InputDevice.SOURCE_TOUCHSCREEN, downTime)
        activity.dispatchTouchEvent(ev)
        ev.recycle()
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) downTime = 0L
    }

    // A mouse resting where the pointer is, so what is under it grows a little as it does under a mouse.
    private fun hover() {
        val ev = pointerEvent(MotionEvent.ACTION_HOVER_MOVE, MotionEvent.TOOL_TYPE_MOUSE, InputDevice.SOURCE_MOUSE)
        activity.window.decorView.dispatchGenericMotionEvent(ev)
        ev.recycle()
    }

    // The scroll is sent from the middle of the screen along the way it moves, since the edge itself is often a bar that does not scroll, such as the title at the top.
    private fun scroll(across: Int, down: Int, amount: Float) {
        val ev = pointerEvent(MotionEvent.ACTION_SCROLL, MotionEvent.TOOL_TYPE_MOUSE, InputDevice.SOURCE_MOUSE) {
            if (across != 0) x = width / 2f
            if (down != 0) y = height / 2f
            setAxisValue(MotionEvent.AXIS_HSCROLL, across * amount)
            setAxisValue(MotionEvent.AXIS_VSCROLL, down * amount)
        }
        activity.window.decorView.dispatchGenericMotionEvent(ev)
        ev.recycle()
    }

    private fun pointerEvent(
        action: Int,
        tool: Int,
        source: Int,
        start: Long = SystemClock.uptimeMillis(),
        axes: MotionEvent.PointerCoords.() -> Unit = {},
    ): MotionEvent {
        val props = MotionEvent.PointerProperties().apply {
            id = 0
            toolType = tool
        }
        val coords = MotionEvent.PointerCoords().apply {
            x = at.first
            y = at.second
            pressure = 1f
            size = 1f
            axes()
        }
        return MotionEvent.obtain(start, SystemClock.uptimeMillis(), action, 1, arrayOf(props), arrayOf(coords), 0, 0, 1f, 1f, 0, 0, source, 0)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(at.first, at.second)
        canvas.drawPath(arrow, fill)
        canvas.drawPath(arrow, edge)
        canvas.restore()
    }
}
