package com.sumizuri.app.feature.player.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.player_skip
import com.sumizuri.app.shared.resources.player_skip_ending
import com.sumizuri.app.shared.resources.player_skip_intro
import com.sumizuri.app.shared.resources.player_up_next
import com.sumizuri.app.source.model.TimeRange
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.art.InkTonalButton
import org.jetbrains.compose.resources.stringResource

// The skip button over the video: a dark tonal button with an icon and a word.
@Composable
private fun SkipButton(
    label: String,
    onClick: () -> Unit,
    icon: ImageVector = Icons.Rounded.FastForward,
    alpha: Float = 0.55f,
) {
    InkTonalButton(
        onClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color.Black.copy(alpha = alpha),
            contentColor = Color.White,
        ),
    ) {
        Icon(icon, null, Modifier.size(20.dp))
        Text(label, Modifier.padding(start = 8.dp))
    }
}

// The button that skips a fixed stretch ahead, for getting past an opening.
@Composable
internal fun PlayerSkipButton(seconds: Int, onClick: () -> Unit) {
    SkipButton(stringResource(Res.string.player_skip, seconds), onClick)
}

// The button that skips the intro or the ending while it plays, a few seconds before it starts.
@Composable
internal fun PlayerMarkerButton(
    positionMs: Long,
    durationMs: Long,
    intro: TimeRange?,
    outro: TimeRange?,
    onSkip: (ActiveMarker) -> Unit,
) {
    val marker = PlayerMarkers.active(positionMs, intro, outro, durationMs)
    AnimatedVisibility(marker != null, enter = fadeIn(), exit = fadeOut()) {
        // The last marker is kept while the button fades out.
        val kept = marker ?: return@AnimatedVisibility
        SkipButton(
            stringResource(
                if (kept.kind ==
                    MarkerKind.Intro
                ) {
                    Res.string.player_skip_intro
                } else {
                    Res.string.player_skip_ending
                },
            ),
            { onSkip(kept) },
            alpha = 0.6f,
        )
    }
}

// The button for the next episode, which shows in the last minute and a half of an episode that has one after it.
@Composable
internal fun PlayerUpNextButton(hasNext: Boolean, positionMs: Long, durationMs: Long, onClick: () -> Unit) {
    AnimatedVisibility(
        PlayerGestureMath.showUpNext(hasNext, positionMs, durationMs),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        InkButton(onClick) {
            Icon(Icons.Rounded.SkipNext, null)
            Text(stringResource(Res.string.player_up_next), Modifier.padding(start = 8.dp))
        }
    }
}
