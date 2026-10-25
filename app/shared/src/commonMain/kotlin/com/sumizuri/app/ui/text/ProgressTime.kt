package com.sumizuri.app.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.progress_elapsed
import com.sumizuri.app.shared.resources.progress_left_hours
import com.sumizuri.app.shared.resources.progress_left_minutes
import com.sumizuri.app.shared.resources.progress_left_soon
import com.sumizuri.app.shared.resources.progress_time
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

// How far into a job the clock waits before it guesses what is left: a few steps done and a few seconds gone, so one quick or slow first step does not throw the guess.
private const val GuessAfterSteps = 3
private const val GuessAfterMs = 4_000L

private const val MinuteMs = 60_000L
private const val HourMs = 60 * MinuteMs

// The time a job has run and a guess at what it has left, in milliseconds. Elapsed is null when the clock did not see the job start, and left is null while there is too little to go on.
data class ProgressTime(val elapsedMs: Long?, val leftMs: Long?)

// Keeps when a job was first seen and how far it was then, and guesses how long it has left from how fast it went since. A count that starts again, such as a second pass from nought, or a total that changed, starts the clock again.
class ProgressClock {
    private var start = -1L
    private var startDone = 0
    private var lastDone = 0
    private var lastTotal = -1

    fun read(done: Int, total: Int, now: Long): ProgressTime {
        if (start < 0 || total != lastTotal || done < lastDone) {
            start = now
            startDone = done
        }
        lastDone = done
        lastTotal = total
        val elapsed = now - start
        val steps = done - startDone
        val left = if (steps >= GuessAfterSteps && elapsed >= GuessAfterMs &&
            done < total
        ) {
            elapsed * (total - done) / steps
        } else {
            null
        }
        return ProgressTime(elapsed.takeIf { startDone == 0 }, left)
    }
}

// The line under a running job: how long it has run and about how long it has left, such as 2:13 · about 4 minutes left. Null before there is anything to say. The clock moves every second while the job runs.
@Composable
fun progressTimeText(done: Int, total: Int): String? {
    val clock = remember { ProgressClock() }
    val now by produceState(nowMillis()) {
        while (true) {
            delay(1_000)
            value = nowMillis()
        }
    }
    val time = clock.read(done, total, now)
    val elapsed = time.elapsedMs?.let(::clockText)
    val left = time.leftMs?.let { ms ->
        when {
            ms < MinuteMs -> stringResource(Res.string.progress_left_soon)

            ms < HourMs -> ((ms + MinuteMs - 1) / MinuteMs).toInt().let {
                pluralStringResource(Res.plurals.progress_left_minutes, it, it)
            }

            else -> stringResource(
                Res.string.progress_left_hours,
                (ms / HourMs).toInt(),
                ((ms % HourMs) / MinuteMs).toInt(),
            )
        }
    }
    return when {
        elapsed != null && left != null -> stringResource(Res.string.progress_time, elapsed, left)
        left != null -> left
        elapsed != null -> stringResource(Res.string.progress_elapsed, elapsed)
        else -> null
    }
}

// A length of time as a clock shows it, such as 4:05 or 1:02:09.
fun clockText(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    fun two(n: Long) = n.toString().padStart(2, '0')
    return if (h > 0) "$h:${two(m)}:${two(s)}" else "$m:${two(s)}"
}
