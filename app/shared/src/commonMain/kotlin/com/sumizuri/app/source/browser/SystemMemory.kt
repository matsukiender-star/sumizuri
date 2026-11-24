package com.sumizuri.app.source.browser

// Devices at or below 5 GiB total RAM keep only one hidden browser page and session alive.
internal const val LowRamThresholdBytes: Long = 5L * 1024L * 1024L * 1024L

internal expect fun totalSystemRamBytes(): Long?

internal fun isLowRamDevice(totalBytes: Long? = totalSystemRamBytes()): Boolean =
    totalBytes != null && totalBytes > 0L && totalBytes <= LowRamThresholdBytes
