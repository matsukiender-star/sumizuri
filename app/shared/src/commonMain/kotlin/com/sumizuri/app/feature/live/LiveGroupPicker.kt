package com.sumizuri.app.feature.live

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.suki_live_all_groups
import com.sumizuri.app.shared.resources.suki_live_group_button
import com.sumizuri.app.shared.resources.suki_live_group_search
import com.sumizuri.app.shared.resources.suki_live_groups
import com.sumizuri.app.ui.kit.art.InkOutlinedButton
import com.sumizuri.app.ui.kit.input.AppSearchBar
import com.sumizuri.app.ui.kit.page.AppSheet
import org.jetbrains.compose.resources.stringResource

// The categories of a channel: a list can give several in one, split by semicolons, such as "Animation;Comedy".
fun groupsOf(group: String): List<String> = group.split(';').map { it.trim() }.filter { it.isNotEmpty() }

// The category the channels are filtered by, on a button that opens every category with a search, as a long row of chips cannot be reached with a mouse.
@Composable
internal fun LiveGroupButton(groups: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val all = stringResource(Res.string.suki_live_all_groups)
    InkOutlinedButton({ open = true }) {
        Text(
            stringResource(Res.string.suki_live_group_button, selected ?: all),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(Icons.Filled.ArrowDropDown, null)
    }
    if (!open) return
    val pick: (String?) -> Unit = {
        open = false
        onSelect(it)
    }
    AppSheet({ open = false }, spacing = 2.dp, title = stringResource(Res.string.suki_live_groups)) {
        var query by remember { mutableStateOf("") }
        AppSearchBar(
            query,
            { query = it },
            stringResource(Res.string.suki_live_group_search),
            Modifier.padding(bottom = 8.dp),
        )
        if (query.isBlank()) GroupRow(all, selected == null) { pick(null) }
        for (group in groups.filter { it.contains(query.trim(), ignoreCase = true) }) {
            GroupRow(group, group == selected) { pick(group) }
        }
    }
}

@Composable
private fun GroupRow(name: String, chosen: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(12.dp),
            ).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            name,
            Modifier.weight(1f),
            color = if (chosen) colors.primary else colors.onSurface,
            fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (chosen) Icon(Icons.Filled.Check, null, Modifier.size(20.dp), tint = colors.primary)
    }
}
