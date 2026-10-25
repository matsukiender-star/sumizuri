package com.sumizuri.app.feature.reader.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.min

// How long the zoom takes to glide from one panel to the next.
private const val GlideMs = 320

// A panel fills this much of the screen, so its edges stay in sight.
private const val Fill = 0.94f

private data class Focus(val scale: Float, val x: Float, val y: Float)

// Zooms a page that fills the screen to one of its panels, gliding from panel to panel, or shows the whole page for null. The shape is the height of the page over its width.
@Composable
internal fun Modifier.panelFocus(panel: Rect?, shape: Float?): Modifier {
    var box by remember { mutableStateOf(IntSize.Zero) }
    val target = remember(panel, shape, box) { focusOn(panel, shape, box) }
    val scale by animateFloatAsState(target.scale, tween(GlideMs))
    val x by animateFloatAsState(target.x, tween(GlideMs))
    val y by animateFloatAsState(target.y, tween(GlideMs))
    return onSizeChanged { box = it }.graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationX = x
        translationY = y
    }
}

// The scale and the move that bring the middle of a panel to the middle of the screen, with the page fitted to the screen as the reader draws it.
private fun focusOn(panel: Rect?, shape: Float?, box: IntSize): Focus {
    if (panel == null || shape == null || shape <= 0f || box.width == 0 || box.height == 0) return Focus(1f, 0f, 0f)
    val width = box.width.toFloat()
    val height = box.height.toFloat()
    val pageWidth = if (height / width > shape) width else height / shape
    val pageHeight = pageWidth * shape
    val left = (width - pageWidth) / 2f + panel.left * pageWidth
    val top = (height - pageHeight) / 2f + panel.top * pageHeight
    val panelWidth = panel.width * pageWidth
    val panelHeight = panel.height * pageHeight
    val scale = (min(width / panelWidth, height / panelHeight) * Fill).coerceIn(1f, 5f)
    val middleX = left + panelWidth / 2f
    val middleY = top + panelHeight / 2f
    return Focus(scale, -(middleX - width / 2f) * scale, -(middleY - height / 2f) * scale)
}
