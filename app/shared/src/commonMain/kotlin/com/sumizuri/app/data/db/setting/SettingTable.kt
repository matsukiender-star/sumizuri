package com.sumizuri.app.data.db.setting

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "setting")
data class SettingEntity(@PrimaryKey val name: String, val value: String)

@Dao
interface SettingDao {
    @Query("SELECT value FROM setting WHERE name = :name")
    fun observe(name: String): Flow<String?>

    // Every setting but the notes kept about covers, which are many and only read one at a time.
    @Query("SELECT * FROM setting WHERE name NOT LIKE 'memo.%'")
    fun observeAll(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM setting WHERE name LIKE :prefix || '%'")
    fun observeWithPrefix(prefix: String): Flow<List<SettingEntity>>

    // The settings as they are now, for a backup. The memos are left out, since they are kept work and not choices.
    @Query("SELECT * FROM setting WHERE name NOT LIKE 'memo.%'")
    suspend fun everything(): List<SettingEntity>

    @Query("SELECT value FROM setting WHERE name = :name")
    suspend fun get(name: String): String?

    // The settings whose names start with the prefix, read once and not watched, so they can be read inside a transaction.
    @Query("SELECT * FROM setting WHERE name LIKE :prefix || '%'")
    suspend fun withPrefix(prefix: String): List<SettingEntity>

    @Query("DELETE FROM setting WHERE name = :name")
    suspend fun delete(name: String)

    @Query("DELETE FROM setting WHERE name LIKE :prefix || '%' AND name NOT LIKE :except || '%'")
    suspend fun deleteWithPrefix(prefix: String, except: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(setting: SettingEntity)
}
