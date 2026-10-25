package com.sumizuri.app.core.perf

import com.sumizuri.app.core.log.AppLog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.concurrent.Volatile
import kotlin.math.roundToInt

private const val Tag = "perf"

// How often the thread that draws the app is asked to answer, and how long it may take before it counts as stuck.
private const val UiCheckEveryMs = 1_000L
private const val UiStuckMs = 250L

// How often the memory and the processor are read.
private const val SystemEveryMs = 5_000L

// The heap counts as nearly full above this share, and as jumping when it grows this much within two readings.
private const val MemoryHighShare = 0.85f
private const val MemoryJumpMb = 150

// The whole app counts as holding a lot of memory above this, and as jumping when it grows this much within two readings.
private const val RamHighMb = 1_500
private const val RamJumpMb = 250

// The processor counts as busy above this share of all the cores for this many readings in a row.
private const val CpuBusyPercent = 25f
private const val CpuBusyReadings = 3

// A frame this slow is noted while the frame meter runs.
private const val SlowFrameNoteMs = 100f

// Frames count as dropping when they stay under this share of the most the meter has seen, which is about what the screen can show, for this many of its reports in a row, about a second.
private const val FpsDropShare = 0.6f
private const val FpsDropReports = 2

// While frames drop, the thread that draws is looked at this many times, this far apart, to see where its time goes.
private const val DropLooks = 10
private const val DropLookEveryMs = 20L

// The same kind of warning is written at most this often, so a long problem does not fill the log.
private const val QuietMs = 30_000L

// Watches the app for frames that freeze or drop, a processor that stays busy and memory that fills or jumps, and writes each to the log with what the app was doing then: the screen, the work that ran, the sources it asked, where the system tells the code that held the processor, and the frames, the memory and the processor at that moment.
object PerfWatch {
    // The screen that is open, set by the navigation.
    var screen: String = "start"

    // The work the app is doing right now, described in a few words each, such as a check of the library and how full the image cache is. Set by the app.
    var activity: () -> List<String> = { emptyList() }

    private val lastWarned = MutableStateFlow<Map<String, Long>>(emptyMap())

    // The latest readings, kept so each warning can say how the app stood without asking the system on the thread that draws. The frames are only known while the meter runs.
    @Volatile private var lastMemory: MemoryUse? = null

    @Volatile private var lastCpu: Float? = null

    @Volatile private var lastFps: Float? = null

    // The most frames a second the meter has reported, and how many reports in a row came in well under it. Both are only touched by the meter, on the thread that draws.
    private var usualFps = 0f
    private var lowReports = 0

    // Where the watch runs, so a drop of the frames can be looked into away from the thread that draws.
    @Volatile private var scope: CoroutineScope? = null

    suspend fun run() = coroutineScope {
        scope = this
        launch(Dispatchers.Default) { watchUiThread() }
        launch(Dispatchers.Default) { watchSystem() }
    }

    // Noted by the frame meter when a frame was very slow.
    fun slowFrame(ms: Float) {
        if (ms >= SlowFrameNoteMs) warn("frame", "a frame took ${ms.roundToInt()} ms on $screen${context()}")
    }

    // Noted by the frame meter at each report. When the frames stay well under the most it has seen, the thread that draws is looked at for a moment and the drop is written down with what was found, at most once in a while for each screen.
    fun frames(stats: FrameStats) {
        lastFps = stats.fps
        if (stats.fps > usualFps) usualFps = stats.fps
        val low = usualFps >= 50f && stats.fps < usualFps * FpsDropShare
        lowReports = if (low) lowReports + 1 else 0
        if (lowReports != FpsDropReports) return
        val on = screen
        val usual = usualFps
        val work = context()
        val kind = "fps $on"
        if (!claim(kind)) return
        scope?.launch(Dispatchers.Default) {
            val found = lookAtUiThread()
            write(
                "frames fell to ${stats.fps.roundToInt()} a second from the usual ${usual.roundToInt()} on $on$work, ${stats.averageMs.oneDecimal()} ms each on average and ${stats.worstMs.roundToInt()} ms at worst; $found",
            )
        }
    }

    // Noted by the frame meter when it stops, so the warnings no longer give a number of frames that is out of date.
    fun framesStopped() {
        lastFps = null
        lowReports = 0
    }

    private fun warn(kind: String, message: String) {
        if (claim(kind)) write(message)
    }

    // Whether a warning of this kind may be written now, which counts as writing it.
    private fun claim(kind: String): Boolean {
        val now = processUptimeMs()
        var allowed = false
        lastWarned.update { all ->
            val last = all[kind]
            if (last != null && now - last < QuietMs) {
                all
            } else {
                allowed = true
                all + (kind to now)
            }
        }
        return allowed
    }

    private fun write(message: String) = AppLog.warn(Tag, message + state())

    private fun context(): String {
        val work = runCatching { activity() }.getOrDefault(emptyList())
        return if (work.isEmpty()) "" else " (" + work.joinToString(", ") + ")"
    }

    // How the app stood at the latest readings: the frames while the meter runs, the memory of the whole app and of its heap, and the processor.
    private fun state(): String {
        val parts = buildList {
            lastFps?.let { add("${it.roundToInt()} fps") }
            lastMemory?.let { memory ->
                memory.processMb?.let { add("$it MB in memory") }
                add("heap ${memory.usedMb} of ${memory.maxMb} MB")
            }
            lastCpu?.let { add("processor ${it.oneDecimal()}%") }
        }
        return if (parts.isEmpty()) "" else " [" + parts.joinToString(", ") + "]"
    }

    // Looks at the thread that draws several times in a row: how much of that time it spent working, which says whether the app's own work or the drawing behind it is slow, and the code it was found in most, the topmost line and the app's own.
    private suspend fun lookAtUiThread(): String {
        val cpuBefore = uiThreadCpuMs()
        val started = processUptimeMs()
        val tops = HashMap<String, Int>()
        val ours = HashMap<String, Int>()
        repeat(DropLooks) {
            val stack = runCatching { uiThreadStack() }.getOrNull().orEmpty()
            stack.firstOrNull()?.let { tops[it] = (tops[it] ?: 0) + 1 }
            // The watch's  own lines are left out, as it is what asks.
            stack.firstOrNull { it.startsWith("com.sumizuri") && !it.startsWith("com.sumizuri.app.core.perf") }?.let {
                ours[it] =
                    (ours[it] ?: 0) + 1
            }
            delay(DropLookEveryMs)
        }
        val cpuAfter = uiThreadCpuMs()
        val passed = (processUptimeMs() - started).coerceAtLeast(1)
        fun most(found: Map<String, Int>) = found.entries
            .sortedByDescending { it.value }
            .take(2)
            .joinToString(", ") { "${it.key} (${it.value} of $DropLooks)" }
        val busy = if (cpuBefore != null &&
            cpuAfter != null
        ) {
            "the UI thread worked ${((cpuAfter - cpuBefore) * 100 / passed).coerceIn(0, 100)}% of the time"
        } else {
            "the time of the UI thread is not known"
        }
        val top = if (tops.isEmpty()) "" else ", found mostly at ${most(tops)}"
        val app = if (ours.isEmpty()) "" else ", in the app at ${most(ours)}"
        return busy + top + app
    }

    // Asks the thread that draws the app to answer now and then. When it does not answer in time it is stuck, so where it is gets read at once, which names the code that holds it, and the warning says how long it stayed stuck.
    private suspend fun CoroutineScope.watchUiThread() {
        while (true) {
            delay(UiCheckEveryMs)
            val started = processUptimeMs()
            val answered = CompletableDeferred<Unit>()
            launch(Dispatchers.Main) { answered.complete(Unit) }
            if (withTimeoutOrNull(UiStuckMs) { answered.await() } != null) continue
            val stack = runCatching { uiThreadStack() }.getOrNull()
            answered.await()
            val stuck = processUptimeMs() - started
            val where = stack?.joinToString(" <- ") ?: "unknown"
            warn("stuck", "the screen froze for $stuck ms on $screen${context()}, the UI thread was at: $where")
        }
    }

    // Reads the memory and the processor every few seconds and warns when either stays high or the memory jumps.
    private suspend fun watchSystem() {
        var heapBefore: Int? = null
        var ramBefore: Int? = null
        var busy = 0
        var callsBefore = Perf.calls.value
        // The first reading of the threads only sets where they start.
        runCatching { busiestThreads(3) }
        cpuUse()
        while (true) {
            delay(SystemEveryMs)
            val memory = memoryUse().copy(processMb = runCatching { processMemoryMb() }.getOrNull())
            val cpu = cpuUse()
            lastMemory = memory
            lastCpu = cpu
            val share = if (memory.maxMb > 0) memory.usedMb.toFloat() / memory.maxMb else 0f
            if (share > MemoryHighShare) {
                warn("memory", "the heap is nearly full on $screen${context()}")
            }
            heapBefore?.let { before ->
                if (memory.usedMb - before >
                    MemoryJumpMb
                ) {
                    warn(
                        "memory-jump",
                        "the heap grew by ${memory.usedMb - before} MB in ${SystemEveryMs / 1000} s on $screen${context()}",
                    )
                }
            }
            heapBefore = memory.usedMb
            memory.processMb?.let { ram ->
                if (ram >
                    RamHighMb
                ) {
                    warn("ram", "the app holds more than $RamHighMb MB of memory on $screen${context()}")
                }
                ramBefore?.let { before ->
                    if (ram - before >
                        RamJumpMb
                    ) {
                        warn(
                            "ram-jump",
                            "the memory of the app grew by ${ram - before} MB in ${SystemEveryMs / 1000} s on $screen${context()}",
                        )
                    }
                }
                ramBefore = ram
            }
            val threads = runCatching { busiestThreads(3) }.getOrNull()
            val calls = Perf.calls.value
            if (cpu != null && cpu > CpuBusyPercent) busy++ else busy = 0
            if (busy >= CpuBusyReadings) {
                val asked = calls
                    .mapNotNull { (name, now) ->
                        val count = now.calls - (callsBefore[name]?.calls ?: 0)
                        if (count > 0) name to count else null
                    }.sortedByDescending { it.second }
                val sources = if (asked.isEmpty()) {
                    ""
                } else {
                    ", sources asked: " +
                        asked.take(4).joinToString(", ") { "${it.first} ${it.second}" }
                }
                val hot = if (threads.isNullOrEmpty()) "" else ", busiest threads: " + threads.joinToString("; ")
                warn(
                    "cpu",
                    "the processor stayed busy for ${CpuBusyReadings * SystemEveryMs / 1000} s on $screen${context()}$sources$hot",
                )
                busy = 0
            }
            if (busy == 0) callsBefore = calls
        }
    }
}
