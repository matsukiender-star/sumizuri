package com.sumizuri.app.feature.title.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.title_tab_about
import com.sumizuri.app.shared.resources.title_tab_chapters
import com.sumizuri.app.shared.resources.title_tab_episodes
import com.sumizuri.app.ui.kit.page.PageGutter
import com.sumizuri.app.ui.kit.setting.PartTabs
import org.jetbrains.compose.resources.stringResource

// The two parts of the phone page.
internal enum class TitleTab { Chapters, About }

// The two tabs, as buttons side by side like the parts of a page of the settings. In the pinned copy under the top bar they sit on the colour of the page so the list can pass under them, and in the list they let the cover show through.
@Composable
internal fun PhoneTabs(
    tab: TitleTab,
    onTab: (TitleTab) -> Unit,
    kind: Kind,
    count: Int,
    stuck: Boolean,
    modifier: Modifier = Modifier,
) {
    val page = MaterialTheme.colorScheme.background
    Column(modifier.fillMaxWidth().background(if (stuck) page else Color.Transparent)) {
        val chapters = "${stringResource(
            if (kind == Kind.Anime) Res.string.title_tab_episodes else Res.string.title_tab_chapters,
        )} · $count"
        val about = stringResource(Res.string.title_tab_about)
        PartTabs(
            TitleTab.entries,
            tab,
            { if (it == TitleTab.Chapters) chapters else about },
            Modifier.padding(horizontal = if (stuck) PageGutter else 0.dp, vertical = if (stuck) 8.dp else 4.dp),
            onSelect = onTab,
        )
        HorizontalDivider(color = if (stuck) MaterialTheme.colorScheme.outlineVariant else Color.Transparent)
    }
}
