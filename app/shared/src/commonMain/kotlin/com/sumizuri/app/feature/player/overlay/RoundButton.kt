package com.sumizuri.app.feature.player.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Replay30
import androidx.compose.material.icons.rounded.Replay5
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator

// A round button in the middle of the video, on the same surface as the panels, or in the main color for the one that matters most. One that cannot be used is dimmed.
@Composable
internal fun RoundButton(
    icon: ImageVector,
    size: Int,
    description: String?,
    onClick: (() -> Unit)?,
    primary: Boolean = false,
    busy: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick ?: {},
        modifier = Modifier.size(size.dp),
        enabled = onClick != null,
        shape = CircleShape,
        color = if (primary) scheme.primary else scheme.surfaceContainerHigh.copy(alpha = 0.85f),
        border = if (primary) null else BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(
            Modifier.size(size.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // A button whose action waits on the video turns its icon into a spinner, so the loading shows where the eye already is.
            if (busy) {
                InkCircularProgressIndicator(
                    Modifier.size((size / 3).dp),
                    color = if (primary) scheme.onPrimary else scheme.onSurface,
                    strokeWidth = 3.dp,
                )
                return@Column
            }
            Icon(
                icon,
                description,
                Modifier.size((size / 2).dp),
                tint = when {
                    onClick == null -> scheme.onSurface.copy(alpha = 0.35f)
                    primary -> scheme.onPrimary
                    else -> scheme.onSurface
                },
            )
        }
    }
}

// The icon for a jump of this many seconds, which has its own for five, ten and thirty.
internal fun jumpIcon(seconds: Int, forward: Boolean): ImageVector = when (seconds) {
    5 -> if (forward) Icons.Rounded.Forward5 else Icons.Rounded.Replay5
    10 -> if (forward) Icons.Rounded.Forward10 else Icons.Rounded.Replay10
    30 -> if (forward) Icons.Rounded.Forward30 else Icons.Rounded.Replay30
    else -> if (forward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind
}

// A speed as text: whole numbers without a decimal, and the rest with their own.
internal fun speedText(speed: Float): String = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()
