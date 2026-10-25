package com.sumizuri.sync.server.auth

import com.sumizuri.sync.core.DeviceInfo
import com.sumizuri.sync.core.ErrorCodes
import com.sumizuri.sync.core.Kdf
import com.sumizuri.sync.core.Limits
import com.sumizuri.sync.core.NewAccount
import com.sumizuri.sync.core.NewPassword
import com.sumizuri.sync.server.ApiException
import com.sumizuri.sync.server.badRequest
import io.ktor.http.HttpStatusCode

// An account checked and with its proofs hashed, ready to be stored.
internal class Prepared(val account: NewAccount, val passwordCheck: String, val recoveryCheck: String)

// Checks a new account and hashes its proofs. This is the slow Argon2 work, done before a transaction opens.
internal fun prepare(account: NewAccount): Prepared {
    checkUsername(account.username.trim())
    checkPassword(account.password)
    lockedKey(account.keyByRecovery, "recovery key")
    val recovery = bytes(account.recoveryProof, Limits.ProofBytes, "recovery proof")
    val proof = bytes(account.password.proof, Limits.ProofBytes, "proof")
    return Prepared(account, ServerCrypto.hashSecret(proof), ServerCrypto.hashSecret(recovery))
}

// The same rules for the name of a new account and for a new name of an old one.
internal fun checkUsername(name: String) {
    if (name.length !in Limits.UsernameMin..Limits.UsernameMax) {
        badRequest("A username is ${Limits.UsernameMin} to ${Limits.UsernameMax} characters long.")
    }
    if (!Limits.UsernamePattern.matches(name)) {
        badRequest(
            "A username can only have letters, digits, dots, dashes and underscores, and starts with a letter or digit.",
        )
    }
}

internal fun checkPassword(password: NewPassword) {
    bytes(password.salt, Limits.SaltBytes, "salt")
    bytes(password.proof, Limits.ProofBytes, "proof")
    lockedKey(password.keyByPassword, "password key")
    val kdf = password.kdf
    if (kdf.algorithm != "argon2id") badRequest("Only argon2id is taken.")
    val strong =
        kdf.memoryKib in Kdf.MinMemoryKib..1_048_576 && kdf.iterations in Kdf.MinIterations..20 &&
            kdf.parallelism in 1..8
    if (!strong) badRequest("The Argon2 setting is too weak or too large.")
}

internal fun checkDevice(device: DeviceInfo) {
    if (device.name.isBlank() || device.name.length > Limits.DeviceNameMax) {
        badRequest("A device name is 1 to ${Limits.DeviceNameMax} characters long.")
    }
    if (device.appVersion.isBlank() || device.appVersion.length > Limits.AppVersionMax) {
        badRequest("The app version is missing or too long.")
    }
}

// The bytes of a base64 text that must be exactly this long, such as a proof or a salt.
internal fun bytes(text: String, size: Int, what: String): ByteArray {
    val found = ServerCrypto.fromBase64(text) ?: badRequest("The $what is not base64.")
    if (found.size != size) badRequest("The $what must be $size bytes.")
    return found
}

// Checks a key locked on the device, which the server keeps without being able to open.
internal fun lockedKey(text: String, what: String) {
    val found = ServerCrypto.fromBase64(text) ?: badRequest("The $what is not base64.")
    if (found.isEmpty() || found.size > Limits.LockedKeyMaxBytes) badRequest("The $what has the wrong size.")
}

internal fun taken() = ApiException(HttpStatusCode.Conflict, ErrorCodes.UsernameTaken, "That username is taken.")

internal fun wrongSignIn() =
    ApiException(HttpStatusCode.Unauthorized, ErrorCodes.WrongSignIn, "The username or password is wrong.")

internal fun wrongTwoStep() = ApiException(
    HttpStatusCode.Unauthorized,
    ErrorCodes.WrongTwoStep,
    "That code is wrong or was used already. Wait for the next one.",
)
