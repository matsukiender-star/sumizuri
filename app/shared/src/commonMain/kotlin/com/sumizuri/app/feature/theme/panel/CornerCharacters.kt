package com.sumizuri.app.feature.theme.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumizuri.app.feature.theme.ThemeEditorState
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.editor_border_weight
import com.sumizuri.app.shared.resources.editor_corners
import com.sumizuri.app.ui.kit.card.AppSectionLabel
import com.sumizuri.app.ui.theme.spec.ShapeCharacter
import com.sumizuri.app.ui.theme.spec.shapesOf
import kotlin.math.roundToInt

// The character of every corner at once, each drawn as a small card in its own corners so the choice is seen and not read, and the weight of the hairlines around pieces.
@Composable
internal fun CornerCharacters(state: ThemeEditorState) {
    val shapes = state.draft.shapes
    // The character the corners have now, or none when a piece was changed on its own.
    val current = remember(shapes) { ShapeCharacter.entries.firstOrNull { shapesOf(it, shapes) == shapes } }
    val colors = MaterialTheme.colorScheme
    Column {
        AppSectionLabel(Res.string.editor_corners)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (character in ShapeCharacter.entries) {
                val chosen = character == current
                val tile = MaterialTheme.shapes.medium
                val sample = remember(character) { shapesOf(character).card.toShape() }
                Column(
                    Modifier
                        .weight(1f)
                        .clip(tile)
                        .background(if (chosen) colors.primary.copy(alpha = 0.12f) else colors.surfaceContainerHighest.copy(alpha = 0.5f))
                        .border(if (chosen) 2.dp else 1.dp, if (chosen) colors.primary else colors.outlineVariant, tile)
                        .clickable { state.shapes { shapesOf(character, it) } }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(width = 40.dp, height = 30.dp).border(2.dp, if (chosen) colors.primary else colors.onSurfaceVariant, sample))
                    Text(choiceLabel(character), style = MaterialTheme.typography.labelMedium, fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
        ThemeSlider(state, Res.string.editor_border_weight, shapes.borderWidth, 0f..4f, { "${(it * 10).roundToInt() / 10f} dp" }) { theme, value -> theme.copy(shapes = theme.shapes.copy(borderWidth = value)) }
    }
}
