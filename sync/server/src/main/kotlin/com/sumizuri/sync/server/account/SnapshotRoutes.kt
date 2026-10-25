package com.sumizuri.sync.server.account

import com.sumizuri.sync.core.ErrorCodes
import com.sumizuri.sync.core.ServerSettings
import com.sumizuri.sync.server.ApiException
import com.sumizuri.sync.server.LiveSettings
import com.sumizuri.sync.server.auth.Session
import com.sumizuri.sync.server.auth.Sessions
import com.sumizuri.sync.server.badRequest
import com.sumizuri.sync.server.db.Libraries
import com.sumizuri.sync.server.db.Snapshots
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
import io.ktor.server.routing.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert

// The full locked copy of a library: a device sends a new one, which takes the place of the last, and any device of the account fetches it. The server never opens it.
fun Route.snapshotRoutes(db: Database, sessions: Sessions, maxBytes: Long, live: LiveSettings) {
    put("/v1/libraries/{id}/snapshot") {
        val session = sessions.of(call)
        val id = ownLibrary(db, session, call)
        if ((call.request.contentLength() ?: 0) > maxBytes) throw tooLarge(maxBytes)
        val bytes = call.receive<ByteArray>()
        if (bytes.isEmpty()) badRequest("The snapshot is empty.")
        if (bytes.size > maxBytes) throw tooLarge(maxBytes)
        // The storage limit of each person counts their other profiles too, since this copy takes the place of the last one of this profile.
        val limitMb = live.number(ServerSettings.UserStorageMb)
        if (limitMb > 0) {
            val others = withContext(Dispatchers.IO) {
                transaction(db) {
                    Libraries
                        .join(Snapshots, JoinType.INNER, Libraries.id, Snapshots.libraryId)
                        .select(Snapshots.size)
                        .where { (Libraries.userId eq session.userId) and (Libraries.id neq id) }
                        .sumOf { it[Snapshots.size] }
                }
            }
            if (others + bytes.size >
                limitMb * 1_048_576L
            ) {
                throw ApiException(
                    HttpStatusCode.PayloadTooLarge,
                    ErrorCodes.TooLarge,
                    "This would go over your storage limit on this server ($limitMb MB).",
                )
            }
        }
        // A device that says which copy it built on only replaces that copy, so two devices sending at once cannot lose each other's changes. Zero means it built on no copy.
        val base = call.request.headers["X-Snapshot-Base"]?.toLongOrNull()
        val at = withContext(Dispatchers.IO) {
            transaction(db) {
                val current = Snapshots
                    .selectAll()
                    .where { Snapshots.libraryId eq id }
                    .singleOrNull()
                    ?.get(Snapshots.createdAt) ?: 0L
                if (base != null &&
                    base != current
                ) {
                    throw ApiException(
                        HttpStatusCode.Conflict,
                        ErrorCodes.Changed,
                        "The copy on the server changed since this device read it.",
                    )
                }
                val now = maxOf(System.currentTimeMillis(), current + 1)
                Snapshots.upsert {
                    it[libraryId] = id
                    it[data] = ExposedBlob(bytes)
                    it[size] = bytes.size.toLong()
                    it[createdAt] = now
                    it[deviceName] = session.deviceName
                }
                now
            }
        }
        call.response.header("X-Snapshot-At", at.toString())
        call.respond(HttpStatusCode.NoContent)
    }

    get("/v1/libraries/{id}/snapshot") {
        val session = sessions.of(call)
        val id = ownLibrary(db, session, call)
        val row = withContext(Dispatchers.IO) {
            transaction(db) {
                Snapshots
                    .selectAll()
                    .where { Snapshots.libraryId eq id }
                    .singleOrNull()
                    ?.let { it[Snapshots.data].bytes to it[Snapshots.createdAt] }
            }
        }
            ?: throw ApiException(HttpStatusCode.NotFound, ErrorCodes.NotFound, "This library has no snapshot yet.")
        call.response.header("X-Snapshot-At", row.second.toString())
        call.respondBytes(row.first, ContentType.Application.OctetStream)
    }
}

// The library in the address, when it belongs to the account that asked.
private suspend fun ownLibrary(db: Database, session: Session, call: RoutingCall): Long {
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
    "The snapshot is bigger than this server takes (${maxBytes / 1_048_576} MB).",
)
