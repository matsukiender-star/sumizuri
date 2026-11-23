package com.sumizuri.app.feature.live

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sumizuri.app.playback.MiniPlayerControl
import com.sumizuri.app.playback.PlayerEngine
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.player_mini_short
import com.sumizuri.app.shared.resources.player_volume
import com.sumizuri.app.ui.kit.input.InkSlider
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

// The wheel of a mouse over a live channel turns its sound up or down.
internal fun Modifier.wheelVolume(engine: PlayerEngine): Modifier = pointerInput(engine) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            val mouse = event.changes.firstOrNull { it.type == PointerType.Mouse } ?: continue
            if (event.type == PointerEventType.Scroll) {
                val step = if (mouse.scrollDelta.y < 0f) 5 else -5
                engine.setVolume((engine.state.value.volume + step).coerceIn(0, 100))
            }
        }
    }
}

// Taps on a live channel: in the small player a tap plays or pauses and a double tap brings the player back, and in the full one a tap shows or hides the controls.
internal fun Modifier.liveTaps(
    small: Boolean,
    mini: MiniPlayerControl?,
    onToggle: () -> Unit,
    onPlayPause: () -> Unit,
): Modifier = pointerInput(small) {
    detectTapGestures(
        onTap = { if (small) onPlayPause() else onToggle() },
        onDoubleTap = if (small && mini != null) ({ _ -> mini.exit() }) else null,
    )
}

// The sound of a live channel: the speaker mutes or brings the sound back, the bar sets it.
@Composable
internal fun LiveVolume(volume: Int, onSet: (Int) -> Unit) {
    IconButton({ onSet(if (volume > 0) 0 else 100) }) {
        Icon(
            when {
                volume <= 0 -> Icons.AutoMirrored.Outlined.VolumeOff
                volume < 50 -> Icons.AutoMirrored.Outlined.VolumeDown
                else -> Icons.AutoMirrored.Outlined.VolumeUp
            },
            stringResource(Res.string.player_volume),
            tint = Color.White,
        )
    }
    InkSlider(volume.toFloat(), { onSet(it.roundToInt()) }, Modifier.width(96.dp), valueRange = 0f..100f)
}

// The button that shrinks the channel to a small player over the other windows, or to picture in picture on a phone.
@Composable
internal fun LiveMiniButton(mini: MiniPlayerControl, aspect: Float) {
    IconButton({ mini.enter(aspect) }) {
        Icon(Icons.Outlined.PictureInPictureAlt, stringResource(Res.string.player_mini_short), tint = Color.White)
    }
}
