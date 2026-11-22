package com.sumizuri.sync.core

import kotlinx.serialization.Serializable

// A signed in device offers its account to one new device: the server gets the id and a locked key, and only the QR code holds the secret that opens it.
@Serializable
data class PairOffer(val id: String, val sealedKey: String)

// Until when the offer can be taken, in the clock of the server.
@Serializable
data class PairOffered(val expiresAt: Long)

// What the new device sends after reading the code.
@Serializable
data class PairClaim(val id: String, val device: DeviceInfo)

// The sign in of the new device, with the account key only the secret of the code opens.
@Serializable
data class PairJoined(val signed: SignedIn, val sealedKey: String)

// The secret of a code and the two things made from it, so the server can match a claim without being able to open the key.
object Pairing {
    const val SecretBytes = 32
    const val IdBytes = 32
    const val OfferMs = 120_000L

    private const val Purpose = "sumizuri pair key"

    fun newSecret(): ByteArray = SyncCrypto.randomBytes(SecretBytes)

    fun idOf(secret: ByteArray): String =
        SyncCrypto.base64(SyncCrypto.hmac(secret, "sumizuri pair id".encodeToByteArray()))

    fun offer(secret: ByteArray, accountKey: ByteArray): PairOffer =
        PairOffer(idOf(secret), SyncCrypto.base64(SyncCrypto.lock(keyOf(secret), accountKey, Purpose)))

    // The account key, or nothing for another secret or changed bytes.
    fun open(secret: ByteArray, sealedKey: String): ByteArray? = runCatching {
        SyncCrypto.unlock(keyOf(secret), SyncCrypto.fromBase64(sealedKey), Purpose)
    }.getOrNull()

    private fun keyOf(secret: ByteArray): ByteArray = SyncCrypto.hmac(secret, Purpose.encodeToByteArray())
}
