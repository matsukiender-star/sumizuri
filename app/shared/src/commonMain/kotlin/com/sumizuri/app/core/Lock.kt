package com.sumizuri.app.core

// A lock that one thread at a time can hold, and the same thread can take again. Each system has its own: the monitor of Java, and the recursive lock of Apple.
internal expect class Lock() {
    fun <T> withLock(block: () -> T): T
}
