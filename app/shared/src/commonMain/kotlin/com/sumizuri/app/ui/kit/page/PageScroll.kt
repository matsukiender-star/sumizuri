package com.sumizuri.app.ui.kit.page

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.sumizuri.app.ui.kit.scroll.mouseDragScroll

// Where the pages that asked to be remembered were scrolled to when they were left, so coming back to them finds the same place.
private val keptScroll = HashMap<String, Int>()

// A column that scrolls with the wheel, a finger, and a mouse drag. With a name for the page it also comes back to where it was when the page is left and opened again.
@Composable
fun Modifier.pageScroll(keep: String? = null): Modifier {
    val state = rememberScrollState(keep?.let { keptScroll[it] } ?: 0)
    if (keep != null) DisposableEffect(state) { onDispose { keptScroll[keep] = state.value } }
    return mouseDragScroll(state).verticalScroll(state).padding(bottom = LocalBottomClearance.current)
}
