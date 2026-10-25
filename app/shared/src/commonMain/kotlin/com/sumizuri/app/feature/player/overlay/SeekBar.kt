package com.sumizuri.app.feature.player.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sumizuri.app.feature.player.formatTime
import com.sumizuri.app.playback.VideoThumbnail
import kotlinx.coroutines.delay

// The line of the player: the time played, the line that shows how far the video has played and how far it is loaded and is dragged or tapped to jump, and the length. While it is held it shows the place of the finger, and the jump is made when it is let go. The callbacks say when a drag starts and ends so the controls do not hide under the finger.
@Composable
internal fun SeekBar(
    positionMs: Long,
    bufferedMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier,
    // The small picture of the spot last asked for, and the way to ask for one, for the engines that make them.
    thumbnail: VideoThumbnail? = null,
    onPreview: ((Long) -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    var dragged by remember { mutableStateOf<Float?>(null) }
    var width by remember { mutableFloatStateOf(1f) }
    val known = durationMs > 0
    val fraction = dragged ?: if (known) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val loaded = if (known) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val shown = dragged?.let { (it * durationMs).toLong() } ?: positionMs
    // The spot under the pointer on a computer, which shows its picture like a drag does.
    var hover by remember { mutableStateOf<Float?>(null) }
    var bar by remember { mutableIntStateOf(0) }
    val asked = (dragged ?: hover)?.takeIf { known && onPreview != null }
    LaunchedEffect(asked) {
        val at = asked ?: return@LaunchedEffect
        delay(PreviewPauseMs)
        onPreview?.invoke((at * durationMs).toLong())
    }
    val lead = with(LocalDensity.current) { TimeWidth.roundToPx() }
    asked?.let { SeekPreview(lead + (it * bar).toInt(), formatTime((it * durationMs).toLong()), thumbnail) }
    Row(modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
        Time(formatTime(shown), lead = true)
        Canvas(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .onSizeChanged { bar = it.width }
                .pointerInput(known) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            hover = when (event.type) {
                                PointerEventType.Move, PointerEventType.Enter -> (
                                    event.changes
                                        .first()
                                        .position.x /
                                        size.width
                                    ).coerceIn(0f, 1f)

                                PointerEventType.Exit -> null

                                else -> hover
                            }
                        }
                    }
                }.pointerInput(known, durationMs) {
                    if (known) {
                        detectTapGestures { offset ->
                            onSeek(
                                (offset.x / size.width * durationMs).toLong().coerceIn(0, durationMs),
                            )
                        }
                    }
                }.pointerInput(known, durationMs) {
                    if (!known) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            width = size.width.toFloat()
                            dragged = (offset.x / width).coerceIn(0f, 1f)
                            onScrubStart()
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragged = (change.position.x / width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            dragged?.let { onSeek((it * durationMs).toLong()) }
                            dragged = null
                            onScrubEnd()
                        },
                        onDragCancel = {
                            dragged = null
                            onScrubEnd()
                        },
                    )
                },
        ) {
            // The same track as the reader: thin and round, thicker and with a bigger thumb while it is held, the loaded part a shade over the rest.
            val held = dragged != null
            val track = (if (held) 6.dp else 4.dp).toPx()
            val middle = size.height / 2
            fun line(color: androidx.compose.ui.graphics.Color, share: Float) {
                if (share >
                    0f
                ) {
                    drawLine(color, Offset(0f, middle), Offset(size.width * share, middle), track, StrokeCap.Round)
                }
            }
            line(scheme.surfaceContainerHighest, 1f)
            line(scheme.onSurface.copy(alpha = 0.22f), loaded)
            line(scheme.primary, fraction)
            val center = Offset(size.width * fraction, middle)
            val knob = (if (held) 10.dp else 7.dp).toPx()
            drawCircle(scheme.surface, knob + 2.dp.toPx(), center)
            drawCircle(scheme.primary, knob, center)
        }
        Time(formatTime(durationMs))
    }
}

@Composable
private fun Time(text: String, lead: Boolean = false) {
    // The time played stands out in the accent, as the page does in the reader.
    Text(
        text,
        Modifier.width(TimeWidth),
        color = if (lead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = if (lead) FontWeight.SemiBold else FontWeight.Normal,
        fontSize = 12.5.sp,
        textAlign = TextAlign.Center,
        style = TextStyle(fontFeatureSettings = "tnum"),
    )
}

// How wide the times at the ends of the bar are, which is where the bar itself starts.
private val TimeWidth = 52.dp

// How long the pointer rests on a spot before its picture is asked for, so a quick sweep does not ask for every spot.
private const val PreviewPauseMs = 90L
