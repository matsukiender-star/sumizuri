package com.sumizuri.app.data.profile

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.sumizuri.app.data.account.LibrarySyncer
import com.sumizuri.app.data.settings.SettingKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

// What a new profile does not take from the one it is made from: the titles with their chapters and marks, the shelves, the log, the settings that are about the place the reader was at, and the link to a profile on the sync server. The link has to go, or the new profile syncs with the same server profile as the one it was made from, and fills with its library.
private val Emptied = listOf(
    "DELETE FROM title",
    "DELETE FROM category",
    "DELETE FROM log",
    "DELETE FROM setting WHERE name LIKE 'session.%' OR name LIKE 'cover.focus.%' OR name LIKE 'reader.title.%' OR name = 'library.category'",
    "DELETE FROM setting WHERE name IN ('${SettingKey.SyncLibraryId}', '${LibrarySyncer.LastAtKey}', '${LibrarySyncer.HistoryKey}')",
)

// Makes the file of a new profile from the file of one that exists. It keeps the sources, the repos and the settings, so the new reader does not start from nothing, and leaves the library empty.
suspend fun cloneWithoutLibrary(from: Path, to: Path, system: FileSystem = FileSystem.SYSTEM) =
    withContext(Dispatchers.IO) {
        val driver = BundledSQLiteDriver()
        // What is waiting in the journal goes into the file first, so the copy has everything.
        driver.open(from.toString()).use { it.run("PRAGMA wal_checkpoint(TRUNCATE)") }
        system.copy(from, to)
        try {
            driver.open(to.toString()).use { connection ->
                connection.execSQL("PRAGMA foreign_keys = ON")
                for (sql in Emptied) connection.execSQL(sql)
                connection.run("PRAGMA wal_checkpoint(TRUNCATE)")
                connection.execSQL("VACUUM")
            }
        } catch (error: Throwable) {
            // A half made file is no use to anyone.
            runCatching { system.delete(to) }
            throw error
        }
    }

private fun SQLiteConnection.run(sql: String) = prepare(sql).use { it.step() }
