package com.sumizuri.app.feature.title

import com.sumizuri.app.data.download.AutoDownloads
import com.sumizuri.app.data.download.Downloader
import com.sumizuri.app.data.library.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// What the title page does to its chapters: read marks and bookmarks, saving them to the device, and picking several to act on at once. It works on the list the page shows, which may be the volumes of the title.
class TitleChapterActions internal constructor(
    private val scope: CoroutineScope,
    private val library: LibraryRepository,
    private val downloads: Downloader,
    private val auto: AutoDownloads,
    private val look: MutableStateFlow<Look>,
    private val listId: StateFlow<Long>,
    private val state: () -> TitleState,
) {
    fun onToggleRead(chapterId: Long, read: Boolean) {
        scope.launch {
            library.setRead(listId.value, chapterId, read)
            if (read) auto.onMarkedRead(listId.value, listOf(chapterId))
        }
    }

    fun onToggleBookmark(chapterId: Long, bookmarked: Boolean) {
        scope.launch { library.setBookmarked(chapterId, bookmarked) }
    }

    fun onMarkAll(read: Boolean) {
        scope.launch {
            val list = listId.value
            val ids = library.chapterIds(list)
            library.setReadMany(list, ids, read)
            if (read) auto.onMarkedRead(list, ids)
        }
    }

    // Saving chapters to the device: one, the picked ones, or all or the unread ones in the order they are read, and taking them off again.
    fun onDownload(chapterId: Long) {
        scope.launch { downloads.queue(listId.value, listOf(chapterId)) }
    }

    fun onRemoveDownload(chapterId: Long) {
        scope.launch { downloads.remove(listOf(chapterId)) }
    }

    fun onDownloadSelected() {
        val picked = look.value.selected
        val ordered = state().ordered.map { it.id }.filter { it in picked }
        scope.launch {
            downloads.queue(listId.value, ordered)
            onClearSelection()
        }
    }

    fun onRemoveSelectedDownloads() {
        val picked = look.value.selected.toList()
        scope.launch {
            downloads.remove(picked)
            onClearSelection()
        }
    }

    fun onDownloadAll(unreadOnly: Boolean) {
        val ids = state().reading.filter { !unreadOnly || !it.read }.map { it.id }
        scope.launch { downloads.queue(listId.value, ids) }
    }

    fun onRemoveAllDownloads() {
        val ids = state().downloads.keys.toList()
        scope.launch { downloads.remove(ids) }
    }

    fun onSelect(chapterId: Long) = look.update {
        it.copy(selected = if (chapterId in it.selected) it.selected - chapterId else it.selected + chapterId)
    }

    fun onClearSelection() = look.update { it.copy(selected = emptySet()) }

    // Picks every chapter shown, or none when all of them are picked already.
    fun onSelectAll() {
        val ids = state().shown.map { it.id }.toSet()
        look.update { it.copy(selected = if (it.selected == ids) emptySet() else ids) }
    }

    fun onInvertSelection() {
        val ids = state().shown.map { it.id }.toSet()
        look.update { it.copy(selected = ids - it.selected) }
    }

    fun onMarkSelected(read: Boolean) {
        val ids = look.value.selected
        scope.launch {
            library.setReadMany(listId.value, ids, read)
            if (read) auto.onMarkedRead(listId.value, ids)
            onClearSelection()
        }
    }

    fun onBookmarkSelected(bookmarked: Boolean) {
        val ids = look.value.selected
        scope.launch {
            library.setBookmarkedMany(ids, bookmarked)
            onClearSelection()
        }
    }

    // Marks everything before the highest chapter picked as read.
    fun onMarkBelowRead() {
        val chapters = state().chapters
        val picked = chapters.filter { it.id in look.value.selected }
        val top = picked.maxOfOrNull { it.number } ?: return
        scope.launch {
            library.markBelowRead(listId.value, top)
            auto.onMarkedRead(listId.value, chapters.filter { it.number < top }.map { it.id })
            onClearSelection()
        }
    }
}
