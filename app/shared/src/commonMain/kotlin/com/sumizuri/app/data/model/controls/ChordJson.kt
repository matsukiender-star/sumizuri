package com.sumizuri.app.data.model.controls

import com.sumizuri.app.ui.controls.KeyChord
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

// The controls as text, for the database and for a file the person keeps. Reading is forgiving: what is missing or not known keeps its usual value.
internal fun chordToJson(chord: KeyChord): JsonObject = buildJsonObject {
    put("key", chord.key)
    if (chord.ctrl) put("ctrl", true)
    if (chord.shift) put("shift", true)
    if (chord.alt) put("alt", true)
    if (chord.meta) put("meta", true)
}

internal fun chordOf(element: JsonElement): KeyChord? {
    val obj = element as? JsonObject ?: return null
    val key = (obj["key"] as? JsonPrimitive)?.contentOrNull ?: return null
    fun flag(name: String) = (obj[name] as? JsonPrimitive)?.booleanOrNull == true
    return KeyChord(key, flag("ctrl"), flag("shift"), flag("alt"), flag("meta"))
}

internal fun <T : Enum<T>> keysToJson(keys: Map<T, List<KeyChord>>): JsonObject = buildJsonObject {
    for ((action, chords) in keys) put(action.name, buildJsonArray { chords.forEach { add(chordToJson(it)) } })
}

// Reads the keys of every action in the list. A chord already given to an earlier action is dropped, so no key does two things.
internal fun <T : Enum<T>> keysOf(
    root: JsonElement?,
    all: List<T>,
    defaults: Map<T, List<KeyChord>>,
): Map<T, List<KeyChord>> {
    val obj = root as? JsonObject ?: return defaults
    val claimed = HashSet<KeyChord>()
    val result = defaults.toMutableMap()
    for (action in all) {
        val list = obj[action.name] as? JsonArray ?: continue
        result[action] = list.mapNotNull(::chordOf).filter { claimed.add(it) }
    }
    return result
}

internal fun <T : Enum<T>> enumOf(all: List<T>, element: JsonElement?, fallback: T): T =
    all.firstOrNull { it.name == (element as? JsonPrimitive)?.contentOrNull } ?: fallback
