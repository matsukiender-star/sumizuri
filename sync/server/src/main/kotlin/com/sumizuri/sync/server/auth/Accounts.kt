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

// Making accounts and signing devices in. The slow Argon2 work is done before a transaction opens, so the database is never held while it runs.
class Accounts(val db: Database, private val setup: Setup) {
    // A check of a random proof, so a sign in with an unknown name takes as long as one with a real name.
    private val decoy = ServerCrypto.hashSecret(ServerCrypto.randomBytes(Limits.ProofBytes))

    fun claimServer(request: SetupRequest): SignedIn {
        checkDevice(request.device)
        val prepared = prepare(request.account)
        return try {
            transaction(db) {
                if (setup.claimedIn()) {
                    throw ApiException(
                        HttpStatusCode.Conflict,
                        ErrorCodes.SetupDone,
                        "This server already has an owner.",
                    )
                }
                if (!setup.matchesIn(request.code)) {
                    throw ApiException(
                        HttpStatusCode.Forbidden,
                        ErrorCodes.WrongCode,
                        "That setup code is wrong. Check the server's log for the newest one.",
                    )
                }
                val userId = insertUser(prepared, Role.Owner)
                setup.claimIn(userId)
                signIn(
                    userId,
                    request.account.username,
                    Role.Owner,
                    request.account.password.keyByPassword,
                    request.device,
                )
            }
        } catch (clash: ExposedSQLException) {
            throw ApiException(HttpStatusCode.Conflict, ErrorCodes.SetupDone, "This server already has an owner.")
        }
    }

    fun signup(request: SignupRequest, mode: SignupMode): SignedIn {
        if (mode == SignupMode.Closed) {
            throw ApiException(
                HttpStatusCode.Forbidden,
                ErrorCodes.SignupClosed,
                "This server does not take new accounts. Ask its owner to make one for you.",
            )
        }
        checkDevice(request.device)
        val prepared = prepare(request.account)
        val inviteHash = request.invite
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { ServerCrypto.codeHash(it) }
        if (mode == SignupMode.Invite && inviteHash == null) {
            throw ApiException(
                HttpStatusCode.Forbidden,
                ErrorCodes.InviteNeeded,
                "This server needs an invite code to make an account.",
            )
        }
        return try {
            transaction(db) {
                if (!setup.claimedIn()) {
                    throw ApiException(
                        HttpStatusCode.Conflict,
                        ErrorCodes.SetupDone,
                        "This server has no owner yet. Set it up first.",
                    )
                }
                if (findIn(keyOf(request.account.username)) != null) throw taken()
                val userId = insertUser(prepared, Role.User)
                if (mode == SignupMode.Invite) useInvite(inviteHash!!, userId)
                signIn(
                    userId,
                    request.account.username,
                    Role.User,
                    request.account.password.keyByPassword,
                    request.device,
                )
            }
        } catch (clash: ExposedSQLException) {
            throw taken()
        }
    }

    // The salt and setting of a name. An unknown name gets a salt made from the server's secret, the same every time, so it looks like a real one.
    fun salt(username: String): SaltAnswer {
        val key = keyOf(username)
        val row = transaction(db) { findIn(key) }
        if (row != null) return SaltAnswer(row[Users.salt], Json.decodeFromString(Kdf.serializer(), row[Users.kdf]))
        return SaltAnswer(
            ServerCrypto.base64(ServerCrypto.hmac(setup.secret, "salt:$key").copyOf(Limits.SaltBytes)),
            Kdf(),
        )
    }

    fun signin(request: SigninRequest): SignedIn {
        checkDevice(request.device)
        val proof = bytes(request.proof, Limits.ProofBytes, "proof")
        val row = transaction(db) { findIn(keyOf(request.username)) }
        val matches = ServerCrypto.checkSecret(proof, row?.get(Users.passwordCheck) ?: decoy)
        // A deleted person is gone as far as signing in goes, even while an admin could still bring them back.
        if (row == null || !matches || row[Users.deletedAt] != null) throw wrongSignIn()
        if (row[Users.disabled]) {
            throw ApiException(
                HttpStatusCode.Forbidden,
                ErrorCodes.AccountDisabled,
                "This account was turned off by the server's owner.",
            )
        }
        checkCode(row, request.code)
        return transaction(db) {
            signIn(row[Users.id], row[Users.username], roleOf(row), row[Users.keyByPassword], request.device)
        }
    }

    // The account key locked with the recovery key, given only for the right recovery proof.
    fun recoveryKey(request: RecoveryKeyRequest): RecoveryKeyAnswer =
        RecoveryKeyAnswer(checkRecovery(request.username, request.recoveryProof)[Users.keyByRecovery])

    // A new password set with the recovery key. Every other device is signed out, since the old password may be known to someone else. Two-step sign in is turned off too, since the recovery key is what is left when the phone with the authenticator app is lost.
    fun recover(request: RecoverRequest): SignedIn {
        checkDevice(request.device)
        checkPassword(request.password)
        val row = checkRecovery(request.username, request.recoveryProof)
        val passwordCheck = ServerCrypto.hashSecret(bytes(request.password.proof, Limits.ProofBytes, "proof"))
        val userId = row[Users.id]
        return transaction(db) {
            Users.update({ Users.id eq userId }) {
                it[salt] = request.password.salt
                it[kdf] = Json.encodeToString(Kdf.serializer(), request.password.kdf)
                it[Users.passwordCheck] = passwordCheck
                it[keyByPassword] = request.password.keyByPassword
                it[twoStepSecret] = null
                it[twoStepPending] = null
            }
            Devices.deleteWhere { Devices.userId eq userId }
            val hadTwoStep = row[Users.twoStepSecret] != null
            if (hadTwoStep) ChangeLog.record(row[Users.username], "two-step off by recovery")
            signIn(
                userId,
                row[Users.username],
                roleOf(row),
                request.password.keyByPassword,
                request.device,
            ).copy(twoStepOff = hadTwoStep)
        }
    }

    fun keyOf(username: String): String = username.trim().lowercase()

    // A new password for a signed in account, given the proof of the current one. The other devices can be signed out with it.
    fun changePassword(session: Session, change: PasswordChange) {
        checkPassword(change.password)
        checkCurrent(session, change.currentProof, change.code)
        val passwordCheck = ServerCrypto.hashSecret(bytes(change.password.proof, Limits.ProofBytes, "proof"))
        transaction(db) {
            Users.update({ Users.id eq session.userId }) {
                it[salt] = change.password.salt
                it[kdf] = Json.encodeToString(Kdf.serializer(), change.password.kdf)
                it[Users.passwordCheck] = passwordCheck
                it[keyByPassword] = change.password.keyByPassword
            }
            if (change.signOutOthers) {
                Devices.deleteWhere { (Devices.userId eq session.userId) and (Devices.id neq session.deviceId) }
            }
        }
    }

    // Deletes the account of a signed in person: the devices are signed out at once, and an admin can bring the account back for a week, after which the server removes it with its profiles and copies. The owner keeps the server, so the owner cannot.
    fun deleteSelf(session: Session, change: AccountDelete) {
        checkCurrent(session, change.currentProof, change.code)
        if (session.role == Role.Owner) {
            throw ApiException(
                HttpStatusCode.Forbidden,
                ErrorCodes.NotAllowed,
                "The owner of the server cannot delete their own account.",
            )
        }
        transaction(db) {
            softDelete(session.userId)
            ChangeLog.record(session.username, "deleted own account")
        }
    }

    // A new username, checked as a new account's name is, that nobody else has.
    fun changeUsername(session: Session, change: UsernameChange) {
        checkCurrent(session, change.currentProof, change.code)
        val name = change.username.trim()
        checkUsername(name)
        transaction(db) {
            val other = findIn(keyOf(name))
            if (other != null && other[Users.id] != session.userId) throw taken()
            Users.update({ Users.id eq session.userId }) {
                it[username] = name
                it[usernameKey] = keyOf(name)
            }
            ChangeLog.record(session.username, "renamed self", name)
        }
    }

    // A new recovery key takes the place of the old one, which stops working.
    fun changeRecovery(session: Session, change: RecoveryChange) {
        checkCurrent(session, change.currentProof, change.code)
        lockedKey(change.keyByRecovery, "recovery key")
        val check = ServerCrypto.hashSecret(bytes(change.recoveryProof, Limits.ProofBytes, "recovery proof"))
        transaction(db) {
            Users.update({ Users.id eq session.userId }) {
                it[recoveryCheck] = check
                it[keyByRecovery] = change.keyByRecovery
            }
        }
    }

    // Checks the proof of the current password of a signed in person, and the code when the account has two-step sign in.
    internal fun checkCurrent(session: Session, proof: String, code: String?): ResultRow {
        val current = bytes(proof, Limits.ProofBytes, "current proof")
        val row = userRow(session.userId)
        if (!ServerCrypto.checkSecret(current, row[Users.passwordCheck])) throw wrongSignIn()
        checkCode(row, code)
        return row
    }

    // With two-step sign in on, asks for the code, and takes a right one only once, so a code seen over a shoulder does not work again.
    private fun checkCode(row: ResultRow, code: String?) {
        val secret = row[Users.twoStepSecret] ?: return
        val typed = code?.takeIf { it.isNotBlank() }
            ?: throw ApiException(
                HttpStatusCode.Unauthorized,
                ErrorCodes.CodeNeeded,
                "Type the code from your authenticator app.",
            )
        val step = Totp.step(secret, typed, row[Users.twoStepUsed]) ?: throw wrongTwoStep()
        val used = transaction(db) {
            Users.update({ (Users.id eq row[Users.id]) and (Users.twoStepUsed less step) }) { it[twoStepUsed] = step }
        }
        if (used == 0) throw wrongTwoStep()
    }

    internal fun userRow(id: Long): ResultRow = transaction(db) { Users.selectAll().where { Users.id eq id }.single() }

    // The account of a name when the recovery proof is right, taking the same time for a name that does not exist.
    private fun checkRecovery(username: String, recoveryProof: String): ResultRow {
        val proof = bytes(recoveryProof, Limits.ProofBytes, "recovery proof")
        val row = transaction(db) { findIn(keyOf(username)) }
        val matches = ServerCrypto.checkSecret(proof, row?.get(Users.recoveryCheck) ?: decoy)
        if (row == null || !matches || row[Users.deletedAt] != null) {
            throw ApiException(
                HttpStatusCode.Unauthorized,
                ErrorCodes.WrongRecovery,
                "The username or recovery key is wrong.",
            )
        }
        return row
    }

    private fun insertUser(prepared: Prepared, role: Role): Long {
        val account = prepared.account
        return Users.insert {
            it[username] = account.username.trim()
            it[usernameKey] = keyOf(account.username)
            it[Users.role] = role.name.lowercase()
            it[salt] = account.password.salt
            it[kdf] = Json.encodeToString(Kdf.serializer(), account.password.kdf)
            it[passwordCheck] = prepared.passwordCheck
            it[keyByPassword] = account.password.keyByPassword
            it[recoveryCheck] = prepared.recoveryCheck
            it[keyByRecovery] = account.keyByRecovery
            it[createdAt] = System.currentTimeMillis()
        }[Users.id]
    }

    // Marks an invite as used by this account, or turns the sign up down when the code is unknown, used or old.
    private fun useInvite(hash: String, userId: Long) {
        val now = System.currentTimeMillis()
        val used = Invites.update({ (Invites.codeHash eq hash) and Invites.usedBy.isNull() }) { it[usedBy] = userId }
        val expiresAt = Invites
            .selectAll()
            .where { Invites.codeHash eq hash }
            .singleOrNull()
            ?.get(Invites.expiresAt)
        val expired = expiresAt != null && expiresAt < now
        if (used == 0 || expired) {
            throw ApiException(
                HttpStatusCode.Forbidden,
                ErrorCodes.InviteNeeded,
                "That invite code is wrong, used or too old.",
            )
        }
    }

    // Adds the device and gives back its token, which is shown to the device once and kept only as a hash.
    private fun signIn(
        userId: Long,
        username: String,
        role: Role,
        keyByPassword: String,
        device: DeviceInfo,
    ): SignedIn {
        val token = ServerCrypto.newToken()
        val now = System.currentTimeMillis()
        val deviceId = Devices.insert {
            it[Devices.userId] = userId
            it[name] = device.name.trim()
            it[type] = device.type.name.lowercase()
            it[appVersion] = device.appVersion.trim()
            it[tokenHash] = ServerCrypto.sha256Hex(token)
            it[createdAt] = now
            it[lastSeen] = now
        }[Devices.id]
        return SignedIn(token, userId, username, role, deviceId, keyByPassword)
    }

    private fun findIn(key: String): ResultRow? = Users.selectAll().where { Users.usernameKey eq key }.singleOrNull()

    private fun roleOf(row: ResultRow): Role = Role.entries.first { it.name.equals(row[Users.role], ignoreCase = true) }
}
