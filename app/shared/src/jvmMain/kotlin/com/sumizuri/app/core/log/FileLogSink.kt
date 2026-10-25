package com.sumizuri.app.core.log

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private const val MaxBytes = 1_000_000L

// Writes each line to the console and to a file in the user's folder, so a run can be read afterwards. The lines are handed to a thread of their own that keeps the file open and writes them in batches, so the thread that logs, the window's one included, never waits for the disk.
class FileLogSink(val file: File) : LogSink {
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val queue = LinkedBlockingQueue<String>()
    private val out: BufferedWriter

    init {
        file.parentFile?.mkdirs()
        // A file that grew large is kept once as the old one, so the log never fills the disk.
        if (file.exists() && file.length() > MaxBytes) file.renameTo(File(file.path + ".old"))
        out = BufferedWriter(FileWriter(file, true))
        thread(isDaemon = true, name = "log-file") { drain() }
        // What is still waiting is written when the app ends.
        Runtime.getRuntime().addShutdownHook(Thread { flush() })
        write(LogEntry(0, LogLevel.Info, "log", "session start"), "session start, ${LocalDateTime.now().format(stamp)}")
    }

    override fun write(entry: LogEntry, line: String) {
        println("Sumizuri $line")
        queue.offer("${LocalDateTime.now().format(stamp)} $line")
    }

    // Writes lines as they come, and empties the buffer to the file whenever no more are waiting.
    private fun drain() {
        while (true) {
            val first = queue.poll(1, TimeUnit.SECONDS)
            if (first == null) {
                flush()
                continue
            }
            write(first)
            while (true) write(queue.poll() ?: break)
            flush()
        }
    }

    @Synchronized
    private fun write(text: String) {
        runCatching {
            out.write(text)
            out.newLine()
        }
    }

    // Also called by a process that ends itself without the usual end, such as the setup, so its last lines are kept.
    @Synchronized
    fun flush() {
        while (true) write(queue.poll() ?: break)
        runCatching { out.flush() }
    }
}
