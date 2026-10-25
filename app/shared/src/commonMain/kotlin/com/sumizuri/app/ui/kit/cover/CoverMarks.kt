package com.sumizuri.app.ui.kit.cover

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.theme.LocalThemeSpec
import com.sumizuri.app.ui.theme.readableOn

// The colors of the cover of the page on show, for the pieces that mark chapters. Null on a page without a cover.
val LocalCoverPalette = compositionLocalOf<CoverPalette?> { null }

// The colors chapters are marked in: the dot of a chapter not read yet, the bar of how far one was read, and the bookmark.
@Immutable
data class MarkColors(val dot: Color, val bar: List<Color>, val bookmark: Color)

// The marks in the colors of the cover, each moved just enough to stand out on the page, or in the accents of the theme when there is no cover or the theme keeps its own.
@Composable
fun rememberMarkColors(): MarkColors {
    val palette = LocalCoverPalette.current
    val scheme = MaterialTheme.colorScheme
    val follow = LocalThemeSpec.current.options.pageColors.chapterMarks
    return remember(palette, scheme, follow) {
        val page = scheme.surface
        val picked = palette
            ?.takeIf { follow }
            ?.colors
            ?.map { readableOn(it, page) }
            .orEmpty()
        if (picked.isEmpty()) {
            MarkColors(scheme.primary, listOf(scheme.primary, scheme.tertiary), scheme.primary)
        } else {
            val bar = if (picked.size > 1) picked else listOf(picked[0], picked[0])
            MarkColors(picked[0], bar, picked.getOrElse(1) { picked[0] })
        }
    }
}

// How far a chapter was read, as a thin bar that runs through the colors of the marks.
@Composable
fun MarkBar(progress: Float, colors: List<Color>, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier.fillMaxWidth().height(3.dp)) {
        val round = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(track, cornerRadius = round)
        val filled = size.width * progress.coerceIn(0f, 1f)
        if (filled > 0f) {
            drawRoundRect(
                Brush.horizontalGradient(colors, endX = size.width),
                size = Size(filled, size.height),
                cornerRadius = round,
            )
        }
    }
}
