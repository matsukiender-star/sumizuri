package com.sumizuri.app.data.net

import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong

// The most one channel may hold that nothing has read yet. A player that is paused with a full buffer stops reading, and is cut here and asks again later from where it was.
private const val MaxHeldBytes = 16L * 1024 * 1024

// What the server sends on one SSH channel, kept in a queue that the router reads on a thread of its own. JSch reads every channel of a session on one thread, so it must never wait for a reader that is slow or stopped.
internal class Inbox(private val onCut: () -> Unit) : InputStream() {
    private val queue = LinkedBlockingQueue<ByteArray>()
    private val held = AtomicLong(0)
    private var piece: ByteArray? = null
    private var at = 0

    @Volatile private var cut = false

    // The side JSch writes to.
    val sink: OutputStream = object : OutputStream() {
        override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

        override fun write(b: ByteArray, off: Int, len: Int) {
            if (cut || len <= 0) return
            if (held.addAndGet(len.toLong()) > MaxHeldBytes) {
                cut = true
                queue.put(End)
                onCut()
                return
            }
            queue.put(b.copyOfRange(off, off + len))
        }

        override fun close() = queue.put(End)
    }

    override fun read(): Int {
        val one = ByteArray(1)
        return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xFF
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        var now = piece
        if (now == null || at >= now.size) {
            now = queue.take()
            if (now === End) {
                queue.put(End)
                return -1
            }
            piece = now
            at = 0
        }
        val count = minOf(len, now.size - at)
        now.copyInto(b, off, at, at + count)
        at += count
        held.addAndGet(-count.toLong())
        return count
    }

    override fun close() {
        cut = true
        queue.put(End)
    }

    private companion object {
        val End = ByteArray(0)
    }
}
