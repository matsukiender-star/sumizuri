package com.sumizuri.app.data.model.library

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.filter_caught_up
import com.sumizuri.app.shared.resources.filter_completed
import com.sumizuri.app.shared.resources.filter_favorite
import com.sumizuri.app.shared.resources.filter_unread
import org.jetbrains.compose.resources.StringResource

// A one tap narrowing of the library, on top of the shelf and the search. Several can be on, and a title has to pass all of them.
enum class QuickFilter(val label: StringResource) {
    Unread(Res.string.filter_unread),
    CaughtUp(Res.string.filter_caught_up),
    Completed(Res.string.filter_completed),
    Favorite(Res.string.filter_favorite),
}
