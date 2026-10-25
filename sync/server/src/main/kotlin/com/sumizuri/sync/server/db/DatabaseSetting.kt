package com.sumizuri.sync.server.db

import com.sumizuri.sync.server.ConfigException
import java.io.File
import java.net.URI
import java.net.URLDecoder

// Which database the server keeps its data in, read from SUMIZURI_DATABASE_URL.
sealed interface DatabaseSetting {
    // One file, the default, which needs nothing else set up.
    data class Sqlite(val file: File) : DatabaseSetting

    // A Postgres server, for many people or for a server that already runs one.
    data class Postgres(val jdbcUrl: String, val user: String?, val password: String?) : DatabaseSetting {
        // Where it is, without the password, for the log.
        val shown: String get() = jdbcUrl.removePrefix("jdbc:postgresql://").substringBefore('?')
        override fun toString() = "Postgres($shown)"
    }

    companion object {
        // No address means SQLite in the data folder. A sqlite: address names another file. A postgres:// address, or a jdbc: one, means Postgres, with the user and password in it or in their own settings.
        fun parse(url: String?, user: String?, password: String?, dataDir: File): DatabaseSetting {
            if (url == null) return Sqlite(File(dataDir, "sumizuri.db"))
            val lower = url.lowercase()
            return when {
                lower.startsWith("jdbc:sqlite:") -> Sqlite(fileIn(url.substring("jdbc:sqlite:".length), dataDir))

                lower.startsWith(
                    "sqlite:",
                ) -> Sqlite(fileIn(url.substring("sqlite:".length).removePrefix("//"), dataDir))

                lower.startsWith("jdbc:postgresql:") -> Postgres(url, user, password)

                lower.startsWith(
                    "postgres://",
                ) || lower.startsWith("postgresql://") -> postgresFrom(url, user, password)

                else -> throw ConfigException(
                    "SUMIZURI_DATABASE_URL must start with sqlite:, postgres:// or jdbc:, not ${url.substringBefore(
                        ':',
                    )}:",
                )
            }
        }

        private fun fileIn(path: String, dataDir: File): File = File(path).let {
            if (it.isAbsolute) it else File(dataDir, path)
        }

        // Turns postgres://user:password@host:5432/name?options into the address the driver takes, keeping the user and password apart.
        private fun postgresFrom(url: String, user: String?, password: String?): Postgres {
            val uri = try {
                URI(url)
            } catch (problem: Exception) {
                throw ConfigException("SUMIZURI_DATABASE_URL is not a valid address: ${problem.message}")
            }
            val host =
                uri.host
                    ?: throw ConfigException(
                        "SUMIZURI_DATABASE_URL has no host, such as postgres://user:password@db:5432/sumizuri",
                    )
            val name =
                uri.path?.removePrefix("/")?.takeIf { it.isNotEmpty() }
                    ?: throw ConfigException("SUMIZURI_DATABASE_URL has no database name at the end, such as /sumizuri")
            val port = if (uri.port > 0) uri.port else 5432
            val info = uri.rawUserInfo?.split(':', limit = 2)?.map { URLDecoder.decode(it, Charsets.UTF_8) }
            val query = uri.rawQuery?.let { "?$it" }.orEmpty()
            return Postgres(
                "jdbc:postgresql://$host:$port/$name$query",
                user ?: info?.getOrNull(0),
                password ?: info?.getOrNull(1),
            )
        }
    }
}
