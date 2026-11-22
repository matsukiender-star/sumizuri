package com.sumizuri.app.source.browser

import com.sun.management.OperatingSystemMXBean
import java.io.File
import java.lang.management.ManagementFactory

internal actual fun totalSystemRamBytes(): Long? {
    val fromMxBean = runCatching {
        val bean = ManagementFactory.getOperatingSystemMXBean()
        (bean as? OperatingSystemMXBean)?.totalMemorySize?.takeIf { it > 0L }
    }.getOrNull()
    if (fromMxBean != null) return fromMxBean
    return readProcMemInfoBytes()
}

private fun readProcMemInfoBytes(): Long? = runCatching {
    val file = File("/proc/meminfo")
    if (!file.canRead()) return@runCatching null
    val line = file.useLines { lines -> lines.firstOrNull { it.startsWith("MemTotal:") } } ?: return@runCatching null
    val kb = line
        .removePrefix("MemTotal:")
        .trim()
        .substringBefore(' ')
        .toLongOrNull() ?: return@runCatching null
    (kb * 1024L).takeIf { it > 0L }
}.getOrNull()
