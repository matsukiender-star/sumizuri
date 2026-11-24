package com.sumizuri.app.data.model.library

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.display_continue_row
import com.sumizuri.app.shared.resources.display_history_row
import com.sumizuri.app.shared.resources.display_updates_row
import com.sumizuri.app.shared.resources.library_title
import com.sumizuri.app.shared.resources.shelf_style_cards
import com.sumizuri.app.shared.resources.shelf_style_grid
import com.sumizuri.app.shared.resources.shelf_style_list
import com.sumizuri.app.shared.resources.shelf_style_pages
import com.sumizuri.app.shared.resources.shelf_style_row
import com.sumizuri.app.shared.resources.suki_library_picks_row
import org.jetbrains.compose.resources.StringResource

// A part of the top of the library page, above the covers.
enum class LibrarySection(val label: StringResource) {
    Continue(Res.string.display_continue_row),
    Updates(Res.string.display_updates_row),
    History(Res.string.display_history_row),

    // The covers of the library, which can stand above or below the other parts.
    Library(Res.string.library_title),

    // Rows of more of the genre of what was seen last in the pinned sources and those used lately. They start under the covers, because above them they push the library down on a phone. Named apart from the old Picks, so an order saved with them on top lets them go down once.
    Recommendations(Res.string.suki_library_picks_row),
}

// How a part of the top of the library page shows its titles: in a row that scrolls sideways, in a grid, as short lines, or as low cards with a small cover in a row that scrolls sideways, which keeps the covers of the library close to the top.
enum class ShelfStyle(val label: StringResource) {
    Row(Res.string.shelf_style_row),
    Grid(Res.string.shelf_style_grid),
    List(Res.string.shelf_style_list),
    Cards(Res.string.shelf_style_cards),

    // Large cards with the page the person stopped on, for the titles they are reading.
    Pages(Res.string.shelf_style_pages),
}
