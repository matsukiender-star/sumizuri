package com.sumizuri.sync.server.auth

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// The small pieces of cryptography the server needs itself. The library data is never unlocked here.
object ServerCrypto {
    private val random = SecureRandom()

    // Argon2 settings for checking a proof. A proof is already slow to make from a password, so these are the lighter ones OWASP lists.
    private const val MemoryKib = 19456
    private const val Iterations = 2
    private const val Parallelism = 1

    fun randomBytes(count: Int): ByteArray = ByteArray(count).also { random.nextBytes(it) }

    fun base64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    // Reads base64, or nothing when it is not base64.
    fun fromBase64(text: String): ByteArray? = runCatching { Base64.getDecoder().decode(text) }.getOrNull()

    // A token for a device, safe in a header.
    fun newToken(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(32))

    fun sha256Hex(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") {
            "%02x".format(it)
        }

    // A code a person reads and types, such as the setup code and invite codes: three groups of four, with no letters that look alike.
    fun readableCode(): String = (1..3).joinToString("-") {
        (1..4).map { CodeLetters[random.nextInt(CodeLetters.length)] }.joinToString("")
    }

    // What is kept of such a code, ignoring case, spaces and dashes.
    fun codeHash(code: String): String = sha256Hex(code.uppercase().filter { it.isLetterOrDigit() })

    private const val CodeLetters = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    fun hmac(key: ByteArray, text: String): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256"))
        doFinal(text.toByteArray())
    }

    // What the server keeps of a proof: argon2id, its settings, a salt of its own and the result, so the proof itself is never stored.
    fun hashSecret(secret: ByteArray): String {
        val salt = randomBytes(16)
        val hash = argon2(secret, salt, MemoryKib, Iterations, Parallelism)
        return "argon2id\$$MemoryKib\$$Iterations\$$Parallelism\$${base64(salt)}\$${base64(hash)}"
    }

    // Whether a proof matches what was kept, taking the same time whether it does or not.
    fun checkSecret(secret: ByteArray, kept: String): Boolean {
        val parts = kept.split('$')
        if (parts.size != 6 || parts[0] != "argon2id") return false
        val salt = fromBase64(parts[4]) ?: return false
        val hash = fromBase64(parts[5]) ?: return false
        val found = argon2(secret, salt, parts[1].toInt(), parts[2].toInt(), parts[3].toInt(), hash.size)
        return MessageDigest.isEqual(found, hash)
    }

    private fun argon2(
        secret: ByteArray,
        salt: ByteArray,
        memoryKib: Int,
        iterations: Int,
        parallelism: Int,
        length: Int = 32,
    ): ByteArray {
        val parameters = Argon2Parameters
            .Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withSalt(salt)
            .withMemoryAsKB(memoryKib)
            .withIterations(iterations)
            .withParallelism(parallelism)
            .build()
        return ByteArray(
            length,
        ).also { out -> Argon2BytesGenerator().apply { init(parameters) }.generateBytes(secret, out) }
    }
}
