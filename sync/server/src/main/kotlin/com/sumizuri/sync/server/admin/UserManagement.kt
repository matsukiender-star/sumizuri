package com.sumizuri.sync.server.admin

import com.sumizuri.sync.core.AdminOverview
import com.sumizuri.sync.core.DeviceEntry
import com.sumizuri.sync.core.DeviceType
import com.sumizuri.sync.core.ErrorCodes
import com.sumizuri.sync.core.LogEntry
import com.sumizuri.sync.core.Role
import com.sumizuri.sync.core.SettingChange
import com.sumizuri.sync.core.UserChange
import com.sumizuri.sync.core.UserDetail
import com.sumizuri.sync.core.UserEntry
import com.sumizuri.sync.core.V1
import com.sumizuri.sync.server.ApiException
import com.sumizuri.sync.server.ChangeLog
import com.sumizuri.sync.server.Config
import com.sumizuri.sync.server.LiveSettings
import com.sumizuri.sync.server.ServerVersion
import com.sumizuri.sync.server.account.rowsOfUser
import com.sumizuri.sync.server.auth.Session
import com.sumizuri.sync.server.auth.Sessions
import com.sumizuri.sync.server.badRequest
import com.sumizuri.sync.server.db.AuditLog
import com.sumizuri.sync.server.db.DatabaseSetting
import com.sumizuri.sync.server.db.Devices
import com.sumizuri.sync.server.db.Invites
import com.sumizuri.sync.server.db.Libraries
import com.sumizuri.sync.server.db.ServerState
import com.sumizuri.sync.server.db.Snapshots
import com.sumizuri.sync.server.db.SyncRows
import com.sumizuri.sync.server.db.Users
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

// The overview of the server and the management of one person, for the owner and admins. Nobody can touch the owner, nobody can turn off or delete their own account, and only the owner manages admins and roles.
fun Route.userManagementRoutes(db: Database, sessions: Sessions, config: Config, live: LiveSettings, startedAt: Long) {
    get(V1.AdminOverview) {
        sessions.admin(call)
        val overview = withContext(Dispatchers.IO) {
            transaction(db) {
                val users = Users.selectAll().toList()
                val devices = Devices.selectAll().toList()
                val dayAgo = System.currentTimeMillis() - 86_400_000L
                AdminOverview(
                    serverName = live.name,
                    version = ServerVersion,
                    database = if (config.database is DatabaseSetting.Postgres) "Postgres" else "SQLite",
                    signupMode = live.signupMode,
                    startedAt = startedAt,
                    users = users.size,
                    admins = users.count { roleOf(it) != Role.User },
                    disabled = users.count { it[Users.disabled] },
                    devices = devices.size,
                    activeToday = devices
                        .filter { it[Devices.lastSeen] > dayAgo }
                        .map { it[Devices.userId] }
                        .distinct()
                        .size,
                    profiles = Libraries.selectAll().count().toInt(),
                    storedBytes = Snapshots.select(Snapshots.size).sumOf { it[Snapshots.size] } + (
                        SyncRows
                            .select(SyncRows.size.sum())
                            .singleOrNull()
                            ?.get(SyncRows.size.sum())
                            ?.toLong() ?: 0L
                        ),
                    openInvites = Invites.selectAll().where { Invites.usedBy.isNull() }.count {
                        (
                            it[Invites.expiresAt]
                                ?: Long.MAX_VALUE
                            ) >
                            System.currentTimeMillis()
                    },
                )
            }
        }
        call.respond(overview)
    }

    get("${V1.AdminUsers}/{id}") {
        sessions.admin(call)
        val id = userId(call)
        call.respond(withContext(Dispatchers.IO) { transaction(db) { detailOf(id) } })
    }

    // Turns a person off or on, or changes their role.
    patch("${V1.AdminUsers}/{id}") {
        val session = sessions.admin(call)
        val id = userId(call)
        val change = call.receive<UserChange>()
        val detail = withContext(Dispatchers.IO) {
            transaction(db) {
                val target = userRow(id)
                checkCanManage(session, target)
                if (change.role != null) {
                    if (session.role != Role.Owner) throw notAllowed("Only the owner can change roles.")
                    if (change.role == Role.Owner) throw notAllowed("Handing over ownership is not done here.")
                }
                Users.update({ Users.id eq id }) { row ->
                    change.disabled?.let { row[disabled] = it }
                    change.role?.let { row[role] = it.name.lowercase() }
                }
                change.disabled?.let {
                    ChangeLog.record(session.username, if (it) "turned off" else "turned on", target[Users.username])
                }
                change.role?.let {
                    ChangeLog.record(
                        session.username,
                        "made ${it.name.lowercase()}",
                        target[Users.username],
                    )
                }
                detailOf(id)
            }
        }
        call.respond(detail.user)
    }

    // Signs out every device of a person, such as when their password may be known to someone else.
    post("${V1.AdminUsers}/{id}/signout") {
        val session = sessions.admin(call)
        val id = userId(call)
        withContext(Dispatchers.IO) {
            transaction(db) {
                val target = userRow(id)
                checkCanManage(session, target, allowSelf = true)
                Devices.deleteWhere { userId eq id }
                ChangeLog.record(session.username, "signed out", target[Users.username])
            }
        }
        call.respond(HttpStatusCode.NoContent)
    }

    // Deletes a person. Their devices are signed out at once, and they stay for a week so an admin can bring them back, after which the server removes them with their profiles and copies.
    delete("${V1.AdminUsers}/{id}") {
        val session = sessions.admin(call)
        val id = userId(call)
        withContext(Dispatchers.IO) {
            transaction(db) {
                val target = userRow(id)
                checkCanManage(session, target)
                softDelete(id)
                ChangeLog.record(session.username, "deleted", target[Users.username])
            }
        }
        call.respond(HttpStatusCode.NoContent)
    }

    // Turns off two-step sign in for a person who lost the phone with their authenticator app and has no recovery key at hand.
    post("${V1.AdminUsers}/{id}/two-step-off") {
        val session = sessions.admin(call)
        val id = userId(call)
        withContext(Dispatchers.IO) {
            transaction(db) {
                val target = userRow(id)
                checkCanManage(session, target)
                Users.update({ Users.id eq id }) {
                    it[twoStepSecret] = null
                    it[twoStepPending] = null
                }
                ChangeLog.record(session.username, "turned off two-step for", target[Users.username])
            }
        }
        call.respond(HttpStatusCode.NoContent)
    }

    // Brings back a person deleted in the last week.
    post("${V1.AdminUsers}/{id}/restore") {
        val session = sessions.admin(call)
        val id = userId(call)
        withContext(Dispatchers.IO) {
            transaction(db) {
                val target = userRow(id)
                if (target[Users.deletedAt] == null) throw notAllowed("This person is not deleted.")
                checkCanManage(session, target)
                Users.update({ Users.id eq id }) { it[deletedAt] = null }
                ChangeLog.record(session.username, "restored", target[Users.username])
            }
        }
        call.respond(HttpStatusCode.NoContent)
    }

    // Hands the server over to another person, who becomes the owner while the old owner becomes an admin. Only the owner can, and only to someone turned on and not deleted.
    post("${V1.AdminUsers}/{id}/owner") {
        val session = sessions.admin(call)
        if (session.role != Role.Owner) throw notAllowed("Only the owner can hand over the server.")
        val id = userId(call)
        withContext(Dispatchers.IO) {
            transaction(db) {
                val target = userRow(id)
                if (id == session.userId) throw notAllowed("You own this server already.")
                if (target[Users.disabled] ||
                    target[Users.deletedAt] != null
                ) {
                    throw notAllowed("Turn this person on first.")
                }
                Users.update({ Users.id eq id }) { it[role] = Role.Owner.name.lowercase() }
                Users.update({ Users.id eq session.userId }) { it[role] = Role.Admin.name.lowercase() }
                ServerState.update({ ServerState.key eq "owner" }) { it[value] = id.toString() }
                ChangeLog.record(session.username, "handed over the server", target[Users.username])
            }
        }
        call.respond(HttpStatusCode.NoContent)
    }

    // The change log, newest first.
    get(V1.AdminLog) {
        sessions.admin(call)
        val limit = call.request.queryParameters["limit"]
            ?.toIntOrNull()
            ?.coerceIn(1, LogMost) ?: LogMost
        val lines = withContext(Dispatchers.IO) {
            transaction(db) {
                AuditLog
                    .selectAll()
                    .orderBy(AuditLog.id, SortOrder.DESC)
                    .limit(limit)
                    .map {
                        LogEntry(
                            it[AuditLog.at],
                            it[AuditLog.actor],
                            it[AuditLog.action],
                            it[AuditLog.target],
                            it[AuditLog.detail],
                        )
                    }
            }
        }
        call.respond(lines)
    }
}

private fun userId(call: RoutingCall): Long =
    call.parameters["id"]?.toLongOrNull() ?: badRequest("Give the number of a person.")

private fun userRow(id: Long): ResultRow = Users.selectAll().where { Users.id eq id }.singleOrNull()
    ?: throw ApiException(HttpStatusCode.NotFound, ErrorCodes.NotFound, "There is no such person on this server.")

private fun checkCanManage(session: Session, target: ResultRow, allowSelf: Boolean = false) {
    if (roleOf(target) == Role.Owner) throw notAllowed("The owner cannot be changed here.")
    if (!allowSelf && target[Users.id] == session.userId) throw notAllowed("You cannot do this to your own account.")
    if (session.role != Role.Owner &&
        roleOf(target) == Role.Admin
    ) {
        throw notAllowed("Only the owner can manage admins.")
    }
}

private fun detailOf(id: Long): UserDetail {
    val row = userRow(id)
    val devices = Devices.selectAll().where { Devices.userId eq id }.orderBy(Devices.lastSeen, SortOrder.DESC).map {
        DeviceEntry(
            id = it[Devices.id],
            name = it[Devices.name],
            type =
            DeviceType.entries.firstOrNull { type -> type.name.equals(it[Devices.type], ignoreCase = true) }
                ?: DeviceType.Desktop,
            appVersion = it[Devices.appVersion],
            createdAt = it[Devices.createdAt],
            lastSeen = it[Devices.lastSeen],
            thisOne = false,
        )
    }
    val profiles = Libraries
        .selectAll()
        .where { Libraries.userId eq id }
        .count()
        .toInt()
    val stored = Libraries
        .join(Snapshots, JoinType.INNER, Libraries.id, Snapshots.libraryId)
        .select(Snapshots.size)
        .where { Libraries.userId eq id }
        .sumOf { it[Snapshots.size] } + rowsOfUser(id)
    val user =
        UserEntry(
            row[Users.id], row[Users.username],
            roleOf(
                row,
            ),
            row[Users.createdAt], row[Users.disabled], devices.size,
            devices.maxOfOrNull {
                it.lastSeen
            },
            row[Users.deletedAt],
            row[Users.twoStepSecret] != null,
        )
    return UserDetail(user, devices, profiles, stored)
}

// The most lines of the change log one request gives.
private const val LogMost = 500

// Marks a person deleted and signs out their devices. The server removes them a week later.
internal fun softDelete(id: Long) {
    Users.update({ Users.id eq id }) { it[deletedAt] = System.currentTimeMillis() }
    Devices.deleteWhere { userId eq id }
}

internal fun roleOf(row: ResultRow): Role = Role.entries.first { it.name.equals(row[Users.role], ignoreCase = true) }

private fun notAllowed(message: String) = ApiException(HttpStatusCode.Forbidden, ErrorCodes.NotAllowed, message)

// The live settings of the server, for the owner and admins. A setting the .env pins cannot be changed, and a value that does not fit is turned down.
fun Route.serverSettingsRoutes(sessions: Sessions, live: LiveSettings) {
    get(V1.AdminSettings) {
        sessions.admin(call)
        call.respond(live.entries())
    }

    patch(V1.AdminSettings) {
        val session = sessions.admin(call)
        val change = call.receive<SettingChange>()
        withContext(
            Dispatchers.IO,
        ) { live.set(change.key, change.value, by = session.username) }?.let { badRequest(it) }
        call.respond(live.entries())
    }
}
