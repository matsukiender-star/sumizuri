package com.sumizuri.app.data.migration

import com.sumizuri.app.data.model.Title
import com.sumizuri.app.source.model.SourceChapter
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.repo.InstalledSource

// A place a title could move to: the entry a source has, how well it fits, and how many chapters it has, which is null when its chapters were not read. The chapters themselves are kept only for the best match, since the others are rarely used.
class MigrationOption(
    val source: InstalledSource,
    val entry: SourceEntry,
    val score: MigrationScore,
    val chapterCount: Int?,
    val chapters: List<SourceChapter>? = null,
)

// One title of the work with what was found for it. A title that is not included is left out of every search and move.
data class MigrationItem(
    val title: Title,
    // The name of the source it comes from now.
    val fromName: String,
    val shelves: Set<Long> = emptySet(),
    val included: Boolean = true,
    val status: MigrationStatus = MigrationStatus.Queued,
    val options: List<MigrationOption> = emptyList(),
    val chosen: MigrationOption? = null,
    val error: String? = null,
    val problem: MoveProblem? = null,
    // How many results the last search ruled out by the rules, and how many sources have been asked.
    val ruledOut: Int = 0,
    val asked: Int = 0,
    // The id of the title it became, once it is moved.
    val movedTo: Long? = null,
) {
    val id: Long get() = title.id
}

// A source that can be searched, in its place in the order the sources are tried in, with how the run has gone with it.
data class SourceChoice(
    val source: InstalledSource,
    val included: Boolean = true,
    val asked: Int = 0,
    val matched: Int = 0,
    val failures: Int = 0,
    val gaveUp: Boolean = false,
)

// Everything about the work: the titles, the sources, the rules, and how far a run has got.
data class MigrationState(
    val loaded: Boolean = false,
    val items: List<MigrationItem> = emptyList(),
    val sources: List<SourceChoice> = emptyList(),
    val rules: MigrationRules = MigrationRules(),
    val running: Boolean = false,
    val moving: Boolean = false,
    val currentSource: String? = null,
    // Which of the sources of the run is being searched, counted from one, and how many the run has.
    val sourceIndex: Int = 0,
    val sourceCount: Int = 0,
    val processed: Int = 0,
    val total: Int = 0,
) {
    val busy: Boolean get() = running || moving

    fun count(status: MigrationStatus): Int = items.count { it.included && it.status == status }
}
