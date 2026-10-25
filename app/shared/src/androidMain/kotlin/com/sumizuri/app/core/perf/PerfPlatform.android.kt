package com.sumizuri.app.core.perf

import android.os.Debug
import android.os.Looper
import android.os.Process
import android.os.SystemClock

private const val Mb = 1024L * 1024L

actual fun memoryUse(): MemoryUse {
    val runtime = Runtime.getRuntime()
    return MemoryUse(
        ((runtime.totalMemory() - runtime.freeMemory()) / Mb).toInt(),
        (runtime.maxMemory() / Mb).toInt(),
        (Debug.getNativeHeapAllocatedSize() / Mb).toInt(),
    )
}

// The share of the memory of the device the app holds, counting what it shares with others in part. Android reads it from a long file, so it is asked off the main thread only.
actual fun processMemoryMb(): Int? = (Debug.getPss() / 1024).toInt()

// The processor time of the process against the time that passed since the last call, shared over all the cores.
private var lastCpuMs = -1L
private var lastWallMs = 0L

@Synchronized
actual fun cpuUse(): Float? {
    val cpu = Process.getElapsedCpuTime()
    val wall = SystemClock.elapsedRealtime()
    val before = lastCpuMs
    val beforeWall = lastWallMs
    lastCpuMs = cpu
    lastWallMs = wall
    if (before < 0 || wall <= beforeWall) return null
    val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
    return ((cpu - before).toFloat() / (wall - beforeWall) / cores * 100f).coerceIn(0f, 100f)
}

actual fun threadCount(): Int = Thread.activeCount()

actual fun processUptimeMs(): Long = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime()

// Android only tells the processor time of the thread that asks, so the time of the main thread cannot be read from another.
actual fun uiThreadCpuMs(): Long? = null

actual fun uiThreadStack(): List<String>? = briefStack(
    Looper
        .getMainLooper()
        .thread.stackTrace
        .map { it.toString() },
)

// Android does not hand out the processor time of each thread in a cheap way, so the warning names the work and the screen instead.
actual fun busiestThreads(count: Int): List<String>? = null
