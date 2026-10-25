package com.sumizuri.app

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.awt.Cursor
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window

// The thickness of an edge that can be dragged, and the size of a corner.
private val EdgeThickness = 6.dp
private val CornerSize = 14.dp

// Which sides of the window a handle moves, and the pointer it shows.
private class Side(val left: Boolean, val right: Boolean, val top: Boolean, val bottom: Boolean, val cursor: Int)

private val West = Side(left = true, right = false, top = false, bottom = false, cursor = Cursor.W_RESIZE_CURSOR)
private val East = Side(left = false, right = true, top = false, bottom = false, cursor = Cursor.E_RESIZE_CURSOR)
private val North = Side(left = false, right = false, top = true, bottom = false, cursor = Cursor.N_RESIZE_CURSOR)
private val South = Side(left = false, right = false, top = false, bottom = true, cursor = Cursor.S_RESIZE_CURSOR)
private val NorthWest = Side(left = true, right = false, top = true, bottom = false, cursor = Cursor.NW_RESIZE_CURSOR)
private val NorthEast = Side(left = false, right = true, top = true, bottom = false, cursor = Cursor.NE_RESIZE_CURSOR)
private val SouthWest = Side(left = true, right = false, top = false, bottom = true, cursor = Cursor.SW_RESIZE_CURSOR)
private val SouthEast = Side(left = false, right = true, top = false, bottom = true, cursor = Cursor.SE_RESIZE_CURSOR)

// A window without a bar from the system has no edges to drag, so thin handles along its edges and in its corners do the resizing.
@Composable
internal fun ResizeHandles(window: Window) {
    Box(Modifier.fillMaxSize()) {
        Handle(window, West, Modifier.align(Alignment.CenterStart).fillMaxHeight().width(EdgeThickness))
        Handle(window, East, Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(EdgeThickness))
        Handle(window, North, Modifier.align(Alignment.TopCenter).fillMaxWidth().height(EdgeThickness))
        Handle(window, South, Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(EdgeThickness))
        Handle(window, NorthWest, Modifier.align(Alignment.TopStart).size(CornerSize))
        Handle(window, NorthEast, Modifier.align(Alignment.TopEnd).size(CornerSize))
        Handle(window, SouthWest, Modifier.align(Alignment.BottomStart).size(CornerSize))
        Handle(window, SouthEast, Modifier.align(Alignment.BottomEnd).size(CornerSize))
    }
}

// One handle. The size is worked out from where the pointer is on the screen and not from the drag, since the handle moves with the window and a drag measured on it would shake.
@Composable
private fun Handle(window: Window, side: Side, modifier: Modifier) {
    var start = Rectangle()
    var from = Point()
    Box(
        modifier
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(side.cursor)))
            .pointerInput(side) {
                detectDragGestures(
                    onDragStart = {
                        start = window.bounds
                        from = MouseInfo.getPointerInfo().location
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        window.bounds =
                            resized(
                                start,
                                from,
                                MouseInfo.getPointerInfo().location,
                                side,
                                window.minimumSize.width,
                                window.minimumSize.height,
                            )
                    },
                )
            },
    )
}

// The bounds the window takes when the pointer moved from one spot on the screen to another, never smaller than the smallest size.
private fun resized(start: Rectangle, from: Point, now: Point, side: Side, minWidth: Int, minHeight: Int): Rectangle {
    val dx = now.x - from.x
    val dy = now.y - from.y
    var x = start.x
    var y = start.y
    var width = start.width
    var height = start.height
    if (side.left) {
        x += dx
        width -= dx
    }
    if (side.right) width += dx
    if (side.top) {
        y += dy
        height -= dy
    }
    if (side.bottom) height += dy
    if (width < minWidth) {
        if (side.left) x -= minWidth - width
        width = minWidth
    }
    if (height < minHeight) {
        if (side.top) y -= minHeight - height
        height = minHeight
    }
    return Rectangle(x, y, width, height)
}
