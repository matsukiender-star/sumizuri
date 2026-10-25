package com.sumizuri.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.sumizuri.app.data.text.localOffsetMillis
import com.sumizuri.app.data.text.nowMillis
import kotlinx.coroutines.delay

// The accent at hours of the day, from the pink of dawn through the gold of morning, the blue of midday, the amber of the afternoon and the violet of dusk to the indigo of night. Between two hours the accent is a mix of both.
private val HoursAndColors = listOf(
    2f to Color(0xFF3A4FA0),
    5.5f to Color(0xFFE8758A),
    8f to Color(0xFFE0A030),
    12f to Color(0xFF2F8FD8),
    16.5f to Color(0xFFE07A3A),
    19f to Color(0xFF8A5CD8),
    22f to Color(0xFF4F5BD5),
)

// How often the accent of the hour is looked at again. It changes too slowly for more.
private const val LookEveryMs = 5 * 60 * 1000L

// The accent for the time of day now, looked at again every few minutes.
@Composable
internal fun rememberClockAccent(): Color = produceState(accentAt(hourNow())) {
    while (true) {
        delay(LookEveryMs)
        value = accentAt(hourNow())
    }
}.value

// The hour of the day now as a number with its minutes, such as 13.5 for half past one.
private fun hourNow(): Float {
    val now = nowMillis()
    val minutes = ((now + localOffsetMillis(now)) / 60_000L) % (24 * 60)
    return minutes / 60f
}

// The accent at an hour, mixed from the two hours around it, going round past midnight.
internal fun accentAt(hour: Float): Color {
    val stops = HoursAndColors
    val after = stops.indexOfFirst { it.first > hour }.let { if (it == -1) 0 else it }
    val before = (after - 1 + stops.size) % stops.size
    val (fromHour, fromColor) = stops[before]
    val (toHour, toColor) = stops[after]
    val span = ((toHour - fromHour) + 24f) % 24f
    val gone = ((hour - fromHour) + 24f) % 24f
    return lerp(fromColor, toColor, if (span == 0f) 0f else gone / span)
}
