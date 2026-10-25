package com.sumizuri.app.data.backup

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

// Reading the fields of a backup written as JSON, where a missing or odd field reads as empty rather than failing the whole backup.

internal fun JsonObject.list(key: String): List<JsonObject> = (this[key] as? JsonArray).orEmpty().mapNotNull {
    it as? JsonObject
}

internal fun JsonObject.text(key: String): String = (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()

internal fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

internal fun JsonObject.bool(key: String): Boolean = (this[key] as? JsonPrimitive)?.booleanOrNull == true
