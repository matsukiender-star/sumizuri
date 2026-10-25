package com.sumizuri.app.data.storage

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.core.log.LogEntry
import com.sumizuri.app.core.log.LogLevel
import com.sumizuri.app.core.log.LogSink
import com.sumizuri.app.data.db.AppDatabase
import com.sumizuri.app.data.db.log.LogRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val Keep = 2000
private const val BatchWaitMs = 500L
private const val PruneEvery = 20

// Saves the log in the database, a batch at a time, and reads the earlier lines back when it starts.
class DatabaseLogSink(private val db: AppDatabase, private val scope: CoroutineScope) : LogSink {
    private val queue = Channel<LogEntry>(Channel.UNLIMITED)

    init {
        scope.launch {
            db.logs().prune(Keep)
            AppLog.restore(
                db
                    .logs()
                    .latest(Keep)
                    .reversed()
                    .map { it.toEntry() },
            )
            drain()
        }
    }

    // Only lines worth reading after a restart are kept. The debug lines, which a check of the library writes for every request, stay in the log of this run and its file.
    override fun write(entry: LogEntry, line: String) {
        if (entry.level >= LogLevel.Info) queue.trySend(entry)
    }

    override fun clear() {
        scope.launch { db.logs().clear() }
    }

    // Waits for a line, gives the next few a moment to arrive, then saves them together.
    private suspend fun drain() {
        var batches = 0
        while (true) {
            val batch = mutableListOf(queue.receive())
            delay(BatchWaitMs)
            while (true) batch += queue.tryReceive().getOrNull() ?: break
            db.logs().insertAll(
                batch.map { LogRow(time = it.time, level = it.level.ordinal, tag = it.tag, message = it.message) },
            )
            if (++batches % PruneEvery == 0) db.logs().prune(Keep)
        }
    }

    private fun LogRow.toEntry() = LogEntry(time, LogLevel.entries.getOrElse(level) { LogLevel.Info }, tag, message)
}
