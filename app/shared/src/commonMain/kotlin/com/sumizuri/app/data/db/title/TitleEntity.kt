package com.sumizuri.app.data.db.title

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

// One database file per profile, so there is no profile column. Kind and status are small codes.
@Entity(
    tableName = "title",
    indices = [
        Index("sourceId", "externalId", unique = true),
        Index("inLibrary", "name"),
    ],
)
data class TitleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val externalId: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE) val name: String,
    val kind: Int,
    val coverUrl: String?,
    val status: Int,
    val summary: String,
    // The genres are kept as one text with a new line between them.
    val author: String = "",
    val genres: String = "",
    val inLibrary: Boolean,
    val addedAt: Long,
    val updatedAt: Long,
    // The rating the source gave, or null when it gave none.
    val rating: Double? = null,
    // The page of the title on the website of the source, or null when the source gave none.
    val webUrl: String? = null,
    // Whether the person marked the title as a favourite.
    @ColumnInfo(defaultValue = "0") val favorite: Boolean = false,
    // The id of the same title listed by volume, when its source offers that, which shows the switch between chapters and volumes.
    val volumesId: String? = null,
)
