package com.sumizuri.app.source.browser

import platform.Foundation.NSProcessInfo

internal actual fun totalSystemRamBytes(): Long? = NSProcessInfo.processInfo.physicalMemory
    .toLong()
    .takeIf { it > 0L }
