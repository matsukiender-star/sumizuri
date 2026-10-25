package com.sumizuri.app.ui.kit.cover

import androidx.compose.runtime.Composable
import com.sumizuri.app.data.local.LocalSources
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.local_manga_name
import com.sumizuri.app.shared.resources.local_novels_name
import com.sumizuri.app.source.repo.InstalledSource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// The words for the two sources of files on the device, which are named in the language of the app. Other sources have none and keep the name they came with.
fun localSourceName(id: String?): StringResource? = when (id) {
    LocalSources.MangaId -> Res.string.local_manga_name
    LocalSources.NovelId -> Res.string.local_novels_name
    else -> null
}

// The name a source is shown by.
@Composable
fun sourceName(id: String?, name: String): String = localSourceName(id)?.let { stringResource(it) } ?: name

@Composable
fun InstalledSource.shownName(): String = sourceName(id, name)
