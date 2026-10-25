package com.sumizuri.sync.core

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// The two keys made from a password or a recovery key: the proof the server checks, and the key that unlocks the account key, which never leaves the device.
class SecretKeys(val proof: ByteArray, val unlock: ByteArray)

// The cryptography of an account, the same on every device. The account key is locked with AES-256-GCM, and a password is stretched with Argon2id before it is split in two with HKDF.
object SyncCrypto {
    private const val NonceBytes = 12

    // What a locked account key is bound to, so a locked piece of something else can never be opened as one.
    const val AccountKeyPurpose = "sumizuri account key v1"

    fun randomBytes(count: Int): ByteArray = secureRandomBytes(count)

    @OptIn(ExperimentalEncodingApi::class)
    fun base64(bytes: ByteArray): String = Base64.Default.encode(bytes)

    @OptIn(ExperimentalEncodingApi::class)
    fun fromBase64(text: String): ByteArray = Base64.Default.decode(text)

    // The proof and unlock key of a password. This takes about a second on a phone, on purpose.
    fun passwordKeys(password: String, salt: ByteArray, kdf: Kdf): SecretKeys {
        val stretched = argon2id(password.encodeToByteArray(), salt, kdf)
        return SecretKeys(hkdf(stretched, "sumizuri password proof v1"), hkdf(stretched, "sumizuri password unlock v1"))
    }

    // The proof and unlock key of a recovery key. It is already random, so it needs no stretching.
    fun recoveryKeys(recovery: ByteArray): SecretKeys =
        SecretKeys(hkdf(recovery, "sumizuri recovery proof v1"), hkdf(recovery, "sumizuri recovery unlock v1"))

    // Locks with a fresh nonce, which goes in front of what is locked.
    fun lock(key: ByteArray, plain: ByteArray, purpose: String): ByteArray {
        val nonce = randomBytes(NonceBytes)
        return nonce + aesGcmSeal(key, nonce, plain, purpose.encodeToByteArray())
    }

    // Unlocks, or gives nothing when the key is wrong or the bytes were changed.
    fun unlock(key: ByteArray, locked: ByteArray, purpose: String): ByteArray? {
        if (locked.size <= NonceBytes) return null
        return runCatching {
            aesGcmOpen(
                key,
                locked.copyOfRange(0, NonceBytes),
                locked.copyOfRange(NonceBytes, locked.size),
                purpose.encodeToByteArray(),
            )
        }.getOrNull()
    }

    // HKDF with SHA-256, one block of 32 bytes, with no salt as the input is already strong.
    private fun hkdf(input: ByteArray, info: String): ByteArray {
        val extract = hmac(ByteArray(32), input)
        return hmac(extract, info.encodeToByteArray() + byteArrayOf(1))
    }

    fun hmac(key: ByteArray, data: ByteArray): ByteArray = hmacSha256(key, data)
}
