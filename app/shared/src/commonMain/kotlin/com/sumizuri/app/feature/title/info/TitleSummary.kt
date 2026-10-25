package com.sumizuri.app.feature.title.info

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.less
import com.sumizuri.app.shared.resources.more
import org.jetbrains.compose.resources.stringResource

private const val ShortLines = 3

// The description cut to a few lines. A tap opens it in full, or when onMore is given, goes to where the whole description is.
@Composable
internal fun TitleSummary(
    text: String,
    modifier: Modifier = Modifier,
    lines: Int = ShortLines,
    onMore: (() -> Unit)? = null,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier.clickable { if (onMore != null) onMore() else open = !open }.animateContentSize()) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (open) Int.MAX_VALUE else lines,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (open) stringResource(Res.string.less) else stringResource(Res.string.more),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
