package com.sumizuri.app.ui.kit.cover.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

// The ring on a cover while titles are being picked. It is filled with a check for a title that is picked and empty for one that is not.
@Composable
internal fun PickBadge(picked: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (picked) colors.primary else colors.background.copy(alpha = 0.6f))
            .border(2.dp, if (picked) colors.primary else colors.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (picked) Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = colors.onPrimary)
    }
}
