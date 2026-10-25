package com.sumizuri.app.data.db.value

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

// One saved value of a source. The area says what kind it is, and the value is JSON text.
@Entity(tableName = "source_value", primaryKeys = ["scope", "area", "key"])
data class SourceValueEntity(val scope: String, val area: Int, val key: String, val value: String)

@Dao
interface SourceValueDao {
    @Query("SELECT value FROM source_value WHERE scope = :scope AND area = :area AND key = :key")
    suspend fun get(scope: String, area: Int, key: String): String?

    // Every value of every source, for a backup.
    @Query("SELECT * FROM source_value")
    suspend fun everything(): List<SourceValueEntity>

    @Query("SELECT * FROM source_value WHERE scope = :scope AND area = :area")
    suspend fun all(scope: String, area: Int): List<SourceValueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(value: SourceValueEntity)

    @Query("DELETE FROM source_value WHERE scope = :scope AND area = :area AND key = :key")
    suspend fun delete(scope: String, area: Int, key: String)

    @Query("DELETE FROM source_value WHERE scope = :scope AND area = :area")
    suspend fun clear(scope: String, area: Int)

    // Everything of one source, for when the source is removed.
    @Query("DELETE FROM source_value WHERE scope = :scope")
    suspend fun clearScope(scope: String)
}
