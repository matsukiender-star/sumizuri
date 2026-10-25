package com.sumizuri.app.feature.theme.panel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
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
import androidx.compose.ui.unit.dp
import com.sumizuri.app.feature.theme.ThemeEditorState
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.editor_shape_large
import com.sumizuri.app.shared.resources.editor_shape_small
import com.sumizuri.app.shared.resources.editor_shape_style
import com.sumizuri.app.shared.resources.editor_shape_summary
import com.sumizuri.app.ui.kit.setting.ChoiceSetting
import com.sumizuri.app.ui.theme.spec.AppThemeSpec
import com.sumizuri.app.ui.theme.spec.ShapeSpec
import com.sumizuri.app.ui.theme.spec.ShapeStyle
import com.sumizuri.app.ui.theme.spec.ThemeShapes
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

// The corners of one kind of piece, which pick reads from the corners of the theme and put writes back. Folded it is one row with a drawing of the corners and their size, and a tap opens how they are cut and how large the large and the small corners are. A pill has no size to choose and only the leaf cut has a small corner.
@Composable
internal fun ShapeEditor(
    state: ThemeEditorState,
    label: StringResource,
    pick: (ThemeShapes) -> ShapeSpec,
    put: (ThemeShapes, ShapeSpec) -> ThemeShapes,
    startOpen: Boolean = false,
) {
    var open by remember { mutableStateOf(startOpen) }
    val spec = pick(state.draft.shapes)
    val colors = MaterialTheme.colorScheme
    fun sized(edit: (ShapeSpec, Float) -> ShapeSpec): (
        AppThemeSpec,
        Float,
    ) -> AppThemeSpec =
        { theme, value -> theme.copy(shapes = put(theme.shapes, edit(pick(theme.shapes), value))) }
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable { open = !open }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(width = 30.dp, height = 22.dp).border(2.dp, colors.primary, spec.toShape()))
            Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(
                if (spec.style ==
                    ShapeStyle.Pill
                ) {
                    choiceLabel(spec.style)
                } else {
                    stringResource(
                        Res.string.editor_shape_summary,
                        choiceLabel(spec.style),
                        spec.large.roundToInt(),
                    )
                },
                style = MaterialTheme.typography.labelMedium,
                color = colors.outline,
            )
            Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = colors.outline)
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(start = 4.dp, bottom = 8.dp)) {
                ChoiceSetting(Res.string.editor_shape_style, ShapeStyle.entries, spec.style, {
                    choiceLabel(it)
                }, icon = null) { style ->
                    state.shapes {
                        put(
                            it,
                            spec.copy(
                                style = style,
                                small = if (style == ShapeStyle.Asymmetric &&
                                    spec.small == 0f
                                ) {
                                    4f
                                } else {
                                    spec.small
                                },
                            ),
                        )
                    }
                }
                if (spec.style != ShapeStyle.Pill) {
                    ThemeSlider(state, Res.string.editor_shape_large, spec.large, 0f..40f, {
                        "${it.roundToInt()} dp"
                    }, sized { shape, value -> shape.copy(large = value) })
                }
                if (spec.style == ShapeStyle.Asymmetric) {
                    ThemeSlider(state, Res.string.editor_shape_small, spec.small, 0f..40f, {
                        "${it.roundToInt()} dp"
                    }, sized { shape, value -> shape.copy(small = value) })
                }
            }
        }
    }
}
