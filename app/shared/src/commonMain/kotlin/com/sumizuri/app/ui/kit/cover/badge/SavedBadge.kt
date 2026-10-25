package com.sumizuri.app.ui.kit.cover.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.title_in_library
import org.jetbrains.compose.resources.stringResource

// A small filled mark for a cover that is already in the library.
@Composable
internal fun SavedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Check,
            stringResource(Res.string.title_in_library),
            Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
