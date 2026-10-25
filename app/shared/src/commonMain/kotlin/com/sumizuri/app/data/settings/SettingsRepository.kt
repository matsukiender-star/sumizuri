package com.sumizuri.app.data.settings

import com.sumizuri.app.data.db.AppDatabase
import com.sumizuri.app.data.db.setting.SettingEntity
import com.sumizuri.app.data.text.enumNamed
import com.sumizuri.app.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

private const val ThemeKey = "themeMode"
private const val UpdateCheckKey = "lastUpdateCheck"
private const val SourceCheckKey = "lastSourceCheck"

class SettingsRepository(private val db: AppDatabase, scope: CoroutineScope? = null) {
    // All the settings read in one query and kept for every screen, so writing one setting asks the database once and not once for every setting that is watched. Each setting then passes on only a change of its own value.
    private val all: Flow<Map<String, String>> = db
        .settings()
        .observeAll()
        .map { rows -> rows.associate { it.name to it.value } }
        .let { if (scope == null) it else it.shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1) }

    private fun value(key: String): Flow<String?> = if (key.startsWith("memo.")) {
        db.settings().observe(key)
    } else {
        all
            .map {
                it[key]
            }.distinctUntilChanged()
    }

    // Every setting whose name starts with the prefix, for a group of choices read together.
    fun prefixed(prefix: String): Flow<Map<String, String>> =
        all.map { saved -> saved.filterKeys { it.startsWith(prefix) } }.distinctUntilChanged()

    val themeMode: Flow<ThemeMode> = value(ThemeKey).map { saved ->
        enumNamed<ThemeMode>(saved) ?: ThemeMode.System
    }

    // When the library was last checked for new chapters, in epoch milliseconds, or 0 when it never was.
    suspend fun lastUpdateCheck(): Long = db.settings().get(UpdateCheckKey)?.toLongOrNull() ?: 0L

    suspend fun setLastUpdateCheck(time: Long) = db.settings().put(SettingEntity(UpdateCheckKey, time.toString()))

    // When the repos were last asked for newer versions of the installed sources, or 0 when they never were.
    suspend fun lastSourceCheck(): Long = db.settings().get(SourceCheckKey)?.toLongOrNull() ?: 0L

    suspend fun setLastSourceCheck(time: Long) = db.settings().put(SettingEntity(SourceCheckKey, time.toString()))

    suspend fun setThemeMode(mode: ThemeMode) = db.settings().put(SettingEntity(ThemeKey, mode.name))

    // Takes back every choice whose name starts with the prefix, except those that start with the other one, so they have their usual values again.
    suspend fun reset(prefix: String, except: String = "~none~") = db.settings().deleteWithPrefix(prefix, except)

    // Takes the look of the app back: the theme in use, dark mode and the black backgrounds. The themes a person made stay, only the choice of the one in use goes.
    suspend fun resetLook() {
        resetNames(ThemeKey, SettingKey.Oled, SettingKey.OledGlow)
        reset("theme.", except = "theme.custom.")
    }

    // Takes the interface back: the language, the lists, the navigation, the motion and where sites open.
    suspend fun resetInterface() {
        reset("layout.")
        resetNames(SettingKey.Language, SettingKey.WebInApp)
    }

    // Takes back the choices with these exact names.
    suspend fun resetNames(vararg names: String) {
        for (name in names) db.settings().delete(name)
    }

    // A yes or no choice of the settings pages, which has the fallback until it is made.
    fun flag(key: String, fallback: Boolean): Flow<Boolean> = value(key).map { it?.toBooleanStrictOrNull() ?: fallback }

    suspend fun setFlag(key: String, on: Boolean) = db.settings().put(SettingEntity(key, on.toString()))

    // A number choice of the settings pages.
    fun number(key: String, fallback: Int): Flow<Int> = value(key).map { it?.toIntOrNull() ?: fallback }

    suspend fun setNumber(key: String, value: Int) = db.settings().put(SettingEntity(key, value.toString()))

    // A choice of the settings pages that is a name, or null while none was made.
    fun text(key: String): Flow<String?> = value(key)

    // The settings whose names start with the prefix, read once.
    suspend fun withPrefix(prefix: String): Map<String, String> = db
        .settings()
        .observeWithPrefix(prefix)
        .first()
        .associate { it.name to it.value }

    // Every setting as it is saved, read once.
    suspend fun everything(): Map<String, String> = db.settings().everything().associate { it.name to it.value }

    // The same read once, for what only needs the value now.
    suspend fun textNow(key: String): String? = db.settings().get(key)

    suspend fun setText(key: String, value: String) = db.settings().put(SettingEntity(key, value))
}
