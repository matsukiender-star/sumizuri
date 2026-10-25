package com.sumizuri.app.ui.kit.scroll

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy

// The lists that are on screen, so a double tap on a tab can send the one shown last back to its top.
object ScrollTops {
    private val lists = mutableListOf<ScrollableState>()

    internal fun add(state: ScrollableState) {
        lists += state
    }

    internal fun remove(state: ScrollableState) {
        lists -= state
    }

    // Says whether there was a list to scroll.
    suspend fun toTop(): Boolean {
        val state = lists.lastOrNull() ?: return false
        state.animateScrollBy(-TopReach)
        return true
    }

    // Far enough up to reach the top of any list.
    private const val TopReach = 1_000_000f
}
