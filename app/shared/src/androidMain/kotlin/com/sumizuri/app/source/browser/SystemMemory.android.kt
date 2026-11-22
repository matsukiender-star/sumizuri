package com.sumizuri.app.source.browser

import java.io.File

internal actual fun totalSystemRamBytes(): Long? = runCatching {
    val file = File("/proc/meminfo")
    if (!file.canRead()) return@runCatching null
    val line = file.useLines { lines -> lines.firstOrNull { it.startsWith("MemTotal:") } } ?: return@runCatching null
    val kb = line.removePrefix("MemTotal:").trim().substringBefore(' ').toLongOrNull() ?: return@runCatching null
    (kb * 1024L).takeIf { it > 0L }
}.getOrNull()
