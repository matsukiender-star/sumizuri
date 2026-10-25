package com.sumizuri.app.data.migration

import com.sumizuri.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private const val RulesKey = "migrate.rules"
private const val OrderKey = "migrate.order"
private const val OffKey = "migrate.off"

private val Codec = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}

// What the person chose for migration and wants kept: the rules, the order the sources are tried in and the sources that are switched off for it.
class MigrationPrefsRepository(private val settings: SettingsRepository) {
    val rules: Flow<MigrationRules> = settings.text(RulesKey).map { text ->
        text?.let { runCatching { Codec.decodeFromString<MigrationRules>(it) }.getOrNull() } ?: MigrationRules()
    }

    suspend fun setRules(rules: MigrationRules) =
        settings.setText(RulesKey, Codec.encodeToString(MigrationRules.serializer(), rules))

    // The ids of the sources in the order they are tried, and the ids that are left out.
    suspend fun order(): List<String> = settings
        .text(OrderKey)
        .first()
        .orEmpty()
        .split(",")
        .filter { it.isNotEmpty() }

    suspend fun off(): Set<String> = settings
        .text(OffKey)
        .first()
        .orEmpty()
        .split(",")
        .filter { it.isNotEmpty() }
        .toSet()

    suspend fun setOrder(ids: List<String>) = settings.setText(OrderKey, ids.joinToString(","))

    suspend fun setOff(ids: Set<String>) = settings.setText(OffKey, ids.joinToString(","))
}
