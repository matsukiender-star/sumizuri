package com.sumizuri.sync.server.account

import com.sumizuri.sync.core.AccountAnswer
import com.sumizuri.sync.core.AccountDelete
import com.sumizuri.sync.core.DeviceEntry
import com.sumizuri.sync.core.DeviceType
import com.sumizuri.sync.core.ErrorCodes
import com.sumizuri.sync.core.LibraryEntry
import com.sumizuri.sync.core.Limits
import com.sumizuri.sync.core.NewLibrary
import com.sumizuri.sync.core.PasswordChange
import com.sumizuri.sync.core.RecoveryChange
import com.sumizuri.sync.core.TwoStepConfirm
import com.sumizuri.sync.core.TwoStepOff
import com.sumizuri.sync.core.TwoStepStart
import com.sumizuri.sync.core.UsernameChange
import com.sumizuri.sync.core.V1
import com.sumizuri.sync.server.ApiException
import com.sumizuri.sync.server.LiveSettings
import com.sumizuri.sync.server.auth.Accounts
import com.sumizuri.sync.server.auth.ServerCrypto
import com.sumizuri.sync.server.auth.Session
import com.sumizuri.sync.server.auth.Sessions
import com.sumizuri.sync.server.auth.TryLimiter
import com.sumizuri.sync.server.auth.confirmTwoStep
import com.sumizuri.sync.server.auth.startTwoStep
import com.sumizuri.sync.server.auth.turnOffTwoStep
import com.sumizuri.sync.server.badRequest
import com.sumizuri.sync.server.db.Devices
import com.sumizuri.sync.server.db.Libraries
import com.sumizuri.sync.server.db.Snapshots
import com.sumizuri.sync.server.db.SyncRows
import com.sumizuri.sync.server.db.Users
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

// The addresses of a signed in device: who it is, the account's devices and libraries, and signing out.
fun Route.accountRoutes(accounts: Accounts, sessions: Sessions, limiter: TryLimiter, live: LiveSettings) {
    val db = accounts.db

    // A new password, given the proof of the current one. Wrong tries count the same as signing in.
    post(V1.Password) {
        val session = sessions.of(call)
        val change = call.receive<PasswordChange>()
        val key = limiter.accountKey(accounts.keyOf(session.username))
        limiter.check(key)
        try {
            withContext(Dispatchers.IO) { accounts.changePassword(session, change) }
        } catch (problem: ApiException) {
            if (problem.code == ErrorCodes.WrongSignIn || problem.code == ErrorCodes.WrongTwoStep) limiter.failed(key)
            throw problem
        }
        call.respond(HttpStatusCode.NoContent)
    }

    // Deleting the account, a new username and a new recovery key each need the current password, and wrong tries count the same as signing in.
    post(V1.AccountDelete) {
        withPassword(
            accounts,
            sessions,
            limiter,
        ) { session -> accounts.deleteSelf(session, call.receive<AccountDelete>()) }
    }

    post(V1.Username) {
        withPassword(
            accounts,
            sessions,
            limiter,
        ) { session -> accounts.changeUsername(session, call.receive<UsernameChange>()) }
    }

    post(V1.Recovery) {
        withPassword(
            accounts,
            sessions,
            limiter,
        ) { session -> accounts.changeRecovery(session, call.receive<RecoveryChange>()) }
    }

    // Two-step sign in: starting gives a new secret, the first code turns it on, and the password with a code turns it off.
    post(V1.TwoStepStart) {
        val session = sessions.of(call)
        val change = call.receive<TwoStepStart>()
        call.respond(counting(accounts, session, limiter) { accounts.startTwoStep(session, change, live.name) })
    }

    post(V1.TwoStepConfirm) {
        withPassword(
            accounts,
            sessions,
            limiter,
        ) { session -> accounts.confirmTwoStep(session, call.receive<TwoStepConfirm>()) }
    }

    post(V1.TwoStepOff) {
        withPassword(
            accounts,
            sessions,
            limiter,
        ) { session -> accounts.turnOffTwoStep(session, call.receive<TwoStepOff>()) }
    }

    // Signs out every other device of the account, such as when one was lost.
    post(V1.SignoutOthers) {
        val session = sessions.of(call)
        withContext(Dispatchers.IO) {
            transaction(db) {
                Devices.deleteWhere {
                    (Devices.userId eq session.userId) and
                        (Devices.id neq session.deviceId)
                }
            }
        }
        call.respond(HttpStatusCode.NoContent)
    }

    post(V1.Signout) {
        val session = sessions.of(call)
        withContext(Dispatchers.IO) { transaction(db) { Devices.deleteWhere { Devices.id eq session.deviceId } } }
        call.respond(HttpStatusCode.NoContent)
    }

    get(V1.Account) {
        val session = sessions.of(call)
        val row =
            withContext(
                Dispatchers.IO,
            ) { transaction(db) { Users.selectAll().where { Users.id eq session.userId }.single() } }
        call.respond(
            AccountAnswer(
                session.userId,
                session.username,
                session.role,
                row[Users.createdAt],
                session.deviceId,
                twoStep =
                row[Users.twoStepSecret] != null,
            ),
        )
    }

    get(V1.Devices) {
        val session = sessions.of(call)
        val devices = withContext(Dispatchers.IO) {
            transaction(db) {
                Devices
                    .selectAll()
                    .where { Devices.userId eq session.userId }
                    .orderBy(Devices.lastSeen, SortOrder.DESC)
                    .map { deviceOf(it, session.deviceId) }
            }
        }
        call.respond(devices)
    }

    // Signs out one of the account's own devices, such as one that was lost.
    delete("${V1.Devices}/{id}") {
        val session = sessions.of(call)
        val id = call.parameters["id"]?.toLongOrNull() ?: badRequest("Give the number of a device.")
        val removed = withContext(Dispatchers.IO) {
            transaction(db) {
                Devices.deleteWhere {
                    (Devices.id eq id) and
                        (Devices.userId eq session.userId)
                }
            }
        }
        if (removed ==
            0
        ) {
            throw ApiException(
                HttpStatusCode.NotFound,
                ErrorCodes.NotFound,
                "There is no such device on this account.",
            )
        }
        call.respond(HttpStatusCode.NoContent)
    }

    get(V1.Libraries) {
        val session = sessions.of(call)
        val libraries = withContext(Dispatchers.IO) {
            // The details of each snapshot come along, but never its bytes.
            transaction(db) {
                val total = SyncRows.size.sum()
                val rowBytes = Libraries
                    .join(SyncRows, JoinType.INNER, Libraries.id, SyncRows.libraryId)
                    .select(Libraries.id, total)
                    .where { Libraries.userId eq session.userId }
                    .groupBy(Libraries.id)
                    .associate { it[Libraries.id] to (it[total]?.toLong() ?: 0L) }
                Libraries
                    .join(Snapshots, JoinType.LEFT, Libraries.id, Snapshots.libraryId)
                    .select(Libraries.columns + listOf(Snapshots.size, Snapshots.createdAt, Snapshots.deviceName))
                    .where { Libraries.userId eq session.userId }
                    .orderBy(Libraries.createdAt)
                    .map { libraryOf(it, rowBytes[it[Libraries.id]]) }
            }
        }
        call.respond(libraries)
    }

    post(V1.Libraries) {
        val session = sessions.of(call)
        val request = call.receive<NewLibrary>()
        for ((text, what) in listOf(request.name to "name", request.keyByAccount to "key")) {
            val bytes = ServerCrypto.fromBase64(text) ?: badRequest("The library's $what is not base64.")
            if (bytes.isEmpty() ||
                bytes.size > Limits.LockedKeyMaxBytes * 4
            ) {
                badRequest("The library's $what has the wrong size.")
            }
        }
        val made = withContext(Dispatchers.IO) {
            transaction(db) {
                val now = System.currentTimeMillis()
                val id = Libraries.insert {
                    it[userId] = session.userId
                    it[name] = request.name
                    it[keyByAccount] = request.keyByAccount
                    it[createdAt] = now
                }[Libraries.id]
                LibraryEntry(id, request.name, request.keyByAccount, now)
            }
        }
        call.respond(made)
    }
}

private fun deviceOf(row: ResultRow, current: Long) = DeviceEntry(
    id = row[Devices.id],
    name = row[Devices.name],
    type = DeviceType.entries.firstOrNull {
        it.name.equals(
            row[Devices.type],
            ignoreCase = true,
        )
    } ?: DeviceType.Desktop,
    appVersion = row[Devices.appVersion],
    createdAt = row[Devices.createdAt],
    lastSeen = row[Devices.lastSeen],
    thisOne = row[Devices.id] == current,
)

private fun libraryOf(row: ResultRow, rowBytes: Long?) = LibraryEntry(
    id = row[Libraries.id],
    name = row[Libraries.name],
    keyByAccount = row[Libraries.keyByAccount],
    createdAt = row[Libraries.createdAt],
    snapshotAt = row.getOrNull(Snapshots.createdAt),
    snapshotSize = row.getOrNull(Snapshots.size),
    snapshotDevice = row.getOrNull(Snapshots.deviceName),
    rowHead = row[Libraries.rowHead],
    rowsAt = row[Libraries.rowsAt],
    rowsSize = rowBytes,
)

// Runs a change of the signed in account that needs its current password, counting a wrong one against the account, and answers with nothing when it worked.
private suspend fun RoutingContext.withPassword(
    accounts: Accounts,
    sessions: Sessions,
    limiter: TryLimiter,
    block: suspend (Session) -> Unit,
) {
    val session = sessions.of(call)
    counting(accounts, session, limiter) { block(session) }
    call.respond(HttpStatusCode.NoContent)
}

// Runs work that checks the password or a two-step code, counting a wrong one against the account the same as a wrong sign in.
private suspend fun <T> counting(accounts: Accounts, session: Session, limiter: TryLimiter, block: suspend () -> T): T {
    val key = limiter.accountKey(accounts.keyOf(session.username))
    limiter.check(key)
    try {
        return withContext(Dispatchers.IO) { block() }
    } catch (problem: ApiException) {
        if (problem.code == ErrorCodes.WrongSignIn || problem.code == ErrorCodes.WrongTwoStep) limiter.failed(key)
        throw problem
    }
}
