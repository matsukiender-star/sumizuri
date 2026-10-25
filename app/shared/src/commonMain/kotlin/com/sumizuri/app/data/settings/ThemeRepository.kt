package com.sumizuri.app.data.settings

import com.sumizuri.app.data.db.AppDatabase
import com.sumizuri.app.data.db.setting.SettingEntity
import com.sumizuri.app.navigation.AppLinks
import com.sumizuri.app.ui.theme.spec.AppThemeSpec
import com.sumizuri.app.ui.theme.spec.ThemePresets
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val ActiveKey = "theme.active"
private const val CustomPrefix = "theme.custom."

// The same reading of a theme everywhere, which lets a file written by a newer version keep working and fills in what an older one lacks.
private val ThemeJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
    prettyPrint = true
}

// The theme in use and the themes a person made, which are kept as one setting row each.
class ThemeRepository(private val db: AppDatabase) {
    // The themes a person made, by name.
    val custom: Flow<List<AppThemeSpec>> = db
        .settings()
        .observeWithPrefix(
            CustomPrefix,
        ).distinctUntilChanged()
        .map { rows ->
            rows
                .mapNotNull {
                    runCatching { ThemeJson.decodeFromString<AppThemeSpec>(it.value) }.getOrNull()
                }.sortedBy { it.name.lowercase() }
        }

    // The theme in use. One that is gone, or never chosen, is the first built in theme.
    val active: Flow<AppThemeSpec> =
        combine(db.settings().observe(ActiveKey).distinctUntilChanged(), custom) { id, mine ->
            (ThemePresets + mine).firstOrNull { it.id == id } ?: ThemePresets.first()
        }

    suspend fun select(id: String) = db.settings().put(SettingEntity(ActiveKey, id))

    // Keeps a theme the person made, or changes it when the id is already there.
    suspend fun save(theme: AppThemeSpec) = db.settings().put(
        SettingEntity(CustomPrefix + theme.id, ThemeJson.encodeToString(AppThemeSpec.serializer(), theme)),
    )

    suspend fun delete(id: String) = db.settings().delete(CustomPrefix + id)

    // The text of a theme as a file, and a theme read from such a text, or null when it cannot be read.
    fun toText(theme: AppThemeSpec): String = ThemeJson.encodeToString(AppThemeSpec.serializer(), theme)

    fun fromText(text: String): AppThemeSpec? =
        runCatching { ThemeJson.decodeFromString<AppThemeSpec>(text) }.getOrNull()

    // A link that carries the theme, to send to someone. Only what differs from the values every theme starts with is in it, so the link stays short.
    @OptIn(ExperimentalEncodingApi::class)
    fun toLink(theme: AppThemeSpec): String {
        val json = LinkJson.encodeToString(AppThemeSpec.serializer(), theme)
        return "${AppLinks.Scheme}://theme?data=" + LinkBase64.encode(json.encodeToByteArray())
    }

    // The theme a link carries, or null when the link holds none.
    @OptIn(ExperimentalEncodingApi::class)
    fun fromLink(data: String): AppThemeSpec? = runCatching {
        LinkJson.decodeFromString<AppThemeSpec>(LinkBase64.decode(data).decodeToString())
    }.getOrNull()
}

// Themes in links leave out the values every theme starts with.
private val LinkJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
    coerceInputValues = true
}

// The letters of a link, with no padding at the end, which links do not like.
@OptIn(ExperimentalEncodingApi::class)
private val LinkBase64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
