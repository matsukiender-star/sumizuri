package com.sumizuri.app.core.perf

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// The memory, the processor and the threads, read again this often.
private const val SystemEveryMs = 1_000L

// The details of the advanced level take a few queries, so they are read less often.
private const val DetailsEveryMs = 2_000L

// The colors of the box: amber when frames run slow, red when they run very slow, and light blue for the details lines.
private val WarnColor = Color(0xFFFFB74D)
private val BadColor = Color(0xFFFF6E6E)
private val DetailColor = Color(0xFFB3E5FC)

// A small box over the app with the frames, the memory and the processor, read live, and with details lines from the app at the advanced level. It can be dragged anywhere, its place is given as a share of the free room from the left and the top, and a tap folds it down to the frames.
@Composable
fun PerfOverlay(
    details: (suspend () -> List<String>)?,
    placed: Offset?,
    onPlaced: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val frames by rememberFrameStats()
    var memory by remember { mutableStateOf(memoryUse()) }
    var cpu by remember { mutableStateOf<Float?>(null) }
    var threads by remember { mutableStateOf(threadCount()) }
    var lines by remember { mutableStateOf(emptyList<String>()) }
    var folded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = systemUse()
            memory = now.memory
            cpu = now.cpu
            threads = now.threads
            delay(SystemEveryMs)
        }
    }
    LaunchedEffect(details, folded) {
        if (details == null || folded) {
            lines = emptyList()
            return@LaunchedEffect
        }
        while (true) {
            lines = runCatching { details() }.getOrElse { listOf("details failed, ${it.message}") }
            delay(DetailsEveryMs)
        }
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val room = with(density) { IntSize(maxWidth.roundToPx(), maxHeight.roundToPx()) }
        // A phone has little room, so the box is smaller there and its long lines wrap.
        val small = maxWidth < 600.dp
        // The box keeps one width, wide enough for its lines, so it does not shift about as the numbers change.
        val width = if (small) 172.dp else 244.dp
        val widthPx = with(density) { width.roundToPx() }.coerceAtMost(room.width)
        var height by remember { mutableIntStateOf(0) }
        val free = IntSize((room.width - widthPx).coerceAtLeast(0), (room.height - height).coerceAtLeast(0))
        // The place the person chose, kept here at once, so the box stays where it was let go while the setting is written. It starts in the top right corner, except on a phone, where that corner holds the buttons of every page, so it starts low on the right there.
        var share by remember(
            placed,
            small,
        ) { mutableStateOf(placed ?: if (small) Offset(1f, 0.72f) else Offset(1f, 0f)) }
        var dragged by remember { mutableStateOf<Offset?>(null) }
        val at = dragged ?: Offset(share.x * free.width, share.y * free.height)
        // The drag is set up once and reads the room and the place as they are, so a change of the numbers never cuts a drag short.
        val freeNow by rememberUpdatedState(free)
        val atNow by rememberUpdatedState(at)
        // Only the box takes touches, so the app under the rest of the screen works as usual.
        Box(
            Modifier
                .offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }
                .width(width)
                .onSizeChanged { height = it.height }
                .clip(MaterialTheme.shapes.small)
                .background(Color.Black.copy(alpha = 0.78f))
                .pointerInput(Unit) { detectTapGestures { folded = !folded } }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragged = atNow },
                        onDragEnd = {
                            val room = freeNow
                            dragged?.let {
                                val x = if (room.width == 0) 0f else it.x / room.width
                                val y = if (room.height == 0) 0f else it.y / room.height
                                share = Offset(x, y)
                                onPlaced(share)
                            }
                            dragged = null
                        },
                        onDragCancel = { dragged = null },
                    ) { change, amount ->
                        change.consume()
                        val room = freeNow
                        val now = (dragged ?: atNow) + amount
                        dragged =
                            Offset(now.x.coerceIn(0f, room.width.toFloat()), now.y.coerceIn(0f, room.height.toFloat()))
                    }
                },
        ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                val fpsColor = when {
                    frames.idle || frames.fps >= 50f || frames.total == 0 -> Color.White
                    frames.fps >= 30f -> WarnColor
                    else -> BadColor
                }
                Line(
                    small,
                    if (frames.idle) {
                        "fps idle"
                    } else {
                        "${frames.fps.roundToInt().toString().padStart(
                            3,
                        )} fps ${frames.averageMs.oneDecimal().padStart(6)} ms"
                    },
                    fpsColor,
                )
                if (folded) return@Column
                Line(
                    small,
                    "slow ${frames.slow}  worst ${frames.worstMs.roundToInt()} ms",
                    if (frames.slow >
                        0
                    ) {
                        WarnColor
                    } else {
                        Color.White
                    },
                )
                Line(
                    small,
                    "mem ${memory.usedMb.toString().padStart(4)}/${memory.maxMb} MB" +
                        (memory.nativeMb?.let { "  native $it" } ?: "") +
                        (memory.processMb?.let { "  ram $it" } ?: ""),
                )
                Line(small, "cpu ${(cpu?.let { it.oneDecimal() + "%" } ?: "…").padStart(6)}  threads $threads")
                for (line in lines) Line(small, line, DetailColor)
            }
        }
    }
}

@Composable
private fun Line(small: Boolean, text: String, color: Color = Color.White) {
    Text(
        text,
        color = color,
        fontSize = if (small) 9.sp else 11.sp,
        lineHeight = if (small) 11.sp else 14.sp,
        fontFamily = FontFamily.Monospace,
    )
}

// A number with one figure after the point.
internal fun Float.oneDecimal(): String {
    val tenths = (this * 10).roundToInt()
    return "${tenths / 10}.${kotlin.math.abs(tenths % 10)}"
}
