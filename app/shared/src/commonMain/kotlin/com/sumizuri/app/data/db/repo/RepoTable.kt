package com.sumizuri.app.data.db.repo

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

// A repo the user added, by its address. Sumizuri has none of its own, so the table starts empty.
@Entity(tableName = "repo")
data class RepoEntity(@PrimaryKey val url: String, val name: String, val addedAt: Long)

@Dao
interface RepoDao {
    @Query("SELECT * FROM repo ORDER BY addedAt")
    fun observeAll(): Flow<List<RepoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(repo: RepoEntity)

    @Query("DELETE FROM repo WHERE url = :url")
    suspend fun delete(url: String)
}
