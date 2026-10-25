@file:OptIn(ExperimentalForeignApi::class, DelicateCryptographyApi::class)

package com.sumizuri.sync.core

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.HMAC
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import platform.zlib.Z_BUF_ERROR
import platform.zlib.Z_OK
import platform.zlib.compress2
import platform.zlib.compressBound
import platform.zlib.uncompress

private const val TagBits = 128
private const val BestCompression = 9

internal actual fun secureRandomBytes(count: Int): ByteArray {
    val bytes = ByteArray(count)
    if (count > 0) {
        val status = bytes.usePinned { SecRandomCopyBytes(kSecRandomDefault, count.convert(), it.addressOf(0)) }
        check(status == 0) { "The system gave no random bytes." }
    }
    return bytes
}

private fun gcm(key: ByteArray) = CryptographyProvider.Default
    .get(AES.GCM)
    .keyDecoder()
    .decodeFromByteArrayBlocking(AES.Key.Format.RAW, key)
    .cipher(TagBits.bits)

internal actual fun aesGcmSeal(key: ByteArray, nonce: ByteArray, plain: ByteArray, bound: ByteArray): ByteArray =
    gcm(key).encryptWithIvBlocking(nonce, plain, bound)

internal actual fun aesGcmOpen(key: ByteArray, nonce: ByteArray, sealed: ByteArray, bound: ByteArray): ByteArray? =
    runCatching {
        gcm(key).decryptWithIvBlocking(nonce, sealed, bound)
    }.getOrNull()

internal actual fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray = CryptographyProvider.Default
    .get(HMAC)
    .keyDecoder(SHA256)
    .decodeFromByteArrayBlocking(HMAC.Key.Format.RAW, key)
    .signatureGenerator()
    .generateSignatureBlocking(data)

internal actual fun argon2id(secret: ByteArray, salt: ByteArray, kdf: Kdf): ByteArray =
    Argon2id.hash(secret, salt, kdf.memoryKib, kdf.iterations, kdf.parallelism)

// The zlib of the system itself, so the stream is the same as the one the Java classes make and read.
internal actual fun zlibDeflate(data: ByteArray): ByteArray = memScoped {
    val room = compressBound(data.size.convert())
    val out = ByteArray(room.toInt())
    val written = alloc<ULongVar>()
    written.value = room
    // An empty array has no first element to point at, so a one byte array stands in, and the length given is still zero.
    val source = if (data.isEmpty()) ByteArray(1) else data
    val status = source.usePinned { from ->
        out.usePinned { to ->
            compress2(
                to.addressOf(0).reinterpret<UByteVar>(),
                written.ptr,
                from.addressOf(0).reinterpret<UByteVar>(),
                data.size.convert(),
                BestCompression,
            )
        }
    }
    check(status == Z_OK) { "The bytes could not be packed." }
    out.copyOf(written.value.toInt())
}

internal actual fun zlibInflate(data: ByteArray, max: Int): ByteArray = memScoped {
    val source = if (data.isEmpty()) ByteArray(1) else data
    val limit = minOf(max.toLong(), Int.MAX_VALUE - 1L)
    // The size of the result is not known, so the room doubles until it fits.
    var room = minOf(maxOf(data.size.toLong() * 4, 1024L), limit + 1)
    var result: ByteArray? = null
    while (result == null) {
        val out = ByteArray(room.toInt())
        val written = alloc<ULongVar>()
        written.value = room.convert()
        val status = source.usePinned { from ->
            out.usePinned { to ->
                uncompress(
                    to.addressOf(0).reinterpret<UByteVar>(),
                    written.ptr,
                    from.addressOf(0).reinterpret<UByteVar>(),
                    data.size.convert(),
                )
            }
        }
        if (status == Z_OK) {
            val size = written.value.toLong()
            if (size > limit) error("The packed bytes are too big.")
            result = out.copyOf(size.toInt())
        } else {
            if (status != Z_BUF_ERROR) error("The packed bytes are not valid.")
            if (room > limit) error("The packed bytes are too big.")
            room = minOf(room * 2, limit + 1)
        }
    }
    result
}
