package com.sumizuri.app.data.model.library

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.style_comfortable
import com.sumizuri.app.shared.resources.style_compact
import com.sumizuri.app.shared.resources.style_compact_list
import com.sumizuri.app.shared.resources.style_cover
import com.sumizuri.app.shared.resources.style_list
import com.sumizuri.app.shared.resources.style_panorama
import org.jetbrains.compose.resources.StringResource

// How the titles of the library are laid out.
enum class LibraryDisplayStyle(val label: StringResource, val isGrid: Boolean) {
    // The cover with the name under it.
    ComfortableGrid(Res.string.style_comfortable, true),

    // The name over the bottom of the cover.
    CompactGrid(Res.string.style_compact, true),

    // The cover alone.
    CoverGrid(Res.string.style_cover, true),

    // A wide tile with the name over it, for covers cut to a band.
    PanoramaGrid(Res.string.style_panorama, true),

    // A row for each title with a small cover, the author and the status.
    List(Res.string.style_list, false),

    // A thin row for each title with only the name and the count.
    CompactList(Res.string.style_compact_list, false),
}
