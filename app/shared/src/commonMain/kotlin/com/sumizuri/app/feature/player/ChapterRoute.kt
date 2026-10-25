package com.sumizuri.app.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.feature.comments.CommentsTarget
import com.sumizuri.app.feature.reader.ReaderRoute

// Opens a chapter in what suits it: an episode in the player, and pages or words in the reader. It looks at the kind of the title first, and shows black until it knows.
@Composable
fun ChapterRoute(
    chapterId: Long,
    onBack: () -> Unit,
    onOpenChapter: (Long) -> Unit,
    onOpenComments: (CommentsTarget) -> Unit,
) {
    val library = LocalAppContainer.current.library
    val kind by produceState<Kind?>(null, chapterId) { value = library.chapterPlace(chapterId)?.kind ?: Kind.Manga }
    when (kind) {
        null -> Box(Modifier.fillMaxSize().background(Color.Black))
        Kind.Anime -> PlayerRoute(chapterId, onBack)
        else -> ReaderRoute(chapterId, onBack, onOpenChapter, onOpenComments)
    }
}
