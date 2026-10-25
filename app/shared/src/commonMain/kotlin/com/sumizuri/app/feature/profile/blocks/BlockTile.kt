package com.sumizuri.app.feature.profile.blocks

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.blocks.BlockView
import com.sumizuri.app.feature.profile.ShownBlock
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.blocks_add
import com.sumizuri.app.ui.kit.card.AppCard
import org.jetbrains.compose.resources.stringResource

// One block. A part of the profile as it was sits on the page as before, and any other view sits on a card with the title of its script.
@Composable
internal fun BlockTile(item: ShownBlock, env: BlockEnv, fixedHeight: Boolean, modifier: Modifier = Modifier) {
    val fill = if (fixedHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth()
    Box(modifier) {
        if (item.view is BlockView.Classic) {
            BlockViewContent(item.view, env, fill)
        } else {
            AppCard(fill) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp).clipToBounds(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (item.title.isNotBlank()) {
                        Text(
                            item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BlockViewContent(item.view, env, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// The tile at the end of the grid that adds a block.
@Composable
internal fun AddBlockTile(onAdd: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .border(1.5.dp, scheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable(onClick = onAdd)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
    ) {
        Icon(Icons.Outlined.Add, null, tint = scheme.primary)
        Text(stringResource(Res.string.blocks_add), style = MaterialTheme.typography.labelLarge, color = scheme.primary)
    }
}
