package com.sumizuri.app.feature.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.text.nowMillis
import kotlin.math.ceil
import kotlin.math.roundToInt

// The pace of the person in tenths of a second for each page, learned as they read and kept between runs.
private const val PaceKey = "pace.pageTenths"
private const val UsualTenths = 80

// A page turned sooner than this was skipped, and one left longer than this was put down, so neither counts.
private const val FastestTenths = 8
private const val SlowestTenths = 1800

// The pace is saved after this many pages, so a page turn does not write to the database each time.
private const val SaveEvery = 10

// The minutes left in a chapter, or null when it cannot be told. Pictures take the pace of the person, learned from the pages they turn one after another. Words take the reading speed set for the reader, for the part not read yet.
@Composable
internal fun rememberMinutesLeft(state: ReaderState, page: Int?, total: Int): Int? {
    val settings = LocalAppContainer.current.settings
    val saved by remember(settings) { settings.number(PaceKey, UsualTenths) }.collectAsStateWithLifecycle(UsualTenths)
    var pace by remember { mutableIntStateOf(-1) }
    var lastPage by remember { mutableIntStateOf(-1) }
    var lastAt by remember { mutableLongStateOf(0L) }
    var counted by remember { mutableIntStateOf(0) }
    val now = if (pace < 0) saved else pace
    LaunchedEffect(page) {
        val at = nowMillis()
        val shown = page ?: return@LaunchedEffect
        val spent = ((at - lastAt) / 100).toInt()
        if (!state.isText && shown == lastPage + 1 && spent in FastestTenths..SlowestTenths) {
            pace = (now * 0.85 + spent * 0.15).roundToInt()
            counted++
            if (counted % SaveEvery == 0) settings.setNumber(PaceKey, pace)
        }
        lastPage = shown
        lastAt = at
    }
    val at = page ?: return null
    if (total <= 0) return null
    val leftPart = (total - at - 1).coerceAtLeast(0)
    return if (state.isText) {
        if (state.words <=
            0
        ) {
            null
        } else {
            ceil(state.words * leftPart.toDouble() / total / state.settings.readingWpm).toInt()
        }
    } else {
        ceil(leftPart * now / 600.0).toInt()
    }
}
