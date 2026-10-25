package com.sumizuri.app.data.db.source

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

// An installed source with the code it runs. Kind and engine are small codes, and the code is kept whole.
@Entity(tableName = "source")
data class SourceEntity(
    @PrimaryKey val id: String,
    val repoUrl: String?,
    val name: String,
    val lang: String,
    val kind: Int,
    val engineKind: String,
    val version: Int,
    val iconUrl: String,
    val baseUrl: String,
    val code: String,
    // What the repo said about the source that the engine needs, as JSON text.
    @ColumnInfo(defaultValue = "") val extra: String = "",
    // A source that is switched off stays installed but is not offered for browsing.
    @ColumnInfo(defaultValue = "1") val enabled: Boolean = true,
    // When the source was last opened, epoch milliseconds, or 0 when it never was.
    @ColumnInfo(defaultValue = "0") val lastUsedAt: Long = 0,
)
