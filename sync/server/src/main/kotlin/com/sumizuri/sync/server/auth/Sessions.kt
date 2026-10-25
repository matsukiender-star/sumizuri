package com.sumizuri.sync.server.auth

import com.sumizuri.sync.core.ErrorCodes
import com.sumizuri.sync.core.Role
import com.sumizuri.sync.server.ApiException
import com.sumizuri.sync.server.db.Devices
import com.sumizuri.sync.server.db.Users
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

// Who sent a request: the account, the device and the account's role.
data class Session(val userId: Long, val username: String, val deviceId: Long, val deviceName: String, val role: Role) {
    val isAdmin: Boolean get() = role == Role.Owner || role == Role.Admin
}

// Finds the device of a token. A device that was signed out, or an account that was turned off, is turned away.
class Sessions(private val db: Database) {
    fun of(call: ApplicationCall): Session {
        val header = call.request.headers["Authorization"].orEmpty()
        val token =
            header.removePrefix("Bearer ").trim().takeIf { header.startsWith("Bearer ") && it.isNotEmpty() }
                ?: throw signedOut()
        val hash = ServerCrypto.sha256Hex(token)
        val now = System.currentTimeMillis()
        val row = transaction(db) {
            Devices
                .join(Users, JoinType.INNER, Devices.userId, Users.id)
                .selectAll()
                .where {
                    Devices.tokenHash eq hash
                }.singleOrNull()
                ?.also { found ->
                    // When the device was last seen is kept to the minute, so a busy device does not write on every request.
                    if (now - found[Devices.lastSeen] >
                        60_000
                    ) {
                        Devices.update({ Devices.id eq found[Devices.id] }) { it[lastSeen] = now }
                    }
                }
        } ?: throw signedOut()
        if (row[Users.disabled]) {
            throw ApiException(
                HttpStatusCode.Forbidden,
                ErrorCodes.AccountDisabled,
                "This account was turned off by the server's owner.",
            )
        }
        val role = Role.entries.first { it.name.equals(row[Users.role], ignoreCase = true) }
        return Session(row[Users.id], row[Users.username], row[Devices.id], row[Devices.name], role)
    }

    // The same, for the addresses only the owner and admins may use.
    fun admin(call: ApplicationCall): Session = of(call).also {
        if (!it.isAdmin) {
            throw ApiException(
                HttpStatusCode.Forbidden,
                ErrorCodes.NotAllowed,
                "Only the owner and admins can do this.",
            )
        }
    }

    private fun signedOut() =
        ApiException(HttpStatusCode.Unauthorized, ErrorCodes.SignedOut, "This device is signed out. Sign in again.")
}
