package com.sumizuri.app.feature.reader.view

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.sumizuri.app.data.model.reader.ReaderPageTurn
import kotlin.math.abs

// How a page of the pager looks as it turns. The pager still moves each page into place, and this layer changes what that looks like: a slide is left as it is, and the other styles hold the page where it is and fade, stack, flip or shrink it instead.
internal fun Modifier.pageTurn(
    pager: PagerState,
    index: Int,
    style: ReaderPageTurn,
    vertical: Boolean,
    reversed: Boolean,
): Modifier {
    if (style == ReaderPageTurn.Slide) return this
    return graphicsLayer {
        // How far the page is from the middle of the screen, in pages: below zero before it comes in, above zero as it leaves.
        val offset = (pager.currentPage - index) + pager.currentPageOffsetFraction
        val away = abs(offset).coerceAtMost(1f)
        val sign = if (reversed) -1f else 1f
        fun hold() {
            if (vertical) translationY = offset * size.height else translationX = sign * offset * size.width
        }
        when (style) {
            ReaderPageTurn.Slide -> Unit

            ReaderPageTurn.Fade -> {
                hold()
                alpha = 1f - away
            }

            // The page that leaves stays put and sinks back, and the next one slides over it.
            ReaderPageTurn.Stack -> if (offset > 0f) {
                hold()
                val shrink = 1f - 0.12f * away
                scaleX = shrink
                scaleY = shrink
                alpha = 1f - 0.6f * away
            }

            // A card turned over: the page on show turns away to its edge, and the next turns in from its own.
            ReaderPageTurn.Flip -> {
                hold()
                cameraDistance = 14f * density
                if (vertical) rotationX = -offset * 180f else rotationY = sign * offset * 180f
                alpha = if (away > 0.5f) 0f else 1f
            }

            ReaderPageTurn.Zoom -> {
                val shrink = 1f - 0.25f * away
                scaleX = shrink
                scaleY = shrink
                alpha = 1f - 0.5f * away
            }
        }
    }
}
