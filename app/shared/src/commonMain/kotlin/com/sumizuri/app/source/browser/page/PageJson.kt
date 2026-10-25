package com.sumizuri.app.source.browser.page

import com.sumizuri.app.source.host.BridgeException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// An object of text values, such as headers, with the values that are not text left out.
internal fun JsonObject.stringMap(key: String): Map<String, String> = (this[key] as? JsonObject)
    ?.mapNotNull { (name, value) ->
        (value as? JsonPrimitive)?.takeIf { it.isString }?.let {
            name to
                it.content
        }
    }.orEmpty()
    .toMap()

internal fun JsonObject.flag(key: String): Boolean = (this[key] as? JsonPrimitive)?.content == "true"

internal fun JsonObject.strings(key: String): List<String> = (this[key] as? JsonArray).orEmpty().mapNotNull {
    (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content
}

// Text as a quoted string that is safe to place inside a script.
internal fun quoteForScript(text: String): String = JsonPrimitive(text).toString()

// A page answers a script with base64 of JSON, which the platform may wrap in quotes.
@OptIn(ExperimentalEncodingApi::class)
internal fun decodePageAnswer(raw: String): JsonObject {
    val clean = raw.replace("\"", "").replace("\\", "").filterNot { it.isWhitespace() }
    if (clean.isEmpty() || clean == "null") throw BridgeException("The page gave no answer, it may have been closed")
    return try {
        val padded = clean.padEnd((clean.length + 3) / 4 * 4, '=')
        Json.parseToJsonElement(Base64.Default.decode(padded).decodeToString()).jsonObject
    } catch (error: IllegalArgumentException) {
        throw BridgeException("The page gave an answer that could not be read: ${error.message}")
    }
}

// The browser could not be started on this device.
class BrowserUnavailableException(message: String) : Exception(message)

// The page could not be loaded. A first failure is tried once more.
class PageLoadException(message: String) : Exception(message)
