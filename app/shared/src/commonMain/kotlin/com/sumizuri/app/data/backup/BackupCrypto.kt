package com.sumizuri.app.data.backup

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.operations.IvAuthenticatedCipher
import okio.Buffer
import okio.BufferedSource
import okio.IOException
import okio.Sink
import okio.Source
import okio.Timeout

// How much plain data each sealed piece holds. The data is sealed piece by piece, so a big backup never has to sit in memory whole.
internal const val CryptoChunk = 1 shl 20

// How hard the password is stretched into a key, the same as the encrypted backups of the Flutter app.
internal const val KeyIterations = 210_000

private const val KeyBits = 256
private const val TagBits = 128

// Makes the key from the password and the salt, and the cipher that seals with it.
@OptIn(DelicateCryptographyApi::class)
internal suspend fun backupCipher(password: String, salt: ByteArray, iterations: Int): IvAuthenticatedCipher {
    val provider = CryptographyProvider.Default
    val key = provider
        .get(
            PBKDF2,
        ).secretDerivation(SHA256, iterations, KeyBits.bits, salt)
        .deriveSecretToByteArray(password.encodeToByteArray())
    return provider
        .get(AES.GCM)
        .keyDecoder()
        .decodeFromByteArray(AES.Key.Format.RAW, key)
        .cipher(TagBits.bits)
}

// The nonce of one piece: the nonce of the file with the number of the piece mixed into its last bytes, so no two pieces share one.
private fun nonceOf(base: ByteArray, index: Long): ByteArray {
    val nonce = base.copyOf()
    for (i in 0 until 8) {
        val at = nonce.size - 1 - i
        nonce[at] = (nonce[at].toInt() xor ((index ushr (8 * i)) and 0xFF).toInt()).toByte()
    }
    return nonce
}

// What each piece is bound to: the header of the file, the number of the piece and whether it is the last. A piece moved, dropped or cut off then fails to open.
private fun boundTo(header: ByteArray, index: Long, last: Boolean): ByteArray {
    val bound = Buffer().write(header).writeLong(index).writeByte(if (last) 1 else 0)
    return bound.readByteArray()
}

// Seals what is written to it piece by piece. Each piece is its length, a mark for the last one, and the sealed bytes. The last piece goes out when it is closed.
@OptIn(DelicateCryptographyApi::class)
internal class SealingSink(
    private val out: Sink,
    private val cipher: IvAuthenticatedCipher,
    private val nonce: ByteArray,
    private val header: ByteArray,
) : Sink {
    private val pending = Buffer()
    private var index = 0L

    override fun write(source: Buffer, byteCount: Long) {
        pending.write(source, byteCount)
        // A piece is only sealed once more data follows it, so the one left at the end can be marked as the last.
        while (pending.size > CryptoChunk) seal(pending.readByteArray(CryptoChunk.toLong()), last = false)
    }

    private fun seal(plain: ByteArray, last: Boolean) {
        val sealed = cipher.encryptWithIvBlocking(nonceOf(nonce, index), plain, boundTo(header, index, last))
        out.write(Buffer().writeInt(sealed.size).writeByte(if (last) 1 else 0).write(sealed), 5L + sealed.size)
        index++
    }

    override fun flush() = out.flush()

    override fun timeout(): Timeout = out.timeout()

    override fun close() {
        seal(pending.readByteArray(), last = true)
        out.close()
    }
}

// Opens the pieces of a SealingSink in order. A wrong password fails at the first piece, and a file cut short or changed fails where it was touched.
@OptIn(DelicateCryptographyApi::class)
internal class OpeningSource(
    private val input: BufferedSource,
    private val cipher: IvAuthenticatedCipher,
    private val nonce: ByteArray,
    private val header: ByteArray,
) : Source {
    private val plain = Buffer()
    private var index = 0L
    private var ended = false

    override fun read(sink: Buffer, byteCount: Long): Long {
        while (plain.size == 0L && !ended) open()
        if (plain.size == 0L) return -1
        return plain.read(sink, byteCount)
    }

    private fun open() {
        if (input.exhausted()) throw IOException("the backup ends before its last piece")
        val size = input.readInt()
        val last = input.readByte().toInt() == 1
        if (size < 0 || size > CryptoChunk + 64) throw IOException("a piece of the backup has a wrong size")
        val sealed = input.readByteArray(size.toLong())
        val opened = try {
            cipher.decryptWithIvBlocking(nonceOf(nonce, index), sealed, boundTo(header, index, last))
        } catch (error: Exception) {
            throw BackupUnreadable(if (index == 0L) BackupProblem.WrongPassword else BackupProblem.Damaged, error)
        }
        plain.write(opened)
        index++
        if (last) {
            ended = true
            if (!input.exhausted()) throw IOException("the backup goes on after its last piece")
        }
    }

    override fun timeout(): Timeout = input.timeout()

    override fun close() = input.close()
}
