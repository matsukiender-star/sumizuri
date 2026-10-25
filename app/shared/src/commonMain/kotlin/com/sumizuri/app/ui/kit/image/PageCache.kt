package com.sumizuri.app.ui.kit.image

import androidx.compose.ui.graphics.ImageBitmap
import com.sumizuri.app.core.Lock

// The most pages that are kept.
private const val MaxPages = 12

// The pages of the reader that were shown a moment ago, so turning back or opening the chapter again does not read and cut them a second time. The page shown longest ago goes first once there are too many or they take more memory than the setting allows, and everything in it goes after a few minutes unseen.
object PageCache {
    private class Entry(val slices: List<ImageBitmap>, val bytes: Long, var lastShown: Long)

    private val pages = HashMap<String, Entry>()
    private var total = 0L

    // The performance overlay and the reader read the cache from different threads.
    private val lock = Lock()

    // A page that was not shown again within this many milliseconds is let go, and the kept pages may take this many bytes. Both are set from the settings.
    private var keepMillis = 5 * 60 * 1000L
    private var maxBytes = 64L * 1024 * 1024

    // How many pages are kept and how many bytes they take, for the performance overlay.
    fun usage(): Pair<Int, Long> = lock.withLock { pages.size to total }

    fun configure(maxBytes: Long, keepMillis: Long) {
        lock.withLock {
            this.maxBytes = maxBytes
            this.keepMillis = keepMillis
            trim(null)
        }
    }

    fun get(key: String, now: Long): List<ImageBitmap>? = lock.withLock {
        val entry = pages[key] ?: return@withLock null
        if (now - entry.lastShown > keepMillis) {
            remove(key)
            return@withLock null
        }
        entry.lastShown = now
        entry.slices
    }

    fun put(key: String, slices: List<ImageBitmap>, now: Long) {
        lock.withLock {
            remove(key)
            val entry = Entry(slices, slices.sumOf { it.width.toLong() * it.height * 4 }, now)
            pages[key] = entry
            total += entry.bytes
            trim(key)
        }
    }

    // How much memory the kept pages take.
    fun bytes(): Long = lock.withLock { total }

    // Lets go of every kept page at once.
    fun clear() {
        lock.withLock {
            pages.clear()
            total = 0
        }
    }

    // Lets go of every page that has gone unseen for a while, and of the oldest ones while the rest take too much.
    fun sweep(now: Long) {
        lock.withLock {
            pages.entries.filter { now - it.value.lastShown > keepMillis }.forEach { remove(it.key) }
            trim(null)
        }
    }

    // The page shown longest ago goes until the count and the memory fit, but never the page just put in, so a page larger than the whole limit is still kept until the next one.
    private fun trim(keep: String?) {
        while (pages.size > MaxPages || total > maxBytes) {
            val oldest = pages.entries.filter { it.key != keep }.minByOrNull { it.value.lastShown } ?: break
            remove(oldest.key)
        }
    }

    private fun remove(key: String) {
        pages.remove(key)?.let { total -= it.bytes }
    }
}
