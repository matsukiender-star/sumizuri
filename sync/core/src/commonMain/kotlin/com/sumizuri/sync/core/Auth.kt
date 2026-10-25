package com.sumizuri.sync.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// How the app turns a password into its proof and its unlock key. It is kept with each account, so a stronger setting can come later without breaking older accounts.
@Serializable
data class Kdf(
    val algorithm: String = "argon2id",
    val memoryKib: Int = 65536,
    val iterations: Int = 3,
    val parallelism: Int = 1,
) {
    companion object {
        // The weakest setting the server takes, so an app can never store an account that is quick to guess.
        const val MinMemoryKib = 19456
        const val MinIterations = 2
    }
}

@Serializable
enum class Role {
    @SerialName("owner")
    Owner,

    @SerialName("admin")
    Admin,

    @SerialName("user")
    User,
}

@Serializable
enum class DeviceType {
    @SerialName("phone")
    Phone,

    @SerialName("tablet")
    Tablet,

    @SerialName("desktop")
    Desktop,
}

// What a device says about itself when it signs in. The owner and admins see it.
@Serializable
data class DeviceInfo(val name: String, val type: DeviceType, val appVersion: String)

// A password as the server keeps it: the salt and setting the app used, the proof made from it, and the account key locked with the unlock key. All bytes are base64.
@Serializable
data class NewPassword(val salt: String, val kdf: Kdf, val proof: String, val keyByPassword: String)

// A new account: its name, its password, and the account key locked a second time with the recovery key, with a proof of that key.
@Serializable
data class NewAccount(
    val username: String,
    val password: NewPassword,
    val recoveryProof: String,
    val keyByRecovery: String,
)

// Claims a new server with the code from its log.
@Serializable
data class SetupRequest(val code: String, val account: NewAccount, val device: DeviceInfo)

@Serializable
data class SignupRequest(val account: NewAccount, val device: DeviceInfo, val invite: String? = null)

// The salt and setting to use for a username, so the app can make the same proof again.
@Serializable
data class SaltAnswer(val salt: String, val kdf: Kdf)

// The code from an authenticator app goes along once the server asked for it, when the account has two-step sign in.
@Serializable
data class SigninRequest(val username: String, val proof: String, val device: DeviceInfo, val code: String? = null)

// Asks for the account key locked with the recovery key, so the app can open it and lock it again with a new password.
@Serializable
data class RecoveryKeyRequest(val username: String, val recoveryProof: String)

@Serializable
data class RecoveryKeyAnswer(val keyByRecovery: String)

// Sets a new password with the recovery key, when the old password is lost.
@Serializable
data class RecoverRequest(
    val username: String,
    val recoveryProof: String,
    val password: NewPassword,
    val device: DeviceInfo,
)

// A signed in device: the token it sends from now on, who it is, and the account key locked with the password.
@Serializable
data class SignedIn(
    val token: String,
    val userId: Long,
    val username: String,
    val role: Role,
    val deviceId: Long,
    val keyByPassword: String,
    // A sign in with the recovery key turns two-step sign in off, and says so here.
    val twoStepOff: Boolean = false,
)

// The rules both sides check.
object Limits {
    const val UsernameMin = 3
    const val UsernameMax = 32

    // Letters, digits, dots, dashes and underscores, starting with a letter or digit.
    val UsernamePattern = Regex("^[A-Za-z0-9][A-Za-z0-9._-]*$")
    const val SaltBytes = 16
    const val ProofBytes = 32

    // A locked key is a nonce, the key and a tag, so well under this.
    const val LockedKeyMaxBytes = 256
    const val DeviceNameMax = 64
    const val AppVersionMax = 32
}

// The codes of ApiError, which the app acts on.
object ErrorCodes {
    const val BadRequest = "bad_request"
    const val ServerError = "server_error"
    const val SetupDone = "setup_done"
    const val WrongCode = "wrong_code"
    const val UsernameTaken = "username_taken"
    const val SignupClosed = "signup_closed"
    const val InviteNeeded = "invite_needed"
    const val WrongSignIn = "wrong_sign_in"
    const val WrongRecovery = "wrong_recovery"
    const val TooManyTries = "too_many_tries"
    const val AccountDisabled = "account_disabled"
    const val SignedOut = "signed_out"
    const val NotAllowed = "not_allowed"
    const val NotFound = "not_found"
    const val TooLarge = "too_large"

    // The copy on the server changed since the device read it, so the device merges again before it sends.
    const val Changed = "snapshot_changed"

    // The account has two-step sign in, and the sign in or change needs the code from the authenticator app.
    const val CodeNeeded = "code_needed"
    const val WrongTwoStep = "wrong_two_step"
}
