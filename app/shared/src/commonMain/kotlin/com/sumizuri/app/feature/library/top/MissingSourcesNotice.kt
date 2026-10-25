package com.sumizuri.app.feature.library.top

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.display_hide_row
import com.sumizuri.app.shared.resources.updates_hidden_action
import com.sumizuri.app.ui.kit.art.InkTonalButton
import org.jetbrains.compose.resources.stringResource

// Something a notice offers besides mending the sources, such as narrowing the library to the titles it is about.
class NoticeAction(val label: String, val onClick: () -> Unit)

// The one note about titles whose source is missing or off, the same on every page that has it: a tinted icon, what is wrong, and a button to the page where it is mended. A page can add a second action and a close.
@Composable
internal fun MissingSourcesNotice(
    text: String,
    onFix: () -> Unit,
    modifier: Modifier = Modifier,
    extra: NoticeAction? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier.fillMaxWidth(),
        MaterialTheme.shapes.medium,
        colors.surfaceContainerHigh,
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.45f)),
    ) {
        Column(Modifier.padding(start = 12.dp, top = 12.dp, end = 4.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(
                            40.dp,
                        ).background(
                            colors.primary.copy(alpha = 0.14f),
                            RoundedCornerShape(10.dp),
                        ).border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.LinkOff, null, Modifier.size(19.dp), tint = colors.primary) }
                Text(
                    text,
                    Modifier.weight(1f).padding(
                        end = if (onDismiss ==
                            null
                        ) {
                            8.dp
                        } else {
                            0.dp
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (onDismiss !=
                    null
                ) {
                    IconButton(
                        onDismiss,
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            stringResource(Res.string.display_hide_row),
                            Modifier.size(20.dp),
                        )
                    }
                }
            }
            // The buttons go on their own line under the words, so a phone does not squeeze the words into a narrow column.
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (extra != null) TextButton(extra.onClick) { Text(extra.label) }
                InkTonalButton(onFix) { Text(stringResource(Res.string.updates_hidden_action)) }
            }
        }
    }
}
