package com.sumizuri.app.feature.title.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import com.sumizuri.app.ui.motion.LocalPageLeaving
import com.sumizuri.app.ui.theme.LocalReduceMotion

// How long the page takes to fade in and out.
private const val InMs = 260
private const val OutMs = 170

// How much bigger the art starts before it settles.
internal const val ArtZoom = 0.04f

// How far the wide page has settled, from 0 to 1: it rises once the page opens and falls again as it is left. The page itself fades by the motion of pages. With less motion it is settled at once and stays.
@Composable
internal fun rememberPageFade(): State<Float> {
    val reduce = LocalReduceMotion.current
    val leaving = LocalPageLeaving.current
    val shown = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(leaving, reduce) {
        when {
            reduce -> shown.snapTo(1f)
            leaving -> shown.animateTo(0f, tween(OutMs, easing = FastOutSlowInEasing))
            else -> shown.animateTo(1f, tween(InMs, easing = FastOutSlowInEasing))
        }
    }
    return shown.asState()
}
