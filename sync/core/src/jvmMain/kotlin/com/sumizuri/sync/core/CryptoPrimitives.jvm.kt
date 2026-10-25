package com.sumizuri.sync.core

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TagBits = 128

private val random = SecureRandom()

internal actual fun secureRandomBytes(count: Int): ByteArray = ByteArray(count).also { random.nextBytes(it) }

internal actual fun aesGcmSeal(key: ByteArray, nonce: ByteArray, plain: ByteArray, bound: ByteArray): ByteArray {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TagBits, nonce))
    cipher.updateAAD(bound)
    return cipher.doFinal(plain)
}

internal actual fun aesGcmOpen(key: ByteArray, nonce: ByteArray, sealed: ByteArray, bound: ByteArray): ByteArray? =
    runCatching {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TagBits, nonce))
        cipher.updateAAD(bound)
        cipher.doFinal(sealed)
    }.getOrNull()

internal actual fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray = Mac.getInstance("HmacSHA256").run {
    init(SecretKeySpec(key, "HmacSHA256"))
    doFinal(data)
}

internal actual fun argon2id(secret: ByteArray, salt: ByteArray, kdf: Kdf): ByteArray {
    val parameters = Argon2Parameters
        .Builder(Argon2Parameters.ARGON2_id)
        .withVersion(Argon2Parameters.ARGON2_VERSION_13)
        .withSalt(salt)
        .withMemoryAsKB(kdf.memoryKib)
        .withIterations(kdf.iterations)
        .withParallelism(kdf.parallelism)
        .build()
    return ByteArray(32).also { out -> Argon2BytesGenerator().apply { init(parameters) }.generateBytes(secret, out) }
}

internal actual fun zlibDeflate(data: ByteArray): ByteArray {
    val deflater = Deflater(Deflater.BEST_COMPRESSION)
    deflater.setInput(data)
    deflater.finish()
    val out = ByteArrayOutputStream(data.size / 4 + 64)
    val buffer = ByteArray(64 * 1024)
    while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
    deflater.end()
    return out.toByteArray()
}

internal actual fun zlibInflate(data: ByteArray, max: Int): ByteArray {
    val inflater = Inflater()
    inflater.setInput(data)
    val out = ByteArrayOutputStream(data.size * 3)
    val buffer = ByteArray(64 * 1024)
    while (!inflater.finished()) {
        val count = inflater.inflate(buffer)
        if (count == 0 && (inflater.needsInput() || inflater.needsDictionary())) error("The packed bytes ended early.")
        out.write(buffer, 0, count)
        if (out.size() > max) error("The packed bytes are too big.")
    }
    inflater.end()
    return out.toByteArray()
}
