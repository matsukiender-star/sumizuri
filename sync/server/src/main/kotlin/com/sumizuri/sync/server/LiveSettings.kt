package com.sumizuri.sync.server

import com.sumizuri.sync.core.ServerSettingEntry
import com.sumizuri.sync.core.ServerSettings
import com.sumizuri.sync.core.SignupMode
import com.sumizuri.sync.server.db.ServerState
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.concurrent.ConcurrentHashMap

// The live settings of the server as they are now: what the .env pins, else what the owner or an admin saved, else the default. Saved values are kept in memory, so reading one costs nothing.
class LiveSettings(private val db: Database, private val pinned: Map<String, String>) {
    private val saved = ConcurrentHashMap<String, String>()

    init {
        transaction(db) {
            ServerState.selectAll().where { ServerState.key like "$Prefix%" }.forEach {
                saved[it[ServerState.key].removePrefix(Prefix)] =
                    it[ServerState.value]
            }
        }
    }

    fun text(key: String): String = pinned[key] ?: saved[key] ?: ServerSettings.definition(key)?.default.orEmpty()

    fun number(key: String): Int =
        text(key).toIntOrNull() ?: ServerSettings.definition(key)?.default?.toIntOrNull() ?: 0

    val name: String get() = text(ServerSettings.Name)

    val signupMode: SignupMode get() = SignupMode.entries.firstOrNull {
        it.name.equals(text(ServerSettings.SignupMode), ignoreCase = true)
    }
        ?: SignupMode.Invite

    // Every setting with its value, default, limits and whether it is pinned, for the page of server settings.
    fun entries(): List<ServerSettingEntry> = ServerSettings.all.map {
        ServerSettingEntry(it.key, it.kind, text(it.key), it.default, it.key in pinned, it.min, it.max, it.options)
    }

    // Saves a value someone set in the app. A pinned setting or a value that does not fit is turned down with words for the person.
    fun set(key: String, value: String, by: String = ""): String? {
        if (key in pinned) return "This setting is set in the server file, so it cannot be changed here."
        ServerSettings.problem(key, value)?.let { return it }
        val kept = value.trim()
        transaction(db) {
            ServerState.upsert {
                it[ServerState.key] = Prefix + key
                it[ServerState.value] = kept
            }
            if (by.isNotEmpty()) ChangeLog.record(by, "changed a setting", key, kept)
        }
        saved[key] = kept
        return null
    }

    private companion object {
        const val Prefix = "setting."
    }
}
