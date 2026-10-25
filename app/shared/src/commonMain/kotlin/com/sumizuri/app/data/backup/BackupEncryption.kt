package com.sumizuri.app.data.backup

import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlin.io.encoding.Base64

private const val EnvelopeVersion = 1
private const val KeyBits = 256
private const val TagBits = 128

// True when the text is a backup that is shut with a password, which is a small envelope that says so, and not the backup itself.
fun looksEncrypted(text: String): Boolean {
    val head = text.trimStart()
    if (!head.startsWith("{")) return false
    return try {
        ((Json.parseToJsonElement(text) as? JsonObject)?.get("sumizuriEncryptedBackup") as? JsonPrimitive)?.intOrNull ==
            EnvelopeVersion
    } catch (error: Exception) {
        false
    }
}

// Opens the envelope with the password and gives the backup text. A wrong password and a damaged file both end in the same failure, since the cipher cannot tell them apart.
@OptIn(DelicateCryptographyApi::class)
suspend fun decryptBackup(envelopeText: String, password: String): String {
    try {
        val envelope = Json.parseToJsonElement(envelopeText).jsonObject
        fun field(name: String) =
            Base64.decode(envelope.getValue(name).let { (it as JsonPrimitive).contentOrNull.orEmpty() })
        val iterations = (envelope["iterations"] as? JsonPrimitive)?.intOrNull ?: 210_000
        val provider = CryptographyProvider.Default
        val key = provider
            .get(
                PBKDF2,
            ).secretDerivation(
                SHA256,
                iterations,
                KeyBits.bits,
                field("salt"),
            ).deriveSecretToByteArray(password.encodeToByteArray())
        val cipher = provider
            .get(AES.GCM)
            .keyDecoder()
            .decodeFromByteArray(AES.Key.Format.RAW, key)
            .cipher(TagBits.bits)
        return cipher.decryptWithIv(field("nonce"), field("ciphertext")).decodeToString()
    } catch (error: Exception) {
        if (error is kotlinx.coroutines.CancellationException) throw error
        throw BackupUnreadable(BackupProblem.WrongPassword, error)
    }
}
