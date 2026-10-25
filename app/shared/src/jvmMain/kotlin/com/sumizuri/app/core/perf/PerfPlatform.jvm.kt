package com.sumizuri.app.core.perf

import java.awt.EventQueue
import java.lang.management.ManagementFactory

private const val Mb = 1024L * 1024L

actual fun memoryUse(): MemoryUse {
    val runtime = Runtime.getRuntime()
    return MemoryUse(
        ((runtime.totalMemory() - runtime.freeMemory()) / Mb).toInt(),
        (runtime.maxMemory() / Mb).toInt(),
        null,
    )
}

// The Java runtime keeps the load of the process itself, measured since the last time it was asked.
actual fun cpuUse(): Float? {
    val system =
        ManagementFactory.getOperatingSystemMXBean() as? com.sun.management.OperatingSystemMXBean ?: return null
    val load = system.processCpuLoad
    return if (load < 0) null else (load * 100).toFloat()
}

actual fun threadCount(): Int = ManagementFactory.getThreadMXBean().threadCount

actual fun processUptimeMs(): Long = ManagementFactory.getRuntimeMXBean().uptime

// The thread of the window, which draws the app on the desktop. It is found once and kept, and found again if Java has ended it and started another.
@Volatile
private var uiThread: Thread? = null

private fun findUiThread(): Thread? {
    uiThread?.takeIf { it.isAlive }?.let { return it }
    if (EventQueue.isDispatchThread()) return Thread.currentThread().also { uiThread = it }
    return Thread
        .getAllStackTraces()
        .keys
        .firstOrNull { it.name.startsWith("AWT-EventQueue") }
        ?.also { uiThread = it }
}

actual fun uiThreadCpuMs(): Long? {
    val thread = findUiThread() ?: return null
    val threads = ManagementFactory.getThreadMXBean()
    if (!threads.isThreadCpuTimeSupported) return null
    if (!threads.isThreadCpuTimeEnabled) threads.isThreadCpuTimeEnabled = true
    val nanos = threads.getThreadCpuTime(thread.threadId())
    return if (nanos < 0) null else nanos / 1_000_000
}

actual fun uiThreadStack(): List<String>? {
    val thread = findUiThread() ?: return null
    return briefStack(thread.stackTrace.map { it.toString() })
}

// The processor time of each thread at the last call, by its id.
private val lastCpu = HashMap<Long, Long>()
private var lastWall = 0L

@Synchronized
actual fun busiestThreads(count: Int): List<String>? {
    val threads = ManagementFactory.getThreadMXBean()
    if (!threads.isThreadCpuTimeSupported) return null
    if (!threads.isThreadCpuTimeEnabled) threads.isThreadCpuTimeEnabled = true
    val wall = System.nanoTime()
    val passed = (wall - lastWall).coerceAtLeast(1)
    val first = lastWall == 0L
    lastWall = wall
    val used = ArrayList<Pair<Long, Long>>()
    val now = HashMap<Long, Long>()
    for (id in threads.allThreadIds) {
        val cpu = threads.getThreadCpuTime(id)
        if (cpu < 0) continue
        now[id] = cpu
        val before = lastCpu[id] ?: continue
        used += id to (cpu - before)
    }
    lastCpu.clear()
    lastCpu.putAll(now)
    if (first) return emptyList()
    return used.sortedByDescending { it.second }.take(count).filter { it.second > 0 }.mapNotNull { (id, cpu) ->
        val info = threads.getThreadInfo(id, 24) ?: return@mapNotNull null
        val share = cpu * 100.0 / passed
        val where =
            briefStack(info.stackTrace.map { it.toString() }).firstOrNull { it.startsWith("com.sumizuri") }
                ?: info.stackTrace
                    .firstOrNull()
                    ?.toString()
                    .orEmpty()
        "${info.threadName} ${share.toInt()}% at $where"
    }
}
