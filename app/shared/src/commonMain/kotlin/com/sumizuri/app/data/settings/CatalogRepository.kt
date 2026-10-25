package com.sumizuri.app.data.settings

import com.sumizuri.app.data.db.AppDatabase
import com.sumizuri.app.data.db.setting.SettingEntity
import com.sumizuri.app.data.model.SavedCatalog
import com.sumizuri.app.source.repo.InstalledSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private const val Key = "browse.customCatalogs"

// Names a source the same way after a reinstall, and the same way the Flutter app does.
fun catalogKey(source: InstalledSource): String = "${source.name}|${source.lang}|${source.kind.name.lowercase()}"

// The searches the reader kept as tabs, for each source, in the order they were made. They are stored in the shape the Flutter app uses.
class CatalogRepository(private val db: AppDatabase) {
    fun observe(sourceKey: String): Flow<List<SavedCatalog>> = db
        .settings()
        .observe(Key)
        .distinctUntilChanged()
        .map { decode(it)[sourceKey].orEmpty() }

    // Replaces the tabs of one source.
    suspend fun save(sourceKey: String, catalogs: List<SavedCatalog>) {
        val all = decode(db.settings().get(Key)).toMutableMap()
        if (catalogs.isEmpty()) all.remove(sourceKey) else all[sourceKey] = catalogs
        db.settings().put(SettingEntity(Key, encode(all)))
    }

    private fun decode(text: String?): Map<String, List<SavedCatalog>> {
        if (text.isNullOrBlank()) return emptyMap()
        val root = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return emptyMap()
        return root.mapValues { (_, value) ->
            (value as? JsonArray).orEmpty().mapNotNull { item ->
                val map = item as? JsonObject ?: return@mapNotNull null
                val name = (map["name"] as? JsonPrimitive)?.content ?: return@mapNotNull null
                SavedCatalog(
                    name = name,
                    query = (map["query"] as? JsonPrimitive)?.content.orEmpty(),
                    filters = (map["filters"] as? JsonObject)?.mapValues { (_, filter) -> plain(filter) }.orEmpty(),
                )
            }
        }
    }

    private fun encode(all: Map<String, List<SavedCatalog>>): String = buildJsonObject {
        for ((key, list) in all) {
            put(
                key,
                buildJsonArray {
                    for (catalog in list) {
                        add(
                            buildJsonObject {
                                put("name", catalog.name)
                                if (catalog.query.isNotEmpty()) put("query", catalog.query)
                                if (catalog.filters.isNotEmpty()) {
                                    put(
                                        "filters",
                                        JsonObject(catalog.filters.mapValues { json(it.value) }),
                                    )
                                }
                            },
                        )
                    }
                },
            )
        }
    }.toString()

    // A filter value as plain data, a flag, a list of texts, or a text.
    private fun plain(element: JsonElement): Any? = when (element) {
        is JsonNull -> null
        is JsonArray -> element.map { (it as? JsonPrimitive)?.content.orEmpty() }
        is JsonPrimitive -> if (element.isString) element.content else element.booleanOrNull ?: element.content
        else -> null
    }

    private fun json(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is Boolean -> JsonPrimitive(value)
        is Iterable<*> -> JsonArray(value.map { JsonPrimitive(it.toString()) })
        else -> JsonPrimitive(value.toString())
    }
}
