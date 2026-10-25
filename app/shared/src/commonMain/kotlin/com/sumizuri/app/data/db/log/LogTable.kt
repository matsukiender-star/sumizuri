package com.sumizuri.app.data.db.log

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query

// One saved line of the log. The level is the position of the level in its list, and the time is epoch milliseconds.
@Entity(tableName = "log")
data class LogRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    val level: Int,
    val tag: String,
    val message: String,
)

@Dao
interface LogDao {
    @Insert
    suspend fun insertAll(rows: List<LogRow>)

    // The newest lines, newest first, straight off the key.
    @Query("SELECT * FROM log ORDER BY id DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<LogRow>

    // Keeps the newest lines and drops the rest.
    @Query("DELETE FROM log WHERE id <= (SELECT MAX(id) FROM log) - :keep")
    suspend fun prune(keep: Int)

    @Query("DELETE FROM log")
    suspend fun clear()
}
