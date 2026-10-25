package com.sumizuri.app.ui.kit.window

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// The red of the close button when the pointer is over it.
private val CloseRed = Color(0xFFE5484D)

// One of the buttons at the right end of the window bar. The close button turns red under the pointer and the others turn a soft grey.
@Composable
internal fun WindowButton(icon: ImageVector, description: String, onClick: () -> Unit, danger: Boolean = false) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val fill = when {
        !hovered -> Color.Transparent
        danger -> CloseRed
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    }
    Box(
        Modifier
            .width(46.dp)
            .fillMaxHeight()
            .background(fill)
            .hoverable(source)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            description,
            Modifier.size(16.dp),
            tint = if (hovered &&
                danger
            ) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
