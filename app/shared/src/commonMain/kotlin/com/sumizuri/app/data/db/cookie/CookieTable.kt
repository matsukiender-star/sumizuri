package com.sumizuri.app.data.db.cookie

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

// A cookie of one source. A null expiry means it lasts until it is cleared.
@Entity(tableName = "cookie", primaryKeys = ["scope", "domain", "path", "name"])
data class CookieEntity(
    val scope: String,
    val domain: String,
    val path: String,
    val name: String,
    val value: String,
    val expiresAt: Long?,
    val secure: Boolean,
    val httpOnly: Boolean,
)

@Dao
interface CookieDao {
    // Every cookie, for a backup that keeps the sign ins to sites.
    @Query("SELECT * FROM cookie")
    suspend fun everything(): List<CookieEntity>

    @Query("SELECT * FROM cookie WHERE scope = :scope")
    suspend fun all(scope: String): List<CookieEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(cookie: CookieEntity)

    @Query("DELETE FROM cookie WHERE scope = :scope AND domain = :domain AND path = :path AND name = :name")
    suspend fun delete(scope: String, domain: String, path: String, name: String)

    @Query("DELETE FROM cookie WHERE expiresAt IS NOT NULL AND expiresAt <= :now")
    suspend fun deleteExpired(now: Long)

    @Query("DELETE FROM cookie WHERE scope = :scope")
    suspend fun clearScope(scope: String)
}
