package com.sumizuri.app.feature.theme.panel

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sumizuri.app.feature.theme.ThemeEditorState
import com.sumizuri.app.feature.theme.otherModeFrom
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.allStringResources
import com.sumizuri.app.shared.resources.editor_accent_from
import com.sumizuri.app.shared.resources.editor_colors_hint
import com.sumizuri.app.shared.resources.editor_fix_contrast
import com.sumizuri.app.shared.resources.editor_low_contrast
import com.sumizuri.app.shared.resources.editor_make_dark
import com.sumizuri.app.shared.resources.editor_make_light
import com.sumizuri.app.shared.resources.theme_dark
import com.sumizuri.app.shared.resources.theme_light
import com.sumizuri.app.ui.kit.art.InkOutlinedButton
import com.sumizuri.app.ui.kit.setting.ChoiceSetting
import com.sumizuri.app.ui.kit.setting.PartTabs
import com.sumizuri.app.ui.theme.SeenContrast
import com.sumizuri.app.ui.theme.contrastOf
import com.sumizuri.app.ui.theme.readableOn
import com.sumizuri.app.ui.theme.roleColor
import com.sumizuri.app.ui.theme.spec.AccentSource
import com.sumizuri.app.ui.theme.spec.AppThemeSpec
import com.sumizuri.app.ui.theme.spec.ThemeColors
import com.sumizuri.app.ui.theme.spec.toLong
import org.jetbrains.compose.resources.stringResource

// The colors that are drawn on the page and must stand out from it.
private val Accents = setOf("primary", "secondary", "tertiary", "error")

// The tab of colors: the mode the colors are for, which is also the one the preview shows, each color with what it is used for and a warning when it is hard to see, a picker that opens under the color tapped, and a way to make the other mode from this one.
@Composable
internal fun ColorsPanel(state: ThemeEditorState) {
    var open by remember { mutableStateOf<String?>(null) }
    val dark = state.dark
    val page = state.draft.roleColor(dark, "surface")

    // The theme with this color of the mode on show set, or taken back to the usual one with null.
    fun colored(theme: AppThemeSpec, role: String, color: Color?): AppThemeSpec {
        val value = color?.toLong()
        return if (dark) {
            theme.copy(
                dark = theme.dark.with(role, value),
            )
        } else {
            theme.copy(light = theme.light.with(role, value))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // The accent can come from the system or the time of day, which then stands in for the accent colors below.
        val from = state.draft.options.accentFrom
        ChoiceSetting(Res.string.editor_accent_from, AccentSource.entries, from, { choiceLabel(it) }) { picked ->
            state.options { it.copy(accentFrom = picked) }
        }
        PaletteBuilder(state)
        Text(
            stringResource(Res.string.editor_colors_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        PartTabs(listOf(false, true), dark, {
            stringResource(if (it) Res.string.theme_dark else Res.string.theme_light)
        }, Modifier.padding(vertical = 4.dp)) {
            state.dark =
                it
        }
        for (role in ThemeColors.Roles) {
            val color = state.draft.roleColor(dark, role)
            // The page and the tint behind chips sit close to the page on purpose, so only the accents are checked.
            val faint = role in Accents && contrastOf(color, page) < SeenContrast
            ColorRow(role, color, open == role, faint, onClick = {
                open = if (open ==
                    role
                ) {
                    null
                } else {
                    role
                }
            }, onFix = { state.change(colored(state.draft, role, readableOn(color, page))) })
            AnimatedVisibility(open == role) {
                // A picker of its own for each color and mode, so its marks start on that color.
                key(role, dark) {
                    ColorPicker(
                        color,
                        onTry = { state.tryOut(colored(state.draft, role, it)) },
                        onPick = { state.change(colored(state.draft, role, it)) },
                        onUsual = { state.change(colored(state.draft, role, null)) },
                    )
                }
            }
        }
        InkOutlinedButton({ state.change(state.draft.otherModeFrom(dark)) }, Modifier.padding(top = 8.dp)) {
            Icon(Icons.Outlined.AutoFixHigh, null, Modifier.size(18.dp))
            Text(
                stringResource(if (dark) Res.string.editor_make_light else Res.string.editor_make_dark),
                Modifier.padding(start = 8.dp),
            )
        }
    }
}

// A color of the theme: a round swatch, what the color is for and the six digits of it. A tap opens the picker under it. A color that is hard to see on the page says so, with a button that moves it just enough.
@Composable
private fun ColorRow(
    role: String,
    color: Color,
    open: Boolean,
    faint: Boolean,
    onClick: () -> Unit,
    onFix: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, colors.outlineVariant, CircleShape),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.allStringResources.getValue("editor_role_$role")),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(Res.allStringResources.getValue("editor_role_${role}_hint")),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.outline,
                )
            }
            Text("#" + hexOf(color), style = MaterialTheme.typography.labelMedium, color = colors.outline)
            Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = colors.outline)
        }
        if (faint) {
            Row(
                Modifier.padding(start = 52.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Outlined.WarningAmber, null, Modifier.size(16.dp), tint = colors.error)
                Text(
                    stringResource(Res.string.editor_low_contrast),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.error,
                )
                TextButton(onFix) { Text(stringResource(Res.string.editor_fix_contrast)) }
            }
        }
    }
}
