package com.sumizuri.sync.server.account

import com.sumizuri.sync.core.ErrorCodes
import com.sumizuri.sync.core.RowLimits
import com.sumizuri.sync.core.RowOut
import com.sumizuri.sync.core.RowPage
import com.sumizuri.sync.core.RowPush
import com.sumizuri.sync.core.RowPushed
import com.sumizuri.sync.core.Rows
import com.sumizuri.sync.core.ServerSettings
import com.sumizuri.sync.server.ApiException
import com.sumizuri.sync.server.LiveSettings
import com.sumizuri.sync.server.auth.Session
import com.sumizuri.sync.server.auth.Sessions
import com.sumizuri.sync.server.badRequest
import com.sumizuri.sync.server.db.Libraries
import com.sumizuri.sync.server.db.Snapshots
import com.sumizuri.sync.server.db.SyncRows
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentLength
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.Base64

// The rows of a library: a device fetches the changes after the number it last saw, a page at a time, and sends the rows it changed in batches. The server only numbers and keeps the locked rows, and never opens one.
fun Route.rowRoutes(db: Database, sessions: Sessions, maxBytes: Long, live: LiveSettings) {
    get("/v1/libraries/{id}/rows") {
        val session = sessions.of(call)
        val id = libraryOf(db, session, call)
        val after = call.request.queryParameters["after"]
            ?.toLongOrNull()
            ?.coerceAtLeast(0) ?: 0
        val limit = call.request.queryParameters["limit"]
            ?.toIntOrNull()
            ?.coerceIn(1, RowLimits.PerBatch) ?: RowLimits.PerBatch
        val page = withContext(Dispatchers.IO) {
            transaction(db) {
                val head = Libraries.select(Libraries.rowHead).where { Libraries.id eq id }.single()[Libraries.rowHead]
                val found = SyncRows
                    .selectAll()
                    .where { (SyncRows.libraryId eq id) and (SyncRows.seq greater after) }
                    .orderBy(SyncRows.seq, SortOrder.ASC)
                    .limit(limit + 1)
                    .map {
                        RowOut(
                            it[SyncRows.rowId],
                            it[SyncRows.seq],
                            base64.encodeToString(it[SyncRows.data].bytes),
                        )
                    }
                RowPage(found.take(limit), head, found.size > limit)
            }
        }
        // A device that asks gets the page packed, which is about a third smaller.
        if (call.request.queryParameters["packed"] == "1") {
            call.response.header(Rows.PackedHeader, "zlib")
            call.respondBytes(
                Rows.pack(wire.encodeToString(RowPage.serializer(), page).encodeToByteArray()),
                ContentType.Application.OctetStream,
            )
        } else {
            call.respond(page)
        }
    }

    post("/v1/libraries/{id}/rows") {
        val session = sessions.of(call)
        val id = libraryOf(db, session, call)
        // The rows are base64 inside the JSON, so a batch is a third bigger than the rows it carries.
        if ((call.request.contentLength() ?: 0) > maxBytes * 4 / 3 + 65_536) throw tooLarge(maxBytes)
        val push = if (call.request.headers[Rows.PackedHeader] == "zlib") {
            val unpacked =
                Rows.unpack(call.receive<ByteArray>(), (maxBytes * 4 / 3 + 65_536).toInt())
                    ?: badRequest("The packed batch could not be read or is too big.")
            runCatching { wire.decodeFromString(RowPush.serializer(), unpacked.decodeToString()) }.getOrNull()
                ?: badRequest("The batch is not valid.")
        } else {
            call.receive<RowPush>()
        }
        if (push.rows.isEmpty()) badRequest("The batch has no rows.")
        if (push.rows.size > RowLimits.PerBatch) badRequest("A batch has at most ${RowLimits.PerBatch} rows.")
        if (push.rows
                .map { it.id }
                .toSet()
                .size != push.rows.size
        ) {
            badRequest("A row is in the batch twice.")
        }
        val rows = push.rows.map { row ->
            if (!Rows.validId(row.id)) badRequest("A row id is not valid.")
            val bytes =
                runCatching { Base64.getDecoder().decode(row.data) }.getOrNull() ?: badRequest("A row is not base64.")
            if (bytes.isEmpty() ||
                bytes.size > RowLimits.RowMaxBytes
            ) {
                badRequest("A row is empty or bigger than ${RowLimits.RowMaxBytes / 1_048_576} MB.")
            }
            row.id to bytes
        }
        val limitMb = live.number(ServerSettings.UserStorageMb)
        val pushed = withContext(Dispatchers.IO) {
            transaction(db) {
                // The library row is held while the batch goes in, so two batches never get the same numbers.
                val head = Libraries
                    .select(Libraries.rowHead)
                    .where { Libraries.id eq id }
                    .forUpdate()
                    .single()[Libraries.rowHead]
                val ids = rows.map { it.first }
                val current = SyncRows
                    .select(SyncRows.rowId, SyncRows.seq, SyncRows.size)
                    .where { (SyncRows.libraryId eq id) and (SyncRows.rowId inList ids) }
                    .associate { it[SyncRows.rowId] to (it[SyncRows.seq] to it[SyncRows.size]) }
                // Another device changed one of these rows since this device fetched, so it fetches and merges again first.
                if (current.values.any { (seq, _) -> seq > push.base }) {
                    throw ApiException(
                        HttpStatusCode.Conflict,
                        ErrorCodes.Changed,
                        "Another device changed some of these rows since this device fetched them.",
                    )
                }
                if (limitMb > 0) {
                    val grows = rows.sumOf { (rowId, bytes) -> bytes.size.toLong() - (current[rowId]?.second ?: 0) }
                    if (grows > 0 &&
                        storedBy(session) + grows > limitMb * 1_048_576L
                    ) {
                        throw ApiException(
                            HttpStatusCode.PayloadTooLarge,
                            ErrorCodes.TooLarge,
                            "This would go over your storage limit on this server ($limitMb MB).",
                        )
                    }
                }
                var seq = head
                SyncRows.batchUpsert(rows) { (rowId, bytes) ->
                    seq++
                    this[SyncRows.libraryId] = id
                    this[SyncRows.rowId] = rowId
                    this[SyncRows.seq] = seq
                    this[SyncRows.data] = ExposedBlob(bytes)
                    this[SyncRows.size] = bytes.size
                }
                Libraries.update({ Libraries.id eq id }) {
                    it[rowHead] = seq
                    it[rowsAt] = System.currentTimeMillis()
                }
                RowPushed(seq, contiguous = head == push.base)
            }
        }
        call.respond(pushed)
    }
}

private val base64 = Base64.getEncoder()
private val wire = Json { ignoreUnknownKeys = true }

// What a person keeps on the server, copies and rows of all their profiles together.
internal fun storedBy(session: Session): Long {
    val copies = Libraries
        .join(Snapshots, JoinType.INNER, Libraries.id, Snapshots.libraryId)
        .select(Snapshots.size)
        .where { Libraries.userId eq session.userId }
        .sumOf { it[Snapshots.size] }
    return copies + rowsOfUser(session.userId)
}

// The bytes of the rows of one person, or of one library when it is given.
internal fun rowsOfUser(userId: Long, libraryId: Long? = null): Long {
    val total = SyncRows.size.sum()
    return Libraries
        .join(SyncRows, JoinType.INNER, Libraries.id, SyncRows.libraryId)
        .select(total)
        .where {
            if (libraryId ==
                null
            ) {
                Libraries.userId eq userId
            } else {
                (Libraries.userId eq userId) and (Libraries.id eq libraryId)
            }
        }.singleOrNull()
        ?.get(total)
        ?.toLong() ?: 0L
}

private suspend fun libraryOf(db: Database, session: Session, call: RoutingCall): Long {
    val id = call.parameters["id"]?.toLongOrNull() ?: badRequest("Give the number of a library.")
    val own = withContext(Dispatchers.IO) {
        transaction(db) {
            Libraries
                .selectAll()
                .where { (Libraries.id eq id) and (Libraries.userId eq session.userId) }
                .empty()
                .not()
        }
    }
    if (!own) {
        throw ApiException(
            HttpStatusCode.NotFound,
            ErrorCodes.NotFound,
            "There is no such library on this account.",
        )
    }
    return id
}

private fun tooLarge(maxBytes: Long) = ApiException(
    HttpStatusCode.PayloadTooLarge,
    ErrorCodes.TooLarge,
    "The batch is bigger than this server takes (${maxBytes / 1_048_576} MB).",
)
