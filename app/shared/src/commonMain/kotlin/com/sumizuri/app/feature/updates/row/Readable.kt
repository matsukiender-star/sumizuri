package com.sumizuri.app.feature.updates.row

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sumizuri.app.ui.kit.page.ReadableWidth

// Keeps rows in a column that is readable on a wide window, centered like the bar and the chips above it, and lets a grid use all the width.
@Composable
internal fun Readable(narrow: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (!narrow) {
        content()
        return
    }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = ReadableWidth).fillMaxWidth()) { content() }
    }
}
