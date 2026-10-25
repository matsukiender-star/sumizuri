package com.sumizuri.app.data.model.library

import com.sumizuri.app.data.text.enumNamed
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.sort_added
import com.sumizuri.app.shared.resources.sort_author
import com.sumizuri.app.shared.resources.sort_chapters
import com.sumizuri.app.shared.resources.sort_favorite
import com.sumizuri.app.shared.resources.sort_kind
import com.sumizuri.app.shared.resources.sort_last_read
import com.sumizuri.app.shared.resources.sort_latest
import com.sumizuri.app.shared.resources.sort_progress
import com.sumizuri.app.shared.resources.sort_random
import com.sumizuri.app.shared.resources.sort_rating
import com.sumizuri.app.shared.resources.sort_source
import com.sumizuri.app.shared.resources.sort_status
import com.sumizuri.app.shared.resources.sort_title
import com.sumizuri.app.shared.resources.sort_unread
import org.jetbrains.compose.resources.StringResource

// An order of the library: the sort field and its direction, and a second field that settles titles the first one ties.
data class SortRule(
    val sort: LibrarySortField = LibrarySortField.Title,
    val ascending: Boolean = true,
    val then: LibrarySortField? = null,
    val thenAscending: Boolean = true,
) {
    // Every field the rule reads.
    val fields: Set<LibrarySortField> get() = setOfNotNull(sort, then)

    // The rule as one line of text, to keep it in a setting row.
    fun encode(): String = listOf(sort.name, ascending, then?.name ?: "-", thenAscending).joinToString(",")

    companion object {
        // The rule a line of text holds, or null when it holds none.
        fun decode(text: String?): SortRule? {
            val parts = text?.split(",") ?: return null
            if (parts.size != 4) return null
            val sort = enumNamed<LibrarySortField>(parts[0]) ?: return null
            return SortRule(
                sort,
                parts[1].toBooleanStrictOrNull() ?: true,
                enumNamed<LibrarySortField>(parts[2]),
                parts[3].toBooleanStrictOrNull() ?: true,
            )
        }
    }
}

// What the library can be ordered by. A shelf saves its choice as the place of the field in this list, so new fields go at the end.
enum class LibrarySortField(val label: StringResource) {
    Title(Res.string.sort_title),
    Unread(Res.string.sort_unread),
    Added(Res.string.sort_added),
    LastRead(Res.string.sort_last_read),
    LatestChapter(Res.string.sort_latest),
    ChapterCount(Res.string.sort_chapters),
    Progress(Res.string.sort_progress),
    Status(Res.string.sort_status),
    Source(Res.string.sort_source),
    Author(Res.string.sort_author),
    Rating(Res.string.sort_rating),
    Kind(Res.string.sort_kind),
    Favorite(Res.string.sort_favorite),
    Random(Res.string.sort_random),
}
