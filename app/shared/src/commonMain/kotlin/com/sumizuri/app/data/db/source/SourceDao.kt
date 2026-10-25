package com.sumizuri.app.data.db.source

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM source ORDER BY name")
    fun observeAll(): Flow<List<SourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(source: SourceEntity)

    @Query("SELECT * FROM source WHERE id = :id")
    suspend fun get(id: String): SourceEntity?

    @Query("UPDATE source SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("UPDATE source SET lastUsedAt = :now WHERE id = :id")
    suspend fun touch(id: String, now: Long)

    @Query("DELETE FROM source WHERE id = :id")
    suspend fun delete(id: String)
}
