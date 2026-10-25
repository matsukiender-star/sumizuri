package com.sumizuri.app.feature.migration.single

import com.sumizuri.app.data.migration.MigrationPlan
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.source.model.SourceEntry
import com.sumizuri.app.source.repo.InstalledSource

// What one source found for the name that is searched.
data class MigrateResult(
    val source: InstalledSource,
    val entries: List<SourceEntry> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

// An entry the person picked, with what moving to it would carry over once its chapters are read.
data class MigrateChoice(
    val source: InstalledSource,
    val entry: SourceEntry,
    val plan: MigrationPlan? = null,
    val error: String? = null,
)

data class MigrateState(
    val loaded: Boolean = false,
    // The title that moves, or null when it is gone.
    val title: Title? = null,
    // The name of the source it comes from, as far as that is known.
    val from: String = "",
    val query: String = "",
    // The installed sources of the same kind that can be searched, and the ones that are ticked.
    val candidates: List<InstalledSource> = emptyList(),
    val chosen: Set<String> = emptySet(),
    val results: List<MigrateResult> = emptyList(),
    val picked: MigrateChoice? = null,
    val keepOld: Boolean = false,
    val working: Boolean = false,
    val problem: String? = null,
) {
    val searching: Boolean get() = results.any { it.loading }
}
