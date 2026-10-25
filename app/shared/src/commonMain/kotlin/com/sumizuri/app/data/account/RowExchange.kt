package com.sumizuri.app.data.account

import com.sumizuri.sync.core.RowIn
import com.sumizuri.sync.core.RowLimits
import com.sumizuri.sync.core.RowPush
import com.sumizuri.sync.core.Rows
import com.sumizuri.sync.core.SyncCrypto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

// How far a sync is: fetching or sending, and how many rows of how many.
data class SyncProgress(val sending: Boolean, val done: Int, val total: Int)

// What this device and the server last agreed on, the number of the last change it fetched, and the repos and source values the server had then.
@Serializable
internal data class RowBase(val state: SyncState, val head: Long, val extras: JsonObject? = null)

// The rows fetched in one sync by key, the newest of each, with the number of the last change fetched and the newest number of the library.
internal class Pulled(val rows: Map<String, RowBody>, val head: Long, val serverHead: Long)

// Fetches and sends the rows of one library, a page or a batch at a time, locking and unlocking each row on the device.
internal class RowExchange(
    private val client: SyncClient,
    private val account: SyncAccount,
    private val libraryId: Long,
    private val libraryKey: ByteArray,
    private val progress: (SyncProgress?) -> Unit,
) {
    // Every change after the number given, oldest first, so a row changed twice ends as it is now.
    suspend fun pull(after: Long): Pulled {
        val rows = LinkedHashMap<String, RowBody>()
        var head = after
        var serverHead = after
        while (true) {
            val page = client.pullRows(account, libraryId, head)
            serverHead = page.head
            for (row in page.rows) {
                val body = Rows.unlock(libraryKey, row.id, SyncCrypto.fromBase64(row.data))?.let(SyncRowCodec::decode)
                    ?: throw SyncProblem(LibrarySyncer.KeyMismatch, "A row on the server could not be opened.")
                rows.remove(body.key)
                rows[body.key] = body
                head = row.seq
            }
            progress(
                SyncProgress(
                    sending = false,
                    done = (head - after).toInt(),
                    total = (serverHead - after).toInt().coerceAtLeast(1),
                ),
            )
            if (!page.more || page.rows.isEmpty()) break
        }
        return Pulled(rows, head, serverHead)
    }

    // Sends the rows in batches built on the change numbered base, and gives the number this device may go on from and the bytes sent. A batch that came right after the last one moves the number on, so the device does not fetch its own rows back.
    suspend fun push(rows: List<RowBody>, base: Long): Pair<Long, Long> {
        var at = base
        val before = client.rowBytesSent
        var done = 0
        progress(SyncProgress(sending = true, done = 0, total = rows.size))
        for (batch in rows.chunked(RowLimits.PerBatch)) {
            val locked = batch.map { body ->
                val id = Rows.idOf(libraryKey, body.key)
                RowIn(id, SyncCrypto.base64(Rows.lock(libraryKey, id, SyncRowCodec.encode(body))))
            }
            val pushed = client.pushRows(account, libraryId, RowPush(at, locked))
            if (pushed.contiguous) at = pushed.head
            done += batch.size
            progress(SyncProgress(sending = true, done = done, total = rows.size))
        }
        return at to client.rowBytesSent - before
    }
}

private val baseJson = Json { ignoreUnknownKeys = true }

internal fun readRowBase(path: Path): RowBase? = runCatching {
    if (!FileSystem.SYSTEM.exists(
            path,
        )
    ) {
        null
    } else {
        baseJson.decodeFromString(
            RowBase.serializer(),
            FileSystem.SYSTEM.read(path) {
                readUtf8()
            },
        )
    }
}.getOrNull()

// Written to a side file first, so a sync cut short never leaves half a file.
internal fun writeRowBase(path: Path, base: RowBase) {
    val temporary = "$path.tmp".toPath()
    FileSystem.SYSTEM.write(temporary) { writeUtf8(baseJson.encodeToString(RowBase.serializer(), base)) }
    FileSystem.SYSTEM.atomicMove(temporary, path)
}
