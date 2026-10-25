package com.sumizuri.app.data.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

// The mark at the top of a file of settings, and the version of its shape.
private const val Mark = "sumizuriSettings"
private const val Version = 1

// What a file of settings leaves out: where the app was and when it last checked, the lock and its PIN, notes about covers, and what one title chose for itself. These belong to one device or one title, not to the way a person likes the app.
private val LocalPrefixes = listOf("sync.", "session.place", "session.hidden.", "memo.", "security.", "reader.title.", "debug.perfOverlayAt", "appupdate.lastCheck", "appupdate.lastRun")
private val LocalNames = setOf("lastUpdateCheck", "lastSourceCheck", SettingKey.NoticeAsked, SettingKey.DownloadsPaused)

private fun shared(name: String) = name !in LocalNames && LocalPrefixes.none { name.startsWith(it) }

// Every choice of the settings as one JSON text, to keep or to move to another device.
suspend fun SettingsRepository.exportText(): String = buildJsonObject {
    put(Mark, Version)
    put("settings", buildJsonObject { for ((name, value) in everything().toList().sortedBy { it.first }) if (shared(name)) put(name, value) })
}.toString()

// Reads a file of settings and keeps each choice in it, over the one there was. Choices it does not name stay as they are. Gives how many it kept, or null when the text is not a file of settings.
suspend fun SettingsRepository.importText(text: String): Int? {
    val root = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
    if ((root[Mark] as? JsonPrimitive)?.intOrNull == null) return null
    val settings = root["settings"] as? JsonObject ?: return null
    var kept = 0
    for ((name, value) in settings) {
        val saved = (value as? JsonPrimitive)?.contentOrNull ?: continue
        if (!shared(name)) continue
        setText(name, saved)
        kept++
    }
    return kept
}
