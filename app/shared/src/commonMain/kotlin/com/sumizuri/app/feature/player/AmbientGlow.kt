package com.sumizuri.app.feature.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// How long the light takes to follow a new scene.
private const val FollowMs = 700

// How strong the light is where it meets the picture.
private const val Strength = 0.7f

// A soft light in the colors at the edges of the picture, which fills the bars around a video that does not fill the screen. Each edge lights its own side and fades toward the middle, where the picture covers it.
@Composable
internal fun AmbientGlow(colors: List<Color>, modifier: Modifier = Modifier) {
    if (colors.size < 4) return
    val left by animateColorAsState(colors[0], tween(FollowMs))
    val right by animateColorAsState(colors[1], tween(FollowMs))
    val top by animateColorAsState(colors[2], tween(FollowMs))
    val bottom by animateColorAsState(colors[3], tween(FollowMs))
    Canvas(modifier) {
        val clear = Color.Transparent
        drawRect(Brush.horizontalGradient(0f to left.copy(alpha = Strength), 0.5f to clear, endX = size.width))
        drawRect(Brush.horizontalGradient(0.5f to clear, 1f to right.copy(alpha = Strength), endX = size.width))
        drawRect(Brush.verticalGradient(0f to top.copy(alpha = Strength), 0.5f to clear, endY = size.height))
        drawRect(Brush.verticalGradient(0.5f to clear, 1f to bottom.copy(alpha = Strength), endY = size.height))
    }
}
