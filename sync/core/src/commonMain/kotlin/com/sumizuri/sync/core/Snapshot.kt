package com.sumizuri.sync.core

// A snapshot is a full copy of one library: the same JSON as a Sumizuri backup, packed with zlib and locked with the library's key, so the server only ever holds locked bytes.
object Snapshot {
    private const val Purpose = "sumizuri snapshot v1"

    fun pack(json: ByteArray, libraryKey: ByteArray): ByteArray = SyncCrypto.lock(libraryKey, deflate(json), Purpose)

    // The JSON of a snapshot, or nothing when the key does not fit or the bytes were changed.
    fun unpack(locked: ByteArray, libraryKey: ByteArray): ByteArray? {
        val packed = SyncCrypto.unlock(libraryKey, locked, Purpose) ?: return null
        return runCatching { inflate(packed) }.getOrNull()
    }

    internal fun deflate(data: ByteArray): ByteArray = zlibDeflate(data)

    internal fun inflate(data: ByteArray): ByteArray = zlibInflate(data, Int.MAX_VALUE)
}
