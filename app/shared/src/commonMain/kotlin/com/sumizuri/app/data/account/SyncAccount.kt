package com.sumizuri.app.data.account

import com.sumizuri.sync.core.Role
import com.sumizuri.sync.core.SyncCrypto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okio.FileSystem
import okio.Path

// The sync account this app is signed in to: the server, who is signed in, this device's token, and the open account key that unlocks the synced data.
@Serializable
data class SyncAccount(
    val server: String,
    val serverName: String,
    val userId: Long,
    val username: String,
    val role: Role,
    val deviceId: Long,
    val token: String,
    val accountKey: String,
)

// The field of a sealed account file.
private const val SealedField = "sealed"

// Keeps the account in a small file in the app's own folder. Where the system can, the file holds the account sealed with a key of the system, so the token and the account key are not plain text. An older plain file still opens and is sealed at once. It is written to a second file first and moved over, so a crash never leaves half a file.
class SyncAccountStore(private val file: Path, private val system: FileSystem = FileSystem.SYSTEM) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutable = MutableStateFlow(load())
    val account: StateFlow<SyncAccount?> = mutable

    private fun load(): SyncAccount? = runCatching {
        if (!system.exists(file)) return null
        val text = system.read(file) { readUtf8() }
        val sealed = (json.parseToJsonElement(text) as? JsonObject)?.get(SealedField)?.jsonPrimitive?.content
        if (sealed != null) {
            // A seal this system cannot open, such as after the keys of the system were reset, counts as signed out.
            val opened = DeviceSecret.open(SyncCrypto.fromBase64(sealed)) ?: return null
            json.decodeFromString(SyncAccount.serializer(), opened.decodeToString())
        } else {
            json.decodeFromString(SyncAccount.serializer(), text).also { write(it) }
        }
    }.getOrNull()

    fun save(account: SyncAccount) {
        write(account)
        mutable.value = account
    }

    private fun write(account: SyncAccount) {
        val plain = json.encodeToString(SyncAccount.serializer(), account)
        val sealed = DeviceSecret.seal(plain.encodeToByteArray())
        val text = if (sealed ==
            null
        ) {
            plain
        } else {
            buildJsonObject { put(SealedField, SyncCrypto.base64(sealed)) }.toString()
        }
        val temporary = file.parent!! / "${file.name}.tmp"
        system.write(temporary) { writeUtf8(text) }
        DeviceSecret.keepPrivate(temporary.toString())
        system.atomicMove(temporary, file)
    }

    fun clear() {
        system.delete(file, mustExist = false)
        mutable.value = null
    }
}
