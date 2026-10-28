package com.sumizuri.app.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.theme.LocalReduceMotion
import com.sumizuri.app.ui.theme.LocalThemeSpec

// Makes a piece shrink a little while it is pressed and grow a little while the pointer rests on it, by the amounts the theme sets. The piece must use the same interaction source for its clicks. A piece the remote of a TV moves to grows and gets a ring, so it is clear where the focus is.
@Composable
fun Modifier.feedbackScale(source: MutableInteractionSource): Modifier {
    val focused by source.collectIsFocusedAsState()
    val ring = MaterialTheme.colorScheme.primary
    val ringed = Modifier.drawWithContent {
        drawContent()
        if (focused) {
            val width = 3.dp.toPx()
            drawRoundRect(ring, cornerRadius = CornerRadius(12.dp.toPx()), style = Stroke(width))
        }
    }
    if (LocalReduceMotion.current) return then(ringed)
    val motion = LocalThemeSpec.current.options.motion
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        if (pressed) {
            motion.pressScale
        } else if (focused) {
            maxOf(motion.hoverScale, 1.06f)
        } else if (hovered) {
            motion.hoverScale
        } else {
            1f
        },
        tween(motionMs(AppMotion.Micro), easing = AppMotion.Exit),
        label = "feedback",
    )
    return hoverable(source).graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.then(ringed)
}
