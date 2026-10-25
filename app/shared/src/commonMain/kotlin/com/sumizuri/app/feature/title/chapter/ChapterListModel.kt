package com.sumizuri.app.feature.title.chapter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sumizuri.app.data.migration.numberKey
import com.sumizuri.app.data.model.TriFilter
import com.sumizuri.app.feature.title.ChapterItem
import com.sumizuri.app.feature.title.TitleState

// The chapters as the list shows them with what is worked out from them, kept until the chapters or the choices change so a long list is not worked through on every frame. A title with seasons shows its seasons until one is open, and then only that season.
internal class ChapterListModel(
    val shown: List<ChapterItem>,
    val duplicates: Int,
    val gaps: ChapterGaps,
    val missing: List<Int>,
    val seasons: List<SeasonGroup> = emptyList(),
    val openSeason: SeasonGroup? = null,
) {
    // The seasons are picked from before any chapter shows.
    val pickingSeason: Boolean get() = seasons.isNotEmpty() && openSeason == null
}

@Composable
internal fun rememberChapterListModel(state: TitleState, open: String? = null): ChapterListModel {
    val seasons = remember(state.chapters) { seasonGroups(state.chapters) }
    val openSeason = remember(seasons, open) { seasons.firstOrNull { it.id == open } }
    val chapters =
        remember(state.chapters, openSeason) {
            openSeason?.let { group ->
                state.chapters.filter { it.season in group.keys }
            }
                ?: state.chapters
        }
    val duplicates = remember(chapters) { duplicateCount(chapters) }
    // The saved chapters only matter while the list is filtered by them, so a download going on does not work the list out again otherwise.
    val saved = if (state.filter.downloaded != TriFilter.Any) state.savedIds else null
    val shown =
        remember(
            state.chapters,
            state.hideDuplicates,
            state.oldestFirst,
            state.sort,
            openSeason,
            state.filter,
            state.hidden,
            saved,
        ) {
            when {
                openSeason != null -> state.shown.filter { it.season in openSeason.keys }
                seasons.isNotEmpty() -> emptyList()
                else -> state.shown
            }
        }
    // Parts shown as one season may each start again at one, which is not a gap.
    val gaps =
        remember(shown, state.grid, openSeason) {
            if (state.grid || openSeason?.merged == true) ChapterGaps(emptyList(), emptyList()) else chapterGaps(shown)
        }
    val missing = remember(chapters) { missingNumbers(chapters) }
    return remember(shown, duplicates, gaps, missing, seasons, openSeason) {
        ChapterListModel(shown, duplicates, gaps, missing, seasons, openSeason)
    }
}

private fun touched(chapter: ChapterItem) = chapter.read || chapter.bookmarked || chapter.progress != null

// The chapters with every repeat of a number in the same season dropped, in the order they came. Of two with the same number the one that was read, bookmarked or begun is kept. Episodes of different seasons share numbers and are not repeats. Numbers are the same when they agree to the third decimal, as one that came through a backup of another app can be off in its last digits, such as 90.0999 for 90.1.
fun withoutDuplicates(chapters: List<ChapterItem>): List<ChapterItem> {
    val keptAt = HashMap<Pair<String?, Long>, Int>()
    val kept = mutableListOf<ChapterItem>()
    for (chapter in chapters) {
        val key = chapter.season to numberKey(chapter.number)
        val at = keptAt[key]
        if (at == null) {
            keptAt[key] = kept.size
            kept += chapter
        } else if (!touched(kept[at]) && touched(chapter)) {
            kept[at] = chapter
        }
    }
    return kept
}

fun duplicateCount(chapters: List<ChapterItem>): Int = chapters.size - withoutDuplicates(chapters).size
