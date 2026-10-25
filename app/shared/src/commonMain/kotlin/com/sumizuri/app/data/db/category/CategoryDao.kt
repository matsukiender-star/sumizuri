package com.sumizuri.app.data.db.category

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT categoryId, COUNT(*) AS titles FROM title_category GROUP BY categoryId")
    fun observeCounts(): Flow<List<CategoryCount>>

    @Query("SELECT titleId, categoryId FROM title_category")
    fun observeMemberships(): Flow<List<Membership>>

    @Query("SELECT titleId, categoryId FROM title_category")
    suspend fun memberships(): List<Membership>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM category")
    suspend fun lastOrder(): Int

    @Query("SELECT * FROM category ORDER BY sortOrder, id")
    suspend fun all(): List<CategoryEntity>

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Query("UPDATE category SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE category SET sortOrder = :order WHERE id = :id")
    suspend fun setOrder(id: Long, order: Int)

    @Query("UPDATE category SET excludeFromUpdate = :exclude WHERE id = :id")
    suspend fun setExcluded(id: Long, exclude: Boolean)

    @Query(
        "UPDATE category SET sortField = :sortField, sortAscending = :ascending, thenField = :thenField, thenAscending = :thenAscending, statusFilter = :status WHERE id = :id",
    )
    suspend fun setRule(
        id: Long,
        sortField: Int?,
        ascending: Boolean,
        thenField: Int?,
        thenAscending: Boolean,
        status: Int,
    )

    @Query("UPDATE category SET kind = :kind WHERE id = :id")
    suspend fun setKind(id: Long, kind: Int)

    @Query("UPDATE category SET conditions = :conditions, matchAll = :matchAll WHERE id = :id")
    suspend fun setConditions(id: Long, conditions: String, matchAll: Boolean)

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT categoryId FROM title_category WHERE titleId = :titleId")
    suspend fun categoriesOf(titleId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addAll(rows: List<TitleCategoryEntity>)

    @Query("DELETE FROM title_category WHERE titleId IN (:titleIds) AND categoryId IN (:categoryIds)")
    suspend fun removeFrom(titleIds: List<Long>, categoryIds: List<Long>)

    // The titles that sit on a shelf which is left out of the check for new chapters.
    @Query(
        "SELECT DISTINCT tc.titleId FROM title_category tc JOIN category c ON c.id = tc.categoryId WHERE c.excludeFromUpdate = 1",
    )
    suspend fun excludedTitles(): List<Long>
}
