package com.sumizuri.app.ui.kit.cover

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.motion.AppMotion
import com.sumizuri.app.ui.motion.motionMs
import com.sumizuri.app.ui.theme.LocalThemeSpec
import com.sumizuri.app.ui.theme.spec.ProgressStyle

// How far in from the edges of the cover a floating bar sits.
private val FloatInset = 6.dp

// The bar that shows how far a title has been read: a round ended bar over a faint track, filled in the two accents of the theme or in its main one, as thick as the theme asks, along the edge or floating a little above it. A title with nothing read, or all read, has none. It fills the width it is given.
@Composable
fun ReadProgress(done: Float, modifier: Modifier = Modifier, onCover: Boolean = true) {
    if (done <= 0f || done >= 1f) return
    val scheme = MaterialTheme.colorScheme
    val look = LocalThemeSpec.current.options.covers
    val fill = if (look.progressStyle ==
        ProgressStyle.Gradient
    ) {
        Brush.horizontalGradient(listOf(scheme.tertiary, scheme.primary))
    } else {
        SolidColor(scheme.primary)
    }
    val track = if (onCover) Color.Black.copy(alpha = 0.35f) else scheme.onSurface.copy(alpha = 0.14f)
    // The bar fills to where it should be when it first shows, and moves when the reading moves it.
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(done) { target = done.coerceIn(0f, 1f) }
    val share by animateFloatAsState(
        target,
        tween(motionMs(AppMotion.Page + 180), easing = AppMotion.Entrance),
        label = "progress",
    )
    val placed = if (look.progressFloating) {
        modifier.padding(
            start = FloatInset,
            end = FloatInset,
            bottom = FloatInset,
        )
    } else {
        modifier
    }
    Canvas(placed.fillMaxWidth().height(look.progressHeight.coerceIn(1f, 12f).dp)) {
        val corner = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = corner)
        val reach = size.width * share
        if (reach >
            0f
        ) {
            drawRoundRect(fill, size = Size(reach.coerceAtLeast(size.height), size.height), cornerRadius = corner)
        }
    }
}
