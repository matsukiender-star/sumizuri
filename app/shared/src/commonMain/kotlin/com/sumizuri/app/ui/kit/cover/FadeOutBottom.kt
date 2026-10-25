package com.sumizuri.app.ui.kit.cover

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

// Fades what it is put on out to nothing at the bottom, in several steps so no edge shows. It fades to clear and not to a color, so whatever is behind the page shows through.
fun Modifier.fadeOutBottom(from: Float = 0.45f): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val rest = 1f - from
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Black,
                from to Color.Black,
                from + rest * 0.35f to Color.Black.copy(alpha = 0.7f),
                from + rest * 0.65f to Color.Black.copy(alpha = 0.25f),
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }
