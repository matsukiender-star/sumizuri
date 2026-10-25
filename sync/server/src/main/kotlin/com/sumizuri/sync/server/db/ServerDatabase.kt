package com.sumizuri.sync.server.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection

// The steps of the database layout, in order. A step is never changed once released, and a new layout is a new step at the end, so every database, SQLite or Postgres, upgrades the same way.
private val Steps: List<JdbcTransaction.() -> Unit> = listOf(
    { SchemaUtils.create(ServerState) },
    { SchemaUtils.create(Users, Devices, Invites) },
    { SchemaUtils.create(Libraries) },
    { SchemaUtils.create(Snapshots) },
    // Deleting a person can be undone for a while, and a change log keeps what was done.
    {
        // A new server made the column with the table already, so it is only added to an older one.
        val jdbc = connection.connection as java.sql.Connection
        val has = jdbc.metaData.getColumns(null, null, "users", "deleted_at").use { it.next() }
        if (!has) exec("ALTER TABLE users ADD COLUMN deleted_at BIGINT NULL")
        SchemaUtils.create(AuditLog)
    },
    // Two-step sign in with the codes of an authenticator app.
    {
        val jdbc = connection.connection as java.sql.Connection
        fun has(column: String) = jdbc.metaData.getColumns(null, null, "users", column).use { it.next() }
        if (!has("two_step_secret")) exec("ALTER TABLE users ADD COLUMN two_step_secret VARCHAR(64) NULL")
        if (!has("two_step_pending")) exec("ALTER TABLE users ADD COLUMN two_step_pending VARCHAR(64) NULL")
        if (!has("two_step_used")) exec("ALTER TABLE users ADD COLUMN two_step_used BIGINT NOT NULL DEFAULT 0")
    },
    // Libraries as rows, sent and fetched in batches.
    {
        val jdbc = connection.connection as java.sql.Connection
        fun has(column: String) = jdbc.metaData.getColumns(null, null, "libraries", column).use { it.next() }
        if (!has("row_head")) exec("ALTER TABLE libraries ADD COLUMN row_head BIGINT NOT NULL DEFAULT 0")
        if (!has("rows_at")) exec("ALTER TABLE libraries ADD COLUMN rows_at BIGINT NULL")
        SchemaUtils.create(SyncRows)
    },
)

// Opens the database the setting names through a pool, and brings its layout up to the newest step.
class ServerDatabase(val setting: DatabaseSetting) {
    private val source = HikariDataSource(poolFor(setting))
    val db: Database = Database.connect(
        source,
        databaseConfig = DatabaseConfig {
            // SQLite only takes these two levels, and serializable keeps its writes in order.
            if (setting is DatabaseSetting.Sqlite) defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        },
    )

    // Runs each step this database has not had yet, each in its own transaction, and says how many ran.
    fun migrate(): Int {
        val current = transaction(db) {
            SchemaUtils.create(SchemaVersion)
            SchemaVersion.selectAll().singleOrNull()?.get(SchemaVersion.version) ?: 0
        }
        if (current >
            Steps.size
        ) {
            error(
                "The database is at layout step $current, newer than this server knows (${Steps.size}). Use a newer server.",
            )
        }
        for (step in current until Steps.size) {
            transaction(db) {
                Steps[step]()
                SchemaVersion.deleteAll()
                SchemaVersion.insert { it[version] = step + 1 }
            }
        }
        return Steps.size - current
    }

    fun close() = source.close()

    private companion object {
        fun poolFor(setting: DatabaseSetting) = HikariConfig().apply {
            poolName = "sumizuri"
            when (setting) {
                is DatabaseSetting.Sqlite -> {
                    setting.file.absoluteFile.parentFile
                        ?.mkdirs()
                    jdbcUrl = "jdbc:sqlite:${setting.file.absolutePath}"
                    // Readers do not wait for a writer, a busy file is waited on for a while instead of failing, and links between tables are checked.
                    addDataSourceProperty("journal_mode", "WAL")
                    addDataSourceProperty("busy_timeout", "5000")
                    addDataSourceProperty("foreign_keys", "true")
                    maximumPoolSize = 4
                    transactionIsolation = "TRANSACTION_SERIALIZABLE"
                }

                is DatabaseSetting.Postgres -> {
                    jdbcUrl = setting.jdbcUrl
                    setting.user?.let { username = it }
                    setting.password?.let { password = it }
                    maximumPoolSize = 10
                }
            }
        }
    }
}
