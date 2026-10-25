package com.sumizuri.app.feature.title.chapter

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sumizuri.app.data.model.ChapterSort
import com.sumizuri.app.feature.title.DownloadCalls
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.chapter_menu_mark_all_read
import com.sumizuri.app.shared.resources.chapter_menu_mark_all_unread
import com.sumizuri.app.shared.resources.chapter_menu_more
import com.sumizuri.app.shared.resources.chapter_menu_select_all
import com.sumizuri.app.shared.resources.chapter_sort_date
import com.sumizuri.app.shared.resources.chapter_sort_number
import com.sumizuri.app.shared.resources.chapter_sort_source
import com.sumizuri.app.shared.resources.download_all
import com.sumizuri.app.shared.resources.download_delete_all
import com.sumizuri.app.shared.resources.download_unread
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// The three dots over the chapters, with the things to do with all of them: picking them, marking them, and saving them to the device or taking them off. How they are listed is in the chapter options.
@Composable
internal fun ChapterMenu(
    onMarkAll: (Boolean) -> Unit,
    onSelectAll: () -> Unit,
    downloads: DownloadCalls? = null,
    hasSaved: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    // The menu opens under the button, so the two are together in one box.
    Box {
        IconButton({ open = true }) { Icon(Icons.Outlined.MoreVert, stringResource(Res.string.chapter_menu_more)) }
        DropdownMenu(open, { open = false }) {
            @Composable
            fun item(text: String, action: () -> Unit) = DropdownMenuItem({ Text(text) }, {
                open = false
                action()
            })
            item(stringResource(Res.string.chapter_menu_select_all), onSelectAll)
            item(stringResource(Res.string.chapter_menu_mark_all_read)) { onMarkAll(true) }
            item(stringResource(Res.string.chapter_menu_mark_all_unread)) { onMarkAll(false) }
            // Saving to the device, the unread chapters or all of them, and taking every saved one off again.
            if (downloads != null) {
                HorizontalDivider()
                downloads.onDownloadAll?.let { start ->
                    item(stringResource(Res.string.download_unread)) { start(true) }
                    item(stringResource(Res.string.download_all)) { start(false) }
                }
                if (hasSaved) item(stringResource(Res.string.download_delete_all), downloads.onRemoveAll)
            }
        }
    }
}

// The name of each order, here and in the settings.
internal fun sortLabel(sort: ChapterSort): StringResource = when (sort) {
    ChapterSort.Source -> Res.string.chapter_sort_source
    ChapterSort.Number -> Res.string.chapter_sort_number
    ChapterSort.Date -> Res.string.chapter_sort_date
}
