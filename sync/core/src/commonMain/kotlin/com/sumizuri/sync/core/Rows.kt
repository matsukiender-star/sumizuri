package com.sumizuri.sync.core

import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// A library on the server as rows: one for each title, each chapter with a mark, each setting and each source. Each row is locked on the device, and its id is made from its key with the library's key, so the server can neither read a row nor tell what it is about. Every change to a row gets the next number of the library, and devices fetch the changes after the number they last saw.

// A row as a device sends it: its id and its locked bytes as base64.
@Serializable
data class RowIn(val id: String, val data: String)

// A batch of rows built on the changes up to base. The server turns the whole batch down when another device changed one of these rows after base.
@Serializable
data class RowPush(val base: Long, val rows: List<RowIn>)

// The number of the newest change once the batch is in, and whether the batch came right after base with nothing from other devices in between, so the device can move on to that number without fetching its own rows back.
@Serializable
data class RowPushed(val head: Long, val contiguous: Boolean)

@Serializable
data class RowOut(val id: String, val seq: Long, val data: String)

// A page of the changes after a number, oldest first, the newest number of the library, and whether more pages follow.
@Serializable
data class RowPage(val rows: List<RowOut>, val head: Long, val more: Boolean)

object RowLimits {
    // The most rows in one batch or one page.
    const val PerBatch = 2500

    // The biggest one row may be once locked, which leaves room for the code of a large source.
    const val RowMaxBytes = 2 * 1024 * 1024

    // The length of a row id: 32 bytes as base64 for addresses, with no padding.
    const val IdLength = 43
}

object Rows {
    private const val IdPurpose = "sumizuri row id v1\n"
    private const val LockPurpose = "sumizuri row v1\n"

    // Rows bigger than this are packed with zlib first. Smaller ones would only grow.
    private const val PackFrom = 512

    // The id of the row with this key. The same key gives the same id on every device of the library.
    @OptIn(ExperimentalEncodingApi::class)
    fun idOf(libraryKey: ByteArray, key: String): String =
        Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(
            SyncCrypto.hmac(
                libraryKey,
                (
                    IdPurpose +
                        key
                    ).encodeToByteArray(),
            ),
        )

    // Locks the bytes of a row, bound to its id so the server cannot swap two rows.
    fun lock(libraryKey: ByteArray, id: String, plain: ByteArray): ByteArray {
        val packed = if (plain.size >= PackFrom) byteArrayOf(1) + Snapshot.deflate(plain) else byteArrayOf(0) + plain
        return SyncCrypto.lock(libraryKey, packed, LockPurpose + id)
    }

    // The bytes of a row, or nothing when the key does not fit or the row was changed or moved.
    fun unlock(libraryKey: ByteArray, id: String, locked: ByteArray): ByteArray? {
        val packed = SyncCrypto.unlock(libraryKey, locked, LockPurpose + id) ?: return null
        if (packed.isEmpty()) return null
        val body = packed.copyOfRange(1, packed.size)
        return if (packed[0] == 1.toByte()) runCatching { Snapshot.inflate(body) }.getOrNull() else body
    }

    // Pages and batches go over the wire packed with zlib when the device asks, which wins back most of what base64 and the repeated names add.
    const val PackedHeader = "X-Rows-Packed"

    fun pack(bytes: ByteArray): ByteArray = Snapshot.deflate(bytes)

    // Unpacks at most max bytes, so a small packed batch can never swell past what the server takes.
    fun unpack(bytes: ByteArray, max: Int): ByteArray? = runCatching { zlibInflate(bytes, max) }.getOrNull()

    fun validId(id: String): Boolean =
        id.length == RowLimits.IdLength && id.all { it.isLetterOrDigit() || it == '-' || it == '_' }
}
