package com.sumizuri.app.core.perf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// The memory of the app in megabytes: what the managed heap uses, the most it may grow to, what native code holds where the system says, and what the whole app holds in the memory of the device, the number a task manager shows, where the system tells.
data class MemoryUse(val usedMb: Int, val maxMb: Int, val nativeMb: Int?, val processMb: Int? = null)

expect fun memoryUse(): MemoryUse

// What the whole app holds in the memory of the device right now, in megabytes, or null where the system does not tell. It can take a moment to read, so it is read away from the thread that draws.
expect fun processMemoryMb(): Int?

// The share of all processors the app used lately, from 0 to 100, or null when it is not known yet.
expect fun cpuUse(): Float?

expect fun threadCount(): Int

// The memory, the processor and the threads, read together.
data class SystemUse(val memory: MemoryUse, val cpu: Float?, val threads: Int)

// Reads them away from the thread that draws the app, as the system can take a good part of a second to give the processor load on Windows.
suspend fun systemUse(): SystemUse = withContext(Dispatchers.Default) {
    SystemUse(memoryUse().copy(processMb = processMemoryMb()), cpuUse(), threadCount())
}

// How long the process has run, in milliseconds since it started.
expect fun processUptimeMs(): Long

// The processor time the thread that draws the app has used since it began, in milliseconds, or null where the system does not tell. Read twice, it says how busy the thread was in between.
expect fun uiThreadCpuMs(): Long?

// Where the thread that draws the app is right now, as lines of its stack, or null where it cannot be read. Taken while the thread is stuck, it names the code that holds it.
expect fun uiThreadStack(): List<String>?

// The threads that used the most processor time since the last call, each as its name, its share and where it is, or null where the system does not tell.
expect fun busiestThreads(count: Int): List<String>?

// The lines of a stack worth showing: the first few, and then up to the third line of the app's own code, which is usually the cause.
internal fun briefStack(frames: List<String>): List<String> {
    val ours = frames.indices.filter { frames[it].startsWith("com.sumizuri") }
    val end = (ours.getOrNull(2) ?: (frames.size - 1)).coerceAtMost(frames.size - 1)
    return frames.take(maxOf(4, end + 1)).take(12)
}
