package com.sumizuri.app.source.host

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

// Answers the calls an extension makes through sendMessage, always as JSON text with an error field on failure.
class HostBridge(private val handlers: Map<String, BridgeHandler>) {
    suspend fun call(name: String, payload: String): String {
        val handler = handlers[name] ?: return errorJson("The host does not support \"$name\" yet.")
        return try {
            val arguments = if (payload.isBlank()) {
                JsonObject(
                    emptyMap(),
                )
            } else {
                Json.parseToJsonElement(payload).jsonObject
            }
            handler(arguments).toString()
        } catch (error: ChallengeException) {
            errorJson("$ChallengeMarker${error.url} (${error.message})")
        } catch (error: BridgeException) {
            errorJson(error.message ?: "The host call failed.")
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            errorJson(error.message ?: error.toString())
        }
    }

    private fun errorJson(message: String): String = buildJsonObject { put("error", message) }.toString()

    companion object {
        // The text a script's error starts with when a bot check stopped a request.
        const val ChallengeMarker = "__CHALLENGE__:"
    }
}

typealias BridgeHandler = suspend (JsonObject) -> JsonElement

// A bridge call that failed on purpose. The extension sees its text as a thrown error.
open class BridgeException(message: String) : Exception(message)

// The host refused the call, for example because the address is private.
class BlockedException(message: String) : BridgeException(message)

// A bot check answered instead of the page.
class ChallengeException(val url: String, message: String) : BridgeException(message)

internal fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.content?.toBooleanStrictOrNull()

internal fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.content?.toDoubleOrNull()?.toInt()

internal fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
