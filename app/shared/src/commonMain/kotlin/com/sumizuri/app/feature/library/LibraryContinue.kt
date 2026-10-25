package com.sumizuri.app.feature.library

import com.sumizuri.app.data.library.LibraryRepository
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.feature.title.chapter.continueTargetOf
import com.sumizuri.app.feature.title.toItem
import kotlinx.coroutines.flow.first

// The chapter the continue button of a title would open, read from its chapters as the page of the title works it out, or null for a title with no chapters yet.
internal suspend fun continueChapterOf(library: LibraryRepository, titleId: Long): Long? {
    val chapters = library.chapters(titleId).first().map { it.toItem() }
    return continueTargetOf(chapters, nowMillis())?.id
}
