package com.sumizuri.app.ui.kit.chips

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.kit.art.StretchDot
import com.sumizuri.app.ui.kit.scroll.mouseDragScroll
import com.sumizuri.app.ui.motion.AppMotion
import com.sumizuri.app.ui.motion.motionMs

// A row of plain text tabs, the chosen one in the accent with a small bar stretching out under it. A tab can also be pressed and held.
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> FilterTabs(
    items: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: ((T) -> Unit)? = null,
    // Tabs that are only drawn, such as those of a preview, take no touches.
    enabled: Boolean = true,
) {
    val scroll = rememberScrollState()
    Row(
        modifier.mouseDragScroll(scroll, horizontal = true).horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for ((value, label) in items) {
            val chosen = value == selected
            val requester = remember { BringIntoViewRequester() }
            // The chosen tab scrolls into view, since a long row of tabs does not fit.
            LaunchedEffect(chosen) { if (chosen) requester.bringIntoView() }
            Column(
                Modifier
                    .bringIntoViewRequester(requester)
                    .clip(CircleShape)
                    .then(
                        if (enabled) {
                            Modifier.combinedClickable(
                                onLongClick = onLongPress?.let { press ->
                                    {
                                        press(value)
                                    }
                                },
                                onClick = { onSelect(value) },
                            )
                        } else {
                            Modifier
                        },
                    ).padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val textColor by animateColorAsState(
                    if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(motionMs(AppMotion.Fast)),
                    label = "tab text",
                )
                Text(
                    label,
                    color = textColor,
                    fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                )
                StretchDot(chosen, Modifier.padding(top = 5.dp), idle = 0.dp, wide = 22.dp, height = 4.dp)
            }
        }
    }
}
