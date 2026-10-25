package com.sumizuri.app.ui.theme.background

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.theme.spec.BackgroundStyle
import com.sumizuri.app.ui.theme.spec.ThemeBackground
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

// How long one slow turn of the shapes and of the ring takes, in milliseconds.
private const val TurnMs = 40_000.0
private const val RingTurnMs = 90_000.0

// How many steps of strength the dots and the washes are drawn in, so their drawing reuses a few kept pieces.
private const val DotLevels = 6
private const val AlphaSteps = 32

// The most gradients kept, past which the kept ones are let go and made again as needed.
private const val MostBrushes = 192

// Draws the moving background. It keeps what it made from one frame to the next, the gradients, the path of the waves and the points of the dots, so a frame makes almost nothing new. One painter belongs to one page.
internal class BackdropPainter {
    private val brushes = HashMap<Long, Brush>()
    private val wave = Path()
    private val dotPaint = Paint().apply { strokeCap = StrokeCap.Round }
    private var dotPoints = Array(DotLevels) { FloatArray(0) }
    private var dotCounts = IntArray(DotLevels)

    // The whole background at a time, in a list of colors used in turn, at a strength from none to three times as strong.
    fun DrawScope.draw(time: Long, look: ThemeBackground, colors: List<Color>, strength: Float, dark: Boolean) {
        if (colors.isEmpty()) return
        val t = ((time % TurnMs.toLong()) / TurnMs * 2 * PI).toFloat()
        fun color(index: Int) = colors[index % colors.size]
        if (strength > 0f) {
            when (look.style) {
                BackgroundStyle.Ink -> ink(t, look, ::color, strength)

                BackgroundStyle.Aurora -> aurora(t, look, ::color, strength)

                BackgroundStyle.Orbs -> orbs(t, look, ::color, strength)

                BackgroundStyle.Waves -> waves(t, look, ::color, strength)

                BackgroundStyle.Dots -> dots(t, look, ::color, strength)

                BackgroundStyle.Strokes -> strokes(t, look, ::color, strength)

                // The covers are drawn by a layer of their own, which can load pictures.
                BackgroundStyle.Covers -> Unit
            }
            if (look.ring) ring(time, look, color(0), strength)
        }
        if (look.vignette > 0f) {
            val span = max(size.width, size.height)
            val edge = Color.Black.copy(alpha = look.vignette * if (dark) 0.7f else 0.25f)
            val shade = Brush.radialGradient(
                0f to Color.Transparent,
                0.5f to Color.Transparent,
                1f to edge,
                center = center,
                radius =
                span * 0.75f,
            )
            drawRect(shade)
        }
    }

    // The ink the app has always had: four washes in their own places, and more scattered about when more are asked for.
    private fun DrawScope.ink(t: Float, look: ThemeBackground, color: (Int) -> Color, strength: Float) {
        val (w, h) = size.width to size.height
        val span = max(w, h)
        val d = look.drift
        val s = look.scale
        if (look.count >= 1) {
            wash(
                color(0),
                Offset(w * (0.3f + 0.14f * d * sin(t)), h * (0.22f + 0.08f * d * cos(t))),
                span * 0.6f * s,
                0.2f * strength,
            )
        }
        if (look.count >= 2) {
            val at = Offset(w * (0.85f + 0.1f * d * cos(t * 0.7f)), h * (0.72f + 0.1f * d * sin(t * 0.7f)))
            wash(color(2), at, span * 0.65f * s, 0.16f * strength)
        }
        if (look.count >=
            3
        ) {
            wash(
                color(1),
                Offset(w * (0.05f + 0.1f * d * sin(t * 1.3f)), h * 0.95f),
                span * 0.5f * s,
                0.1f * strength,
            )
        }
        if (look.count >= 4) {
            wash(
                color(0),
                Offset(w * (0.78f + 0.12f * d * sin(t)), h * (0.06f + 0.06f * d * cos(t))),
                w * 0.75f * s,
                0.07f * strength,
            )
        }
        for (i in 4 until look.count) {
            val turn = t * (0.5f + noise(i, 3)) + noise(i, 4) * 6.28f
            val at = Offset(w * (noise(i, 1) + 0.12f * d * sin(turn)), h * (noise(i, 2) + 0.1f * d * cos(turn)))
            wash(color(i), at, span * (0.25f + 0.3f * noise(i, 5)) * s, (0.08f + 0.08f * noise(i, 6)) * strength)
        }
    }

    // Wide soft bands of light across the top of the page, swaying side to side.
    private fun DrawScope.aurora(t: Float, look: ThemeBackground, color: (Int) -> Color, strength: Float) {
        val (w, h) = size.width to size.height
        val span = max(w, h)
        for (i in 0 until look.count) {
            val sway = h * 0.05f * look.drift * sin(t * (0.6f + 0.2f * i) + i)
            val y = h * (0.08f + 0.55f * i / look.count.coerceAtLeast(1)) + sway
            val x = w * (0.5f + 0.28f * look.drift * sin(t * (0.4f + 0.15f * i) + i * 1.7f))
            val at = Offset(x, y)
            scale(3f, 0.5f, pivot = at) { wash(color(i), at, span * 0.32f * look.scale, 0.18f * strength) }
        }
    }

    // Small lights floating about the page, each fading in and out on its own.
    private fun DrawScope.orbs(t: Float, look: ThemeBackground, color: (Int) -> Color, strength: Float) {
        val (w, h) = size.width to size.height
        val span = max(w, h)
        for (i in 0 until look.count * 3) {
            val turn = t * (0.6f + noise(i, 3)) + noise(i, 4) * 6.28f
            val at = Offset(
                w * (noise(i, 1) + 0.08f * look.drift * sin(turn)),
                h * (noise(i, 2) + 0.08f * look.drift * cos(turn * 0.8f)),
            )
            val pulse = 0.65f + 0.35f * sin(t * 3f + noise(i, 7) * 6.28f)
            wash(
                color(i),
                at,
                span * (0.04f + 0.08f * noise(i, 5)) * look.scale,
                (0.18f + 0.18f * noise(i, 6)) * pulse * strength,
            )
        }
    }

    // Layers of waves rising from the bottom of the page, each rolling at its own pace, drawn with one kept path.
    private fun DrawScope.waves(t: Float, look: ThemeBackground, color: (Int) -> Color, strength: Float) {
        val (w, h) = size.width to size.height
        val step = 12.dp.toPx()
        for (i in 0 until look.count) {
            val base = h * (0.5f + 0.45f * (i + 1) / (look.count + 1))
            val height = 26.dp.toPx() * look.scale * max(look.drift, 0.2f)
            val length = w / (1.1f + 0.35f * i)
            val roll = t * (1f + 0.3f * i) * if (i % 2 == 0) 1f else -1f
            wave.reset()
            wave.moveTo(0f, h)
            var x = 0f
            while (x <= w + step) {
                wave.lineTo(x, base + height * sin(x / length * 6.28f + roll + i))
                x += step
            }
            wave.lineTo(w, h)
            wave.close()
            drawPath(wave, color(i).copy(alpha = 0.09f * strength))
        }
    }

    // A field of dots that ripples outward from a few slowly moving points. The dots of each color and strength go in a kept list of numbers and are drawn at once.
    private fun DrawScope.dots(t: Float, look: ThemeBackground, color: (Int) -> Color, strength: Float) {
        val (w, h) = size.width to size.height
        val gap = max(26.dp.toPx() * look.scale, 10.dp.toPx())
        val ripple = 70.dp.toPx() * look.scale
        val sources = look.count.coerceIn(1, 6)
        val most = ((w / gap).toInt() + 1) * ((h / gap).toInt() + 1) * 2
        val groups = sources * DotLevels
        if (dotPoints.size < groups || dotPoints[0].size < most) {
            dotPoints = Array(groups) { FloatArray(most) }
            dotCounts = IntArray(groups)
        }
        dotCounts.fill(0)
        var y = gap / 2
        while (y < h) {
            var x = gap / 2
            while (x < w) {
                var nearest = 0
                var close = Float.MAX_VALUE
                for (i in 0 until sources) {
                    val sx = w * (noise(i, 1) + 0.15f * look.drift * sin(t + i))
                    val sy = h * (noise(i, 2) + 0.15f * look.drift * cos(t * 0.8f + i))
                    val apart = hypot(x - sx, y - sy)
                    if (apart < close) {
                        close = apart
                        nearest = i
                    }
                }
                val lit = 0.5f + 0.5f * sin(close / ripple * 6.28f - t * 8f)
                val group = nearest * DotLevels + (lit * (DotLevels - 1)).toInt()
                val at = dotCounts[group]
                dotPoints[group][at] = x
                dotPoints[group][at + 1] = y
                dotCounts[group] = at + 2
                x += gap
            }
            y += gap
        }
        dotPaint.strokeWidth = 3.dp.toPx() * look.scale
        drawIntoCanvas { canvas ->
            for (group in 0 until groups) {
                val count = dotCounts[group]
                if (count == 0) continue
                val alpha = (0.06f + 0.3f * (group % DotLevels) / (DotLevels - 1)) * strength
                dotPaint.color = color(group / DotLevels).copy(alpha = alpha.coerceIn(0f, 1f))
                // The canvas draws every number it is given, so each group is handed only the part it filled.
                canvas.drawRawPoints(PointMode.Points, dotPoints[group].copyOf(count), dotPaint)
            }
        }
    }

    // Wide curved brush strokes, as if made with sumi ink, turning slowly round their own centers.
    private fun DrawScope.strokes(t: Float, look: ThemeBackground, color: (Int) -> Color, strength: Float) {
        val (w, h) = size.width to size.height
        val span = max(w, h)
        for (i in 0 until look.count) {
            val at = Offset(
                w * (noise(i, 1) + 0.06f * look.drift * sin(t + i)),
                h * (noise(i, 2) + 0.06f * look.drift * cos(t + i)),
            )
            val radius = span * (0.18f + 0.22f * noise(i, 5)) * look.scale
            val start = noise(i, 3) * 360f + t * (8f + 10f * noise(i, 4)) * if (i % 2 == 0) 1f else -1f
            val sweep = 60f + 100f * noise(i, 6)
            val width = (16f + 26f * noise(i, 7)).dp.toPx() * look.scale
            val box = Size(radius * 2, radius * 2)
            val corner = Offset(at.x - radius, at.y - radius)
            drawArc(
                color(i).copy(alpha = 0.12f * strength),
                start,
                sweep,
                false,
                corner,
                box,
                style = Stroke(width, cap = StrokeCap.Round),
            )
            // A thinner pass along the same curve gives the stroke the uneven edge of a brush.
            val edge = Offset(corner.x + width * 0.2f, corner.y)
            drawArc(
                color(i).copy(alpha = 0.08f * strength),
                start + 4f,
                sweep - 10f,
                false,
                edge,
                box,
                style = Stroke(width * 0.45f, cap = StrokeCap.Round),
            )
        }
    }

    // The faint ring like the one on the icon, near the top corner, turning very slowly.
    private fun DrawScope.ring(time: Long, look: ThemeBackground, color: Color, strength: Float) {
        val turn = ((time % RingTurnMs.toLong()) / RingTurnMs * 360).toFloat()
        val middle = Offset(size.width * 0.92f, size.height * 0.1f)
        val radius = max(size.width, size.height) * 0.42f * look.scale
        rotate(turn, middle) {
            drawArc(
                color.copy(alpha = 0.12f * strength),
                0f,
                300f,
                false,
                topLeft = Offset(middle.x - radius, middle.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }

    // One soft wash of a color, strongest at its center and gone at its edge. The gradient is made once for each color, strength and size and moved into place, so a frame makes no new one.
    private fun DrawScope.wash(color: Color, center: Offset, radius: Float, strength: Float) {
        if (radius <= 0f) return
        val alpha = (strength.coerceIn(0f, 1f) * AlphaSteps).roundToInt()
        if (alpha == 0) return
        val reach = radius.roundToInt().coerceAtLeast(1)
        val key =
            ((color.toArgb().toLong() and 0xFFFFFFFFL) shl 32) or (alpha.toLong() shl 24) or
                (reach.toLong() and 0xFFFFFF)
        if (brushes.size > MostBrushes) brushes.clear()
        val brush = brushes.getOrPut(key) {
            val inner = color.copy(alpha = alpha.toFloat() / AlphaSteps)
            Brush.radialGradient(listOf(inner, Color.Transparent), center = Offset.Zero, radius = reach.toFloat())
        }
        translate(center.x, center.y) { drawCircle(brush, reach.toFloat(), Offset.Zero) }
    }
}

// A number from 0 to 1 that is always the same for the same shape and question, so shapes keep their places from one moment to the next.
private fun noise(index: Int, question: Int): Float {
    val x = sin(index * 12.9898f + question * 78.233f) * 43758.547f
    return x - floor(x)
}
