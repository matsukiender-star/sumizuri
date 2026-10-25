package com.sumizuri.app.data.library

import com.sumizuri.app.data.blocks.BlockContext
import com.sumizuri.app.data.blocks.ReadEvent
import com.sumizuri.app.data.db.AppDatabase
import com.sumizuri.app.data.model.stats.readingStatsOf
import com.sumizuri.app.data.text.nowMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

// The reading of the open profile: the chapters read and when, the titles of the library, and the statistics added up from them. It follows every chapter that is marked read, and the blocks of the profile run on it.
class StatisticsRepository(db: AppDatabase, library: LibraryRepository) {
    val reading: Flow<BlockContext> = combine(db.progress().observeReadStamps(), library.library) { reads, titles ->
        val now = nowMillis()
        BlockContext(
            reads.map {
                ReadEvent(it.titleId, it.at)
            },
            titles,
            readingStatsOf(reads.map { it.titleId to it.at }, titles, now),
            now,
        )
    }
}
