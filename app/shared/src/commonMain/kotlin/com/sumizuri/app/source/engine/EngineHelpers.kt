package com.sumizuri.app.source.engine

import com.sumizuri.app.source.SourceError
import com.sumizuri.app.source.host.HostBridge
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// A script's errors arrive as text, so the kind is read from the wording the host and the engine use.
internal fun classifyFailure(method: String, message: String): SourceError = when {
    message.contains("__NOT_IMPLEMENTED__:") -> SourceError.NotImplemented(method)

    message.contains(HostBridge.ChallengeMarker) -> {
        val url = message.substringAfter(HostBridge.ChallengeMarker).substringBefore(' ')
        SourceError.Challenge(url, message.substringAfter(HostBridge.ChallengeMarker))
    }

    message.contains("Refusing to fetch") -> SourceError.Blocked(message)

    message.contains("Could not reach") -> SourceError.Network(message)

    else -> SourceError.Script(message)
}

// A plain Kotlin value as JSON, for arguments and stored values.
internal fun anyToJson(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is Boolean -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is String -> JsonPrimitive(value)
    is Collection<*> -> JsonArray(value.map(::anyToJson))
    is Map<*, *> -> JsonObject(value.entries.associate { it.key.toString() to anyToJson(it.value) })
    else -> JsonPrimitive(value.toString())
}

// A JSON value as a plain Kotlin value: text, number, flag, list or map.
fun primitiveValue(value: JsonElement?): Any? = when (value) {
    null, is JsonNull -> null

    is JsonPrimitive -> when {
        value.isString -> value.content
        value.content == "true" -> true
        value.content == "false" -> false
        else -> value.content.toLongOrNull() ?: value.content.toDoubleOrNull() ?: value.content
    }

    is JsonArray -> value.map { primitiveValue(it) }

    is JsonObject -> value.mapValues { primitiveValue(it.value) }
}
