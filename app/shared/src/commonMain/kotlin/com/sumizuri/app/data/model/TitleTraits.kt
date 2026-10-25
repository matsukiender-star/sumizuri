package com.sumizuri.app.data.model

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.kind_anime
import com.sumizuri.app.shared.resources.kind_manga
import com.sumizuri.app.shared.resources.kind_novel
import com.sumizuri.app.shared.resources.status_cancelled
import com.sumizuri.app.shared.resources.status_completed
import com.sumizuri.app.shared.resources.status_hiatus
import com.sumizuri.app.shared.resources.status_ongoing
import com.sumizuri.app.shared.resources.status_unknown
import org.jetbrains.compose.resources.StringResource

// The code is what the database stores, so a code is never reused or reordered.
enum class Kind(val code: Int, val label: StringResource) {
    Manga(0, Res.string.kind_manga),
    Novel(1, Res.string.kind_novel),
    Anime(2, Res.string.kind_anime),
    ;

    companion object {
        fun fromCode(code: Int): Kind = entries.firstOrNull { it.code == code } ?: Manga
    }
}

// The code is what the database stores, so a code is never reused or reordered.
enum class Status(val code: Int, val label: StringResource) {
    Unknown(0, Res.string.status_unknown),
    Ongoing(1, Res.string.status_ongoing),
    Completed(2, Res.string.status_completed),
    Hiatus(3, Res.string.status_hiatus),
    Cancelled(4, Res.string.status_cancelled),
    ;

    companion object {
        fun fromCode(code: Int): Status = entries.firstOrNull { it.code == code } ?: Unknown
    }
}

// Whether the source of a saved title can be asked now, or is switched off, or is not installed at all.
enum class SourceState { Ready, Off, Missing }

// Looks a source up in the map of installed sources and whether each is on.
fun sourceStateOf(sourceId: String, on: Map<String, Boolean>): SourceState = when (on[sourceId]) {
    true -> SourceState.Ready
    false -> SourceState.Off
    null -> SourceState.Missing
}
