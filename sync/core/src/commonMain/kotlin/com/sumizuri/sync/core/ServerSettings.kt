package com.sumizuri.sync.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// What kind of value a server setting takes.
@Serializable
enum class SettingKind {
    @SerialName("text")
    Text,

    @SerialName("number")
    Number,

    @SerialName("choice")
    Choice,
}

// One live setting as the owner and admins see it: what it is now, its default, whether the server's .env pins it, and what it may be.
@Serializable
data class ServerSettingEntry(
    val key: String,
    val kind: SettingKind,
    val value: String,
    val default: String,
    val pinned: Boolean,
    val min: Int? = null,
    val max: Int? = null,
    val options: List<String> = emptyList(),
)

@Serializable
data class SettingChange(val key: String, val value: String)

// The one list of live settings. The server reads them, the app shows them, and the .env pins any of them by its name. Adding a setting is adding one entry here.
object ServerSettings {
    class Definition(
        val key: String,
        val env: String,
        val kind: SettingKind,
        val default: String,
        val min: Int? = null,
        val max: Int? = null,
        val options: List<String> = emptyList(),
    )

    const val Name = "name"
    const val SignupMode = "signupMode"
    const val MinPassword = "minPassword"
    const val WrongTries = "wrongTries"
    const val UserStorageMb = "userStorageMb"
    const val IdleDeviceDays = "idleDeviceDays"

    val all = listOf(
        Definition(Name, "SUMIZURI_NAME", SettingKind.Text, "Sumizuri sync"),
        Definition(
            SignupMode,
            "SUMIZURI_SIGNUP_MODE",
            SettingKind.Choice,
            "invite",
            options = listOf("open", "invite", "closed"),
        ),
        Definition(MinPassword, "SUMIZURI_MIN_PASSWORD", SettingKind.Number, "10", min = 8, max = 128),
        Definition(WrongTries, "SUMIZURI_WRONG_TRIES", SettingKind.Number, "5", min = 3, max = 50),
        // Nothing means no limit.
        Definition(UserStorageMb, "SUMIZURI_USER_STORAGE_MB", SettingKind.Number, "0", min = 0, max = 1_000_000),
        // Devices not seen for this many days are signed out. Nothing means never.
        Definition(IdleDeviceDays, "SUMIZURI_IDLE_DEVICE_DAYS", SettingKind.Number, "0", min = 0, max = 3650),
    )

    fun definition(key: String): Definition? = all.firstOrNull { it.key == key }

    // Why a value does not fit its setting, or nothing when it does.
    fun problem(key: String, value: String): String? {
        val definition = definition(key) ?: return "There is no setting named $key."
        val text = value.trim()
        return when (definition.kind) {
            SettingKind.Text -> if (text.isEmpty() || text.length > 64) "It must be 1 to 64 characters long." else null

            SettingKind.Choice -> if (text.lowercase() !in
                definition.options
            ) {
                "It must be one of ${definition.options.joinToString(", ")}."
            } else {
                null
            }

            SettingKind.Number -> {
                val number = text.toIntOrNull() ?: return "It must be a whole number."
                if (number < (definition.min ?: Int.MIN_VALUE) ||
                    number > (definition.max ?: Int.MAX_VALUE)
                ) {
                    "It must be from ${definition.min} to ${definition.max}."
                } else {
                    null
                }
            }
        }
    }
}
