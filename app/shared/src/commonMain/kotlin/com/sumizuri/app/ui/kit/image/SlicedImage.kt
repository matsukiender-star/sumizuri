package com.sumizuri.app.ui.kit.image

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.min
import kotlin.math.roundToInt

// A picture that was cut into pieces, drawn as one in the room it is given. Each piece ends where the next begins, so no seam shows. The bias moves a smaller picture left, to the middle or right.
@Composable
fun SlicedImage(
    slices: List<ImageBitmap>,
    modifier: Modifier = Modifier,
    fit: ImageFit = ImageFit.Width,
    bias: Float = 0.5f,
    visibleRangePx: (() -> ClosedFloatingPointRange<Float>?)? = null,
) {
    val width = slices.first().width
    val height = slices.sumOf { it.height }
    val ratio = width.toFloat() / height
    val density = LocalDensity.current
    val sized = when (fit) {
        ImageFit.Width -> modifier.fillMaxWidth().aspectRatio(ratio)
        ImageFit.Height -> modifier.fillMaxHeight().aspectRatio(ratio, matchHeightConstraintsFirst = true)
        ImageFit.Screen -> modifier.fillMaxSize()
        ImageFit.Original -> modifier.requiredSize(with(density) { width.toDp() }, with(density) { height.toDp() })
    }
    val filter = LocalPageFilter.current
    Spacer(sized.drawBehind { drawSlices(slices, width, height, bias, filter, visibleRangePx?.invoke()) })
}

// A color change laid over the pictures of the reader, such as gray or turned around, or null for the pictures as they are.
val LocalPageFilter = compositionLocalOf<ColorFilter?> { null }

// The filter for the choices of the reader, or null when none is on. Gray, old paper brown, a stronger or softer contrast and colors turned around can all be on together.
fun pageFilterOf(gray: Boolean, invert: Boolean, sepia: Float = 0f, contrast: Float = 1f): ColorFilter? {
    if (!gray && !invert && sepia <= 0f && contrast == 1f) return null
    val matrix = ColorMatrix()
    if (gray) matrix.setToSaturation(0f)
    if (sepia > 0f) matrix *= sepiaMatrix(sepia.coerceIn(0f, 1f))
    if (contrast != 1f) {
        // Each color is pushed away from the middle gray, so the darks get darker and the lights lighter.
        val shift = 127.5f * (1f - contrast)
        matrix *= ColorMatrix(floatArrayOf(contrast, 0f, 0f, 0f, shift, 0f, contrast, 0f, 0f, shift, 0f, 0f, contrast, 0f, shift, 0f, 0f, 0f, 1f, 0f))
    }
    if (invert) {
        matrix *=
            ColorMatrix(
                floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f),
            )
    }
    return ColorFilter.colorMatrix(matrix)
}

private fun DrawScope.drawSlices(
    slices: List<ImageBitmap>,
    sourceWidth: Int,
    sourceHeight: Int,
    bias: Float,
    filter: ColorFilter?,
    visibleRangePx: ClosedFloatingPointRange<Float>?,
) {
    val scale = min(size.width / sourceWidth, size.height / sourceHeight)
    val left = (size.width - sourceWidth * scale) * bias
    val above = (size.height - sourceHeight * scale) / 2f
    val from = left.roundToInt()
    val across = (left + sourceWidth * scale).roundToInt() - from
    var firstVisible = -1
    var lastVisible = -1
    if (visibleRangePx != null) {
        var offset = 0
        for (index in slices.indices) {
            val start = (above + offset * scale).roundToInt()
            offset += slices[index].height
            val end = (above + offset * scale).roundToInt()
            if (end > visibleRangePx.start && start < visibleRangePx.endInclusive) {
                if (firstVisible == -1) firstVisible = index
                lastVisible = index
            }
        }
    }
    var top = 0
    for ((index, slice) in slices.withIndex()) {
        // The edges are rounded to whole pixels from the top, so the pieces always meet exactly.
        val start = (above + top * scale).roundToInt()
        val end = (above + (top + slice.height) * scale).roundToInt()
        // Draw one slice above and below the viewport so its texture is ready as it scrolls in.
        if (visibleRangePx == null ||
            (
                firstVisible != -1 &&
                    index in (firstVisible - 1).coerceAtLeast(0)..(lastVisible + 1).coerceAtMost(slices.lastIndex)
                )
        ) {
            drawImage(
                image = slice,
                srcSize = IntSize(slice.width, slice.height),
                dstOffset = IntOffset(from, start),
                dstSize = IntSize(across, end - start),
                colorFilter = filter,
                filterQuality = ImageFilter,
            )
        }
        top += slice.height
    }
}

// The old paper brown at a strength from 0 to 1, mixed from the colors as they are toward the usual sepia tone.
private fun sepiaMatrix(amount: Float): ColorMatrix {
    val tone = floatArrayOf(0.393f, 0.769f, 0.189f, 0.349f, 0.686f, 0.168f, 0.272f, 0.534f, 0.131f)
    val values = FloatArray(20)
    for (row in 0 until 3) {
        for (column in 0 until 3) {
            val same = if (row == column) 1f else 0f
            values[row * 5 + column] = same + (tone[row * 3 + column] - same) * amount
        }
    }
    values[18] = 1f
    return ColorMatrix(values)
}
