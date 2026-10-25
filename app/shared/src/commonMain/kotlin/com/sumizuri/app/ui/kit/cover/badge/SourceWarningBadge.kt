package com.sumizuri.app.ui.kit.cover.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.source_unreachable
import org.jetbrains.compose.resources.stringResource

// A small mark for a cover whose source is not installed or is switched off. Hovering or pressing and holding it says so.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SourceWarningBadge(modifier: Modifier = Modifier) {
    val words = stringResource(Res.string.source_unreachable)
    TooltipBox(
        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        { PlainTooltip { Text(words) } },
        rememberTooltipState(),
        modifier,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.LinkOff, words, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
