package com.sumizuri.app.feature.updates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.library.ReleasePlan
import com.sumizuri.app.data.library.UpcomingTitle
import com.sumizuri.app.data.text.dateText
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.updates_calendar
import com.sumizuri.app.shared.resources.updates_list
import com.sumizuri.app.shared.resources.updates_upcoming
import com.sumizuri.app.shared.resources.updates_upcoming_empty
import com.sumizuri.app.shared.resources.updates_upcoming_hint
import com.sumizuri.app.ui.kit.cover.CoverArt
import com.sumizuri.app.ui.kit.page.AppSheet
import com.sumizuri.app.ui.kit.setting.PartTabs
import org.jetbrains.compose.resources.stringResource

// The titles of the library whose next chapter is expected, the soonest first, each with the day it is due. The day is a guess from how often the title came out before, and a title with too few dated chapters has none.
@Composable
internal fun UpcomingSheet(onOpenTitle: (Long) -> Unit, onDismiss: () -> Unit) {
    val library = LocalAppContainer.current.library
    val upcoming by produceState<List<UpcomingTitle>?>(null) { value = library.upcoming() }
    val plans by produceState<List<ReleasePlan>?>(null) { value = library.releasePlans() }
    // The month of releases by default, and the list of what comes next as the other view.
    var calendar by remember { mutableStateOf(true) }
    AppSheet(onDismiss, spacing = 4.dp, title = stringResource(Res.string.updates_upcoming)) {
        PartTabs(listOf(true, false), calendar, {
            stringResource(if (it) Res.string.updates_calendar else Res.string.updates_list)
        }) { calendar = it }
        if (calendar) {
            plans?.let { ReleaseCalendar(it, onOpenTitle) }
            return@AppSheet
        }
        val list = upcoming
        when {
            list == null -> Unit

            list.isEmpty() -> Text(
                stringResource(Res.string.updates_upcoming_empty),
                color = MaterialTheme.colorScheme.outline,
            )

            else -> {
                Text(
                    stringResource(Res.string.updates_upcoming_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (item in list) {
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenTitle(item.title.id) }.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CoverArt(item.title, Modifier.width(40.dp), letterSize = 14)
                            Text(
                                item.title.name,
                                Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                dateText(item.at),
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
