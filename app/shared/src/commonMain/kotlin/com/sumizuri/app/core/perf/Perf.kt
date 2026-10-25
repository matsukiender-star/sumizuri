package com.sumizuri.app.core.perf

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// The moments of a start the app notes, in the order they happen.
enum class StartMark { FirstFrame, LibraryShown }

// How the calls to one source went: how many, how many failed, and how long they took.
data class CallStats(
    val source: String,
    val calls: Int = 0,
    val failures: Int = 0,
    val totalMs: Long = 0,
    val worstMs: Long = 0,
) {
    val averageMs: Long get() = if (calls == 0) 0 else totalMs / calls
}

// How long one kind of work of the app took, such as putting the library list together.
data class WorkStats(
    val name: String,
    val count: Int = 0,
    val lastMs: Long = 0,
    val worstMs: Long = 0,
    val totalMs: Long = 0,
) {
    val averageMs: Long get() = if (count == 0) 0 else totalMs / count
}

// What the app measures about itself for the performance page and the overlay. Recording is a few number updates, cheap enough to stay on all the time.
object Perf {
    private val mutableMarks = MutableStateFlow<Map<StartMark, Long>>(emptyMap())
    val marks: StateFlow<Map<StartMark, Long>> = mutableMarks

    private val mutableCalls = MutableStateFlow<Map<String, CallStats>>(emptyMap())
    val calls: StateFlow<Map<String, CallStats>> = mutableCalls

    private val mutableWork = MutableStateFlow<Map<String, WorkStats>>(emptyMap())
    val work: StateFlow<Map<String, WorkStats>> = mutableWork

    // Notes when a moment of the start happened, only the first time.
    fun mark(mark: StartMark) {
        if (mark in mutableMarks.value) return
        val at = processUptimeMs()
        mutableMarks.update { if (mark in it) it else it + (mark to at) }
    }

    fun recordCall(source: String, ms: Long, ok: Boolean) {
        mutableCalls.update { all ->
            val was = all[source] ?: CallStats(source)
            all +
                (
                    source to
                        was.copy(
                            calls = was.calls + 1,
                            failures = was.failures + if (ok) 0 else 1,
                            totalMs =
                            was.totalMs + ms,
                            worstMs = maxOf(was.worstMs, ms),
                        )
                    )
        }
    }

    fun recordWork(name: String, ms: Long) {
        mutableWork.update { all ->
            val was = all[name] ?: WorkStats(name)
            all +
                (
                    name to
                        was.copy(
                            count = was.count + 1,
                            lastMs = ms,
                            worstMs = maxOf(was.worstMs, ms),
                            totalMs =
                            was.totalMs + ms,
                        )
                    )
        }
    }

    private val mutableRequests = MutableStateFlow(0)

    // The network requests of the sources that are under way right now.
    val requests: StateFlow<Int> = mutableRequests

    fun requestStarted() = mutableRequests.update { it + 1 }

    fun requestEnded() = mutableRequests.update { it - 1 }

    // Starts the counting of calls and work again. The moments of the start stay, since they happen once.
    fun reset() {
        mutableCalls.value = emptyMap()
        mutableWork.value = emptyMap()
    }
}

// Times a piece of work and records it under the name.
inline fun <T> timed(name: String, block: () -> T): T {
    val start = kotlin.time.TimeSource.Monotonic
        .markNow()
    val result = block()
    Perf.recordWork(name, start.elapsedNow().inWholeMilliseconds)
    return result
}
