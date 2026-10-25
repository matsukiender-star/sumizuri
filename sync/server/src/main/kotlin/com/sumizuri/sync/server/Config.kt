package com.sumizuri.sync.server

import com.sumizuri.sync.core.ServerSettings
import com.sumizuri.sync.core.SettingKind
import com.sumizuri.sync.server.db.DatabaseSetting
import java.io.File

// The machine settings of the server, and the live settings the .env pins. They come from the environment, which is what Docker passes, and a .env file in the working folder fills in what the environment leaves out.
data class Config(
    val dataDir: File,
    val database: DatabaseSetting,
    val port: Int,
    // The address the server listens on. 127.0.0.1 keeps it to this machine, behind a proxy that adds HTTPS.
    val host: String,
    val publicUrl: String?,
    val trustProxy: Boolean,
    val maxUploadMb: Int,
    // Live settings given in the environment, by their key. The app shows them locked, and nobody can change them there.
    val pinned: Map<String, String>,
) {
    companion object {
        fun load(environment: Map<String, String> = System.getenv(), envFile: File = File(".env")): Config {
            val values = readEnvFile(envFile) + environment
            fun text(key: String): String? = values["SUMIZURI_$key"]?.trim()?.takeIf { it.isNotEmpty() }
            fun number(key: String, default: Int): Int =
                text(key)?.let { it.toIntOrNull() ?: fail("SUMIZURI_$key must be a number, not $it") } ?: default
            fun flag(key: String, default: Boolean): Boolean = when (text(key)?.lowercase()) {
                null -> default
                "true", "on", "yes", "1" -> true
                "false", "off", "no", "0" -> false
                else -> fail("SUMIZURI_$key must be on or off, not ${text(key)}")
            }
            val dataDir =
                text("DATA_DIR")
                    ?: fail(
                        "SUMIZURI_DATA_DIR is not set. Give the folder where the server keeps its data, in the environment or in a .env file.",
                    )
            val pinned = ServerSettings.all
                .mapNotNull { setting ->
                    val value = values[setting.env]?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                    ServerSettings.problem(setting.key, value)?.let { fail("${setting.env}: $it") }
                    setting.key to if (setting.kind == SettingKind.Text) value else value.lowercase()
                }.toMap()
            val dataFolder = File(dataDir).absoluteFile
            return Config(
                dataDir = dataFolder,
                database = DatabaseSetting.parse(
                    text("DATABASE_URL"),
                    text("DATABASE_USER"),
                    values["SUMIZURI_DATABASE_PASSWORD"],
                    dataFolder,
                ),
                port = number("PORT", 8080),
                host = text("HOST") ?: "0.0.0.0",
                publicUrl = text("PUBLIC_URL")?.trimEnd('/'),
                trustProxy = flag("TRUST_PROXY", true),
                maxUploadMb = number("MAX_UPLOAD_MB", 100),
                pinned = pinned,
            )
        }

        // Reads KEY=value lines, leaving out blank lines and lines that start with #, and taking off quotes around a value.
        private fun readEnvFile(file: File): Map<String, String> {
            if (!file.isFile) return emptyMap()
            return file
                .readLines()
                .mapNotNull { line ->
                    val text = line.trim()
                    if (text.isEmpty() || text.startsWith("#") || '=' !in text) return@mapNotNull null
                    val key = text.substringBefore('=').trim()
                    val value = text
                        .substringAfter('=')
                        .trim()
                        .removeSurrounding("\"")
                        .removeSurrounding("'")
                    key to value
                }.toMap()
        }

        private fun fail(message: String): Nothing = throw ConfigException(message)
    }
}

// A setting that is missing or wrong, said in words the person who runs the server can act on.
class ConfigException(message: String) : RuntimeException(message)
