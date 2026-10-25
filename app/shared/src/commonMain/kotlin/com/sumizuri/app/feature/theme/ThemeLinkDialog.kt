package com.sumizuri.app.feature.theme

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.cancel
import com.sumizuri.app.shared.resources.theme_link_add
import com.sumizuri.app.shared.resources.theme_link_bad
import com.sumizuri.app.shared.resources.theme_link_title
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.dialog.AppDialog
import com.sumizuri.app.ui.theme.spec.AppThemeSpec
import org.jetbrains.compose.resources.stringResource

// Asks whether to add a theme someone shared as a link, showing it drawn in its own colors. Adding keeps it as a theme of the person's own and uses it.
@Composable
fun ThemeLinkDialog(data: String, onDone: () -> Unit) {
    val container = LocalAppContainer.current
    val themes = rememberThemeModel()
    val theme = remember(data) { container.themes.fromLink(data) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    AppDialog(
        onDismissRequest = onDone,
        title = { Text(stringResource(Res.string.theme_link_title)) },
        text = {
            if (theme == null) {
                Text(stringResource(Res.string.theme_link_bad))
            } else {
                ThemeCard(
                    theme,
                    dark,
                    selected = false,
                    actions = emptyList(),
                    onClick = {},
                    modifier = Modifier.width(200.dp),
                )
            }
        },
        confirmButton = {
            if (theme != null) {
                InkButton({
                    themes.save(theme.copy(id = AppThemeSpec.CustomPrefix + newThemeId()))
                    onDone()
                }) { Text(stringResource(Res.string.theme_link_add)) }
            }
        },
        dismissButton = { TextButton(onDone) { Text(stringResource(Res.string.cancel)) } },
    )
}
