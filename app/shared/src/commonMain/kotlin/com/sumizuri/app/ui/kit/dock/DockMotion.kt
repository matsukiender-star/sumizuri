package com.sumizuri.app.ui.kit.dock

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

// A soft spring for things that grow or arrive, settling without a bounce one can see.
private fun <T> settle() = spring<T>(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)

// How the capsule at the top and the dock at the bottom come in: they rise from their edge with a spring, grow a little from it, and fade in. With less motion asked for they only fade.
fun dockEnter(fromTop: Boolean, reduce: Boolean): EnterTransition {
    if (reduce) return fadeIn(tween(150))
    val origin = TransformOrigin(0.5f, if (fromTop) 0f else 1f)
    return fadeIn(tween(180)) +
        slideInVertically(settle<IntOffset>()) { if (fromTop) -it / 2 else it / 2 } +
        scaleIn(settle(), initialScale = 0.94f, transformOrigin = origin)
}

// How they go: quicker than they came, so the page is back at once.
fun dockExit(fromTop: Boolean, reduce: Boolean): ExitTransition {
    if (reduce) return fadeOut(tween(120))
    val origin = TransformOrigin(0.5f, if (fromTop) 0f else 1f)
    return fadeOut(tween(120)) +
        slideOutVertically(tween(160, easing = FastOutLinearInEasing)) { if (fromTop) -it / 3 else it / 3 } +
        scaleOut(tween(160), targetScale = 0.96f, transformOrigin = origin)
}

// How the dock turns from the small pill with the page into the full dock and back: its size moves on the spring while the pill fades out and the dock fades in a moment later, so it reads as one thing changing shape.
fun AnimatedContentTransitionScope<Boolean>.dockMorph(reduce: Boolean): ContentTransform {
    val size =
        SizeTransform(
            clip = true,
        ) { _, _ ->
            if (reduce) {
                tween<IntSize>(
                    150,
                )
            } else {
                spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
            }
        }
    return (fadeIn(tween(200, delayMillis = if (reduce) 0 else 80)) togetherWith fadeOut(tween(90))) using size
}

// How the panels of a dock change, where none is the state with no panel open. Opening one grows the dock with a spring while the panel rises in from the bottom edge a moment later, closing one fades it out fast while the dock settles back, and one panel fades into the next while the dock takes its new size. The size is the only thing that moves the dock, so nothing fights it.
fun <T> AnimatedContentTransitionScope<T>.panelChange(reduce: Boolean, none: T): ContentTransform {
    val size = SizeTransform(clip = true) { _, _ -> if (reduce) tween<IntSize>(150) else settle() }
    if (reduce) return (fadeIn(tween(150)) togetherWith fadeOut(tween(100))) using size
    val opening = initialState == none
    val closing = targetState == none
    val enter = if (closing) {
        EnterTransition.None
    } else {
        fadeIn(tween(220, delayMillis = if (opening) 70 else 40)) +
            scaleIn(settle(), initialScale = 0.96f, transformOrigin = TransformOrigin(0.5f, 1f))
    }
    val exit = fadeOut(tween(if (closing) 140 else 90))
    return (enter togetherWith exit) using size
}
