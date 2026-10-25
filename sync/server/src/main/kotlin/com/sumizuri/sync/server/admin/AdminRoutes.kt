package com.sumizuri.sync.server.admin

import com.sumizuri.sync.core.ErrorCodes
import com.sumizuri.sync.core.InviteEntry
import com.sumizuri.sync.core.InviteMade
import com.sumizuri.sync.core.NewInvite
import com.sumizuri.sync.core.Role
import com.sumizuri.sync.core.UserEntry
import com.sumizuri.sync.core.V1
import com.sumizuri.sync.server.ApiException
import com.sumizuri.sync.server.ChangeLog
import com.sumizuri.sync.server.auth.ServerCrypto
import com.sumizuri.sync.server.auth.Sessions
import com.sumizuri.sync.server.badRequest
import com.sumizuri.sync.server.db.Devices
import com.sumizuri.sync.server.db.Invites
import com.sumizuri.sync.server.db.Users
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

// How many characters of an invite's hash the list knows it by.
private const val InviteIdLength = 12

// The addresses only the owner and admins may use: the people on the server and the invite codes.
fun Route.adminRoutes(db: Database, sessions: Sessions) {
    get(V1.AdminUsers) {
        sessions.admin(call)
        val users = withContext(Dispatchers.IO) {
            transaction(db) {
                val devices = Devices.selectAll().groupBy { it[Devices.userId] }
                Users.selectAll().orderBy(Users.createdAt).map { row ->
                    val own = devices[row[Users.id]].orEmpty()
                    UserEntry(
                        id = row[Users.id],
                        username = row[Users.username],
                        role = Role.entries.first { it.name.equals(row[Users.role], ignoreCase = true) },
                        createdAt = row[Users.createdAt],
                        disabled = row[Users.disabled],
                        devices = own.size,
                        lastSeen = own.maxOfOrNull { it[Devices.lastSeen] },
                        deletedAt = row[Users.deletedAt],
                        twoStep = row[Users.twoStepSecret] != null,
                    )
                }
            }
        }
        call.respond(users)
    }

    get(V1.AdminInvites) {
        sessions.admin(call)
        call.respond(withContext(Dispatchers.IO) { transaction(db) { invitesIn() } })
    }

    // Makes an invite code and shows it this once. Only its hash is kept.
    post(V1.AdminInvites) {
        val session = sessions.admin(call)
        val request = call.receive<NewInvite>()
        val days = request.days
        if (days != null && days !in 1..365) badRequest("An invite lasts 1 to 365 days, or forever.")
        val code = ServerCrypto.readableCode()
        val hash = ServerCrypto.codeHash(code)
        val now = System.currentTimeMillis()
        val expires = days?.let { now + it * 86_400_000L }
        withContext(Dispatchers.IO) {
            transaction(db) {
                Invites.insert {
                    it[codeHash] = hash
                    it[createdBy] = session.userId
                    it[createdAt] = now
                    it[expiresAt] = expires
                }
                ChangeLog.record(session.username, "made an invite", detail = days?.let { "$it days" } ?: "no end")
            }
        }
        call.respond(InviteMade(code, InviteEntry(hash.take(InviteIdLength), session.username, now, expires, null)))
    }

    delete("${V1.AdminInvites}/{id}") {
        val session = sessions.admin(call)
        val id =
            call.parameters["id"]?.takeIf {
                it.length == InviteIdLength &&
                    it.all { c -> c in '0'..'9' || c in 'a'..'f' }
            }
                ?: badRequest("Give the id of an invite.")
        val removed = withContext(Dispatchers.IO) {
            transaction(db) {
                Invites.deleteWhere { codeHash like "$id%" }.also {
                    if (it >
                        0
                    ) {
                        ChangeLog.record(session.username, "removed an invite")
                    }
                }
            }
        }
        if (removed == 0) throw ApiException(HttpStatusCode.NotFound, ErrorCodes.NotFound, "There is no such invite.")
        call.respond(HttpStatusCode.NoContent)
    }
}

// Every invite, newest first, with the names of who made and who used it.
private fun invitesIn(): List<InviteEntry> {
    val names = Users.selectAll().associate { it[Users.id] to it[Users.username] }
    return Invites.selectAll().orderBy(Invites.createdAt, SortOrder.DESC).map { row: ResultRow ->
        InviteEntry(
            id = row[Invites.codeHash].take(InviteIdLength),
            createdBy = names[row[Invites.createdBy]].orEmpty(),
            createdAt = row[Invites.createdAt],
            expiresAt = row[Invites.expiresAt],
            usedBy = row[Invites.usedBy]?.let { names[it] },
        )
    }
}
