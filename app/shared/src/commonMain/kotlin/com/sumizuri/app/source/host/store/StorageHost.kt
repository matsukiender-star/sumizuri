package com.sumizuri.app.source.host.store

import com.sumizuri.app.source.host.BridgeException
import com.sumizuri.app.source.host.string
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// A small key and value store for one source, and the source's preference values. Both are kept in the value store.
class StorageHost(private val scope: String, private val store: ValueStore) {

    suspend fun get(arguments: JsonObject): JsonElement = buildJsonObject {
        put("value", read(ValueArea.Storage, key(arguments)))
    }

    suspend fun set(arguments: JsonObject): JsonElement {
        write(ValueArea.Storage, key(arguments), arguments["value"] ?: JsonNull)
        return ok()
    }

    suspend fun delete(arguments: JsonObject): JsonElement {
        store.delete(scope, ValueArea.Storage, key(arguments))
        return ok()
    }

    suspend fun clear(): JsonElement {
        store.clear(scope, ValueArea.Storage)
        return ok()
    }

    suspend fun all(): JsonElement = buildJsonObject { put("values", JsonObject(readAll(ValueArea.Storage))) }

    suspend fun preference(arguments: JsonObject): JsonElement = buildJsonObject {
        put("value", read(ValueArea.Preference, key(arguments)))
    }

    suspend fun setPreference(arguments: JsonObject): JsonElement {
        write(ValueArea.Preference, key(arguments), arguments["value"] ?: JsonNull)
        return ok()
    }

    suspend fun preferences(): JsonElement =
        buildJsonObject { put("values", JsonObject(readAll(ValueArea.Preference))) }

    // The preference values, as JSON.
    suspend fun preferenceMap(): Map<String, JsonElement> = readAll(ValueArea.Preference)

    suspend fun putPreference(key: String, value: JsonElement) = write(ValueArea.Preference, key, value)

    private suspend fun read(area: ValueArea, key: String): JsonElement =
        store.get(scope, area, key)?.let { Json.parseToJsonElement(it) } ?: JsonNull

    private suspend fun readAll(area: ValueArea): Map<String, JsonElement> = store.all(scope, area).mapValues {
        Json.parseToJsonElement(it.value)
    }

    private suspend fun write(area: ValueArea, key: String, value: JsonElement) =
        store.set(scope, area, key, value.toString())

    private fun ok(): JsonElement = buildJsonObject { put("ok", true) }

    private fun key(arguments: JsonObject): String =
        arguments.string("key") ?: throw BridgeException("This call needs a key")
}
