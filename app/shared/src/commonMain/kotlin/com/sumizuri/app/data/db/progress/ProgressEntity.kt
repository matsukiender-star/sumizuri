package com.sumizuri.app.data.db.progress

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sumizuri.app.data.db.chapter.ChapterEntity
import com.sumizuri.app.data.db.title.TitleEntity

// One row per touched chapter, with its title repeated so counts and history need no join.
@Entity(
    tableName = "progress",
    foreignKeys = [
        ForeignKey(
            ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            TitleEntity::class,
            parentColumns = ["id"],
            childColumns = ["titleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("titleId", "read"), Index("readAt")],
)
data class ProgressEntity(
    @PrimaryKey val chapterId: Long,
    val titleId: Long,
    val read: Boolean,
    val readAt: Long?,
    val position: Double?,
)
