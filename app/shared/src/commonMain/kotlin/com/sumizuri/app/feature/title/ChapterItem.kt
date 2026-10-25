package com.sumizuri.app.feature.title

// One row of the chapter list, from the database or straight from a source. The time is epoch milliseconds, or 0 when unknown.
data class ChapterItem(
    val key: String,
    // The row id when the chapter is saved, and its place in the list from the source plus one for a preview.
    val id: Long,
    val number: Double,
    val name: String?,
    val at: Long,
    val read: Boolean,
    val bookmarked: Boolean = false,
    // When it was marked read, or null.
    val readAt: Long? = null,
    // How far the reader got in a chapter that is not finished, from 0 to 1, or null.
    val progress: Float? = null,
    // The season the chapter is in as the source names it, with its name and picture.
    val season: String? = null,
    val seasonName: String? = null,
    val seasonCoverUrl: String? = null,
    // Where the chapter is in the list of the source, or -1 when it is not listed.
    val sourceOrder: Int = -1,
    // The group that put it out, such as a scanlation team, when the source names one.
    val scanlator: String? = null,
    // Its language as a code such as en, and its volume, when the source names them.
    val language: String? = null,
    val volume: Double? = null,
    // Other labels the source gave, by name.
    val tags: Map<String, String> = emptyMap(),
)
