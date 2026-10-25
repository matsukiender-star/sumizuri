package com.sumizuri.app.ui.kit.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.motion.entrance
import com.sumizuri.app.ui.theme.LocalCompactLists
import com.sumizuri.app.ui.theme.LocalThemeSpec

// A card with one row of content, something on the left, two lines of text and something on the right.
@Composable
fun AppListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    subtitleLines: Int = 1,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    AppCard(modifier.fillMaxWidth().entrance(), onClick) {
        Row(
            Modifier.padding(
                horizontal = 12.dp,
                vertical = (if (LocalCompactLists.current) 4f else 10f)
                    .times(
                        LocalThemeSpec.current.options.layout
                            .let { it.density.scale * it.spacing },
                    ).dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            leading?.invoke()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = subtitleLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trailing?.invoke()
        }
    }
}

// The arrow at the end of a row that opens a page of its own.
@Composable
fun ListChevron(tint: Color = MaterialTheme.colorScheme.outline) {
    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = tint)
}
