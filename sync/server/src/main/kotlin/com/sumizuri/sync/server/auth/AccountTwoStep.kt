package com.sumizuri.sync.server.auth

import com.sumizuri.sync.core.AccountDelete
import com.sumizuri.sync.core.DeviceInfo
import com.sumizuri.sync.core.ErrorCodes
import com.sumizuri.sync.core.Kdf
import com.sumizuri.sync.core.Limits
import com.sumizuri.sync.core.NewAccount
import com.sumizuri.sync.core.NewPassword
import com.sumizuri.sync.core.PasswordChange
import com.sumizuri.sync.core.RecoverRequest
import com.sumizuri.sync.core.RecoveryChange
import com.sumizuri.sync.core.RecoveryKeyAnswer
import com.sumizuri.sync.core.RecoveryKeyRequest
import com.sumizuri.sync.core.Role
import com.sumizuri.sync.core.SaltAnswer
import com.sumizuri.sync.core.SetupRequest
import com.sumizuri.sync.core.SignedIn
import com.sumizuri.sync.core.SigninRequest
import com.sumizuri.sync.core.SignupMode
import com.sumizuri.sync.core.SignupRequest
import com.sumizuri.sync.core.TwoStepConfirm
import com.sumizuri.sync.core.TwoStepOff
import com.sumizuri.sync.core.TwoStepSecret
import com.sumizuri.sync.core.TwoStepStart
import com.sumizuri.sync.core.UsernameChange
import com.sumizuri.sync.server.ApiException
import com.sumizuri.sync.server.ChangeLog
import com.sumizuri.sync.server.Setup
import com.sumizuri.sync.server.admin.softDelete
import com.sumizuri.sync.server.badRequest
import com.sumizuri.sync.server.db.Devices
import com.sumizuri.sync.server.db.Invites
import com.sumizuri.sync.server.db.Users
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

// Starts turning on two-step sign in for a signed in person, with a new secret that waits until a code from it is confirmed, so an app that was never set up cannot lock the person out.
fun Accounts.startTwoStep(session: Session, change: TwoStepStart, serverName: String): TwoStepSecret {
    val row = userRow(session.userId)
    if (row[Users.twoStepSecret] != null) {
        throw ApiException(
            HttpStatusCode.Conflict,
            ErrorCodes.NotAllowed,
            "Two-step sign in is on already. Turn it off first to set up a new app.",
        )
    }
    checkCurrent(session, change.currentProof, null)
    val secret = Totp.newSecret()
    transaction(db) { Users.update({ Users.id eq session.userId }) { it[twoStepPending] = secret } }
    return TwoStepSecret(secret, Totp.link(secret, "Sumizuri", "${session.username} ($serverName)"))
}

// The first code from the app turns two-step sign in on.
fun Accounts.confirmTwoStep(session: Session, change: TwoStepConfirm) {
    val row = userRow(session.userId)
    val pending = row[Users.twoStepPending]
        ?: throw ApiException(
            HttpStatusCode.Conflict,
            ErrorCodes.NotAllowed,
            "Start turning on two-step sign in first.",
        )
    val step = Totp.step(pending, change.code, 0) ?: throw wrongTwoStep()
    transaction(db) {
        Users.update({ Users.id eq session.userId }) {
            it[twoStepSecret] = pending
            it[twoStepPending] = null
            it[twoStepUsed] = step
        }
        ChangeLog.record(session.username, "turned on two-step sign in")
    }
}

// Turns two-step sign in off, given the password and a code.
fun Accounts.turnOffTwoStep(session: Session, change: TwoStepOff) {
    val row = checkCurrent(session, change.currentProof, change.code)
    if (row[Users.twoStepSecret] == null && row[Users.twoStepPending] == null) return
    transaction(db) {
        Users.update({ Users.id eq session.userId }) {
            it[twoStepSecret] = null
            it[twoStepPending] = null
        }
        ChangeLog.record(session.username, "turned off two-step sign in")
    }
}
