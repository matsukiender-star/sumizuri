package com.sumizuri.app.source.model

// A title as a source lists it.
data class SourceEntry(
    val url: String,
    val title: String,
    val coverUrl: String? = null,
    val bannerUrl: String? = null,
    val author: String? = null,
    val description: String? = null,
    val rating: Double? = null,
    val status: String? = null,
    val genres: List<String> = emptyList(),
    val webUrl: String? = null,
    // The id of the same title listed by volume instead of by chapter, for a source that offers both. Opening it gives whole volumes as the chapters.
    val volumesId: String? = null,
)

// A chapter, or an episode for anime. Times are epoch milliseconds.
data class SourceChapter(
    val url: String,
    val title: String,
    val number: Double? = null,
    val volume: Double? = null,
    val scanlator: String? = null,
    // The language of the chapter, for a site that lists each chapter once for every language.
    val language: String? = null,
    // Any other labels of the chapter by name, such as an edition or a quality, so a title can show only some of them.
    val tags: Map<String, String> = emptyMap(),
    val uploadedAt: Long? = null,
    val locked: Boolean = false,
    val unlocksAt: Long? = null,
    val webUrl: String? = null,
    val season: String? = null,
    val seasonName: String? = null,
    val seasonCoverUrl: String? = null,
)

data class SourceComment(
    val author: String,
    val text: String,
    val postedAt: Long? = null,
    val avatarUrl: String? = null,
    // The key of the comment on the site, which its replies are asked for by.
    val id: String? = null,
    val likes: Int? = null,
    // Hidden until tapped, as it gives something away.
    val spoiler: Boolean = false,
    // How many replies it has, and the ones the source gave with it. More can be asked for when there are more than it gave.
    val replyCount: Int = 0,
    val replies: List<SourceComment> = emptyList(),
)

// One page of comments or replies. Whether another page follows and how many there are in all are known only when the source says so.
data class CommentPage(val comments: List<SourceComment>, val hasNextPage: Boolean? = null, val total: Int? = null)

enum class CommentSort { Newest, Oldest, Top }

// What an extension says about itself, read once when it is loaded.
data class SourceInfo(
    val name: String,
    val lang: String?,
    val iconUrl: String?,
    val baseUrl: String?,
    val hasDetails: Boolean,
    val hasFilters: Boolean,
    val hasCatalogs: Boolean,
    val hasComments: Boolean = false,
    val hasChapterComments: Boolean = false,
    val hasCommentReplies: Boolean = false,
    val hasPreferences: Boolean = false,
)

// A named list a source offers besides popular and latest, such as a genre or a ranking.
data class SourceCatalog(val id: String, val name: String)
