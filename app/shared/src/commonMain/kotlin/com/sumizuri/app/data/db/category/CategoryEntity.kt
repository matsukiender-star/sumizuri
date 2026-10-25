package com.sumizuri.app.data.db.category

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sumizuri.app.data.db.title.TitleEntity

// A shelf of the library that the person made. A title can be on any number of them. The order is the order of the tabs, and a shelf can leave its titles out of the check for new chapters.
@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val sortOrder: Int,
    val excludeFromUpdate: Boolean = false,
    // What the shelf is ordered by instead of the order of the library, as the number of the sort, or null to use the order of the library.
    val sortField: Int? = null,
    @ColumnInfo(defaultValue = "1") val sortAscending: Boolean = true,
    // The status the shelf shows only, as the code of the status, with 0 for any.
    @ColumnInfo(defaultValue = "0") val statusFilter: Int = 0,
    // The kind of title the shelf is for, as the code of the kind, with -1 for a shelf that is for every kind.
    @ColumnInfo(defaultValue = "-1") val kind: Int = -1,
    // The field that settles ties of the shelf order, as the place of the field, or null for none, and its direction.
    val thenField: Int? = null,
    @ColumnInfo(defaultValue = "1") val thenAscending: Boolean = true,
    // The conditions that put titles on the shelf by themselves, as text, and whether a title must meet all of them.
    @ColumnInfo(defaultValue = "") val conditions: String = "",
    @ColumnInfo(defaultValue = "1") val matchAll: Boolean = true,
)

// One title on one shelf. A title leaving the database or a shelf being deleted takes its rows with it.
@Entity(
    tableName = "title_category",
    primaryKeys = ["titleId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            TitleEntity::class,
            parentColumns = ["id"],
            childColumns = ["titleId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId")],
)
data class TitleCategoryEntity(val titleId: Long, val categoryId: Long)

// A title on a shelf, without the rest of either.
data class Membership(val titleId: Long, val categoryId: Long)

// How many titles a shelf holds.
data class CategoryCount(val categoryId: Long, val titles: Int)

// A time of a title, such as when it was last read, as epoch milliseconds.
data class TitleTime(val titleId: Long, val at: Long)
