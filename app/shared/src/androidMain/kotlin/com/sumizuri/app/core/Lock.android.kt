package com.sumizuri.app.core

internal actual class Lock actual constructor() {
    private val monitor = Any()

    actual fun <T> withLock(block: () -> T): T = synchronized(monitor, block)
}
