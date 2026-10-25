package com.sumizuri.app.core.perf

import platform.Foundation.*

// When this was first asked, which is early in the start of the app, since the overlay of the numbers reads it at once.
private val startedAt = NSDate().timeIntervalSince1970

// The readings of the memory and the processor need calls of the kernel that are not written yet. The memory of the phone is told as the most the app may grow to, and the rest is left unknown.
actual fun memoryUse(): MemoryUse = MemoryUse(
    usedMb = 0,
    maxMb = (
        NSProcessInfo.processInfo.physicalMemory /
            1_048_576UL
        ).toInt(),
    nativeMb = null,
)

actual fun processMemoryMb(): Int? = null

actual fun cpuUse(): Float? = null

actual fun threadCount(): Int = 0

actual fun processUptimeMs(): Long = ((NSDate().timeIntervalSince1970 - startedAt) * 1000).toLong()

actual fun uiThreadCpuMs(): Long? = null

actual fun uiThreadStack(): List<String>? = null

actual fun busiestThreads(count: Int): List<String>? = null
