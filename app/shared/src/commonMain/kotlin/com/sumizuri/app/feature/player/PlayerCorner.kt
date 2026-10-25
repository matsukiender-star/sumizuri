package com.sumizuri.app.feature.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// A place at an edge of the player for a button or a panel, kept clear of the notch and the bars of the system on the sides given.
@Composable
internal fun BoxScope.PlayerCorner(
    alignment: Alignment,
    sides: WindowInsetsSides,
    padding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier.align(alignment).windowInsetsPadding(WindowInsets.safeDrawing.only(sides)).padding(padding),
    ) { content() }
}
