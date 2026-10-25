package com.sumizuri.app.feature.profile.blocks

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.blocks.BlockTemplate
import com.sumizuri.app.data.blocks.ProfilePreset
import com.sumizuri.app.ui.kit.card.AppCard
import org.jetbrains.compose.resources.stringResource

// The parts of the profile as it came are drawn fainter in a sketch than the blocks made of scripts.
private val Sections =
    setOf(BlockTemplate.Figures, BlockTemplate.Year, BlockTemplate.Continue, BlockTemplate.Statistics)

// The presets two to a row, each a card with a sketch of where its blocks go, its name and what it holds. The chosen one has a ring around it.
@Composable
internal fun PresetPicker(selected: ProfilePreset?, onPick: (ProfilePreset) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (pair in ProfilePreset.entries.chunked(2)) {
            // The two cards of a row are as tall as each other.
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (preset in pair) {
                    val ring = if (preset ==
                        selected
                    ) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                    } else {
                        Modifier
                    }
                    AppCard(Modifier.weight(1f).fillMaxHeight().then(ring), onClick = { onPick(preset) }) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LayoutSketch(
                                preset.parts.map {
                                    SketchPart(it.w, it.sketchRows, it.template in Sections)
                                },
                                Modifier.fillMaxWidth().height(64.dp),
                            )
                            Text(
                                stringResource(preset.label),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                stringResource(preset.about),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (pair.size == 1) Column(Modifier.weight(1f)) {}
            }
        }
    }
}
