package com.sumizuri.app.feature.player.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.player_unlock
import org.jetbrains.compose.resources.stringResource

// What stands over the picture while the controls are locked: nothing but a tap that brings up a button to unlock, which goes again after two seconds.
@Composable
internal fun PlayerLockedOverlay(hintVisible: Boolean, onTap: () -> Unit, onUnlock: () -> Unit) {
    Box(
        Modifier.fillMaxSize().clickable(
            interactionSource = MutableInteractionSource(),
            indication = null,
            onClick = onTap,
        ),
    ) {
        AnimatedVisibility(
            hintVisible,
            Modifier.align(Alignment.CenterStart).padding(start = 24.dp),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            FilledTonalIconButton(
                onUnlock,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Rounded.LockOpen, stringResource(Res.string.player_unlock))
            }
        }
    }
}
