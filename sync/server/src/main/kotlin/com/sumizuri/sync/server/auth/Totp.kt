package com.sumizuri.sync.server.auth

import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// The six digit codes of an authenticator app, as RFC 6238 lays them out: HMAC-SHA1 of the count of 30 second steps.
object Totp {
    private const val StepMs = 30_000L
    private const val Digits = 6
    private const val SecretBytes = 20

    // A code from the step before or after still counts, for a phone clock that is a little off.
    private const val Drift = 1
    private const val Base32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    // A new secret as base32, the way authenticator apps take it.
    fun newSecret(): String = encode(ServerCrypto.randomBytes(SecretBytes))

    // The link a QR code carries, which an authenticator app reads to add the account.
    fun link(secret: String, issuer: String, username: String): String {
        fun part(text: String) = URLEncoder.encode(text, Charsets.UTF_8).replace("+", "%20")
        return "otpauth://totp/${part(
            issuer,
        )}:${part(
            username,
        )}?secret=$secret&issuer=${part(issuer)}&algorithm=SHA1&digits=$Digits&period=${StepMs / 1000}"
    }

    // The step the code belongs to when it is right, newer than the step used last, or null. Spaces in the code are skipped.
    fun step(secret: String, code: String, usedStep: Long, now: Long = System.currentTimeMillis()): Long? {
        val typed = code.filter { !it.isWhitespace() }
        if (typed.length != Digits || !typed.all { it.isDigit() }) return null
        val key = decode(secret) ?: return null
        val current = now / StepMs
        return (current - Drift..current + Drift).firstOrNull { step ->
            step > usedStep &&
                same(codeAt(key, step), typed)
        }
    }

    private fun codeAt(key: ByteArray, step: Long): String {
        val counter = ByteArray(8) { (step ushr (56 - it * 8)).toByte() }
        val hash = Mac.getInstance("HmacSHA1").run {
            init(SecretKeySpec(key, "HmacSHA1"))
            doFinal(counter)
        }
        val offset = hash.last().toInt() and 0x0f
        val number =
            ((hash[offset].toInt() and 0x7f) shl 24) or ((hash[offset + 1].toInt() and 0xff) shl 16) or
                ((hash[offset + 2].toInt() and 0xff) shl 8) or
                (hash[offset + 3].toInt() and 0xff)
        return (number % 1_000_000).toString().padStart(Digits, '0')
    }

    // Compares the codes in the same time whether they match or not.
    private fun same(a: String, b: String): Boolean =
        java.security.MessageDigest.isEqual(a.toByteArray(), b.toByteArray())

    private fun encode(bytes: ByteArray): String {
        val out = StringBuilder()
        var buffer = 0
        var bits = 0
        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xff)
            bits += 8
            while (bits >= 5) {
                out.append(Base32[(buffer shr (bits - 5)) and 31])
                bits -= 5
            }
        }
        if (bits > 0) out.append(Base32[(buffer shl (5 - bits)) and 31])
        return out.toString()
    }

    private fun decode(text: String): ByteArray? {
        val out = java.io.ByteArrayOutputStream()
        var buffer = 0
        var bits = 0
        for (char in text.uppercase().trimEnd('=')) {
            val value = Base32.indexOf(char).takeIf { it >= 0 } ?: return null
            buffer = (buffer shl 5) or value
            bits += 5
            if (bits >= 8) {
                out.write((buffer shr (bits - 8)) and 0xff)
                bits -= 8
            }
        }
        return out.toByteArray()
    }
}
