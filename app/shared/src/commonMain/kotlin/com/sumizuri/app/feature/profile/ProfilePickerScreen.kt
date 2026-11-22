package com.sumizuri.app.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumizuri.app.StartShown
import com.sumizuri.app.data.AppContainer
import com.sumizuri.app.data.profile.Profile
import com.sumizuri.app.data.profile.ProfileHost
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.suki_profile_picker_subtitle
import com.sumizuri.app.shared.resources.suki_profile_picker_title
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import com.sumizuri.app.ui.theme.SumizuriTheme
import com.sumizuri.app.ui.theme.ThemeMode
import com.sumizuri.app.ui.theme.spec.ThemePresets
import org.jetbrains.compose.resources.stringResource

// Holds the startup profile gate outside key(container) so switching profiles never reopens it.
@Composable
internal fun StartupProfileGate(
    profiles: ProfileHost,
    container: AppContainer,
    windowBar: (@Composable () -> Unit)?,
    content: @Composable () -> Unit,
) {
    // Freeze the startup count so creating a second profile during the session does not trigger the picker.
    var open by remember { mutableStateOf(profiles.state.value.profiles.size >= 2) }
    var switchingTo by remember { mutableStateOf<Long?>(null) }
    var previousContainer by remember { mutableStateOf<AppContainer?>(null) }
    val state by profiles.state.collectAsStateWithLifecycle()

    // Wait until ProfileHost finishes opening the new container so the previous profile never flashes.
    LaunchedEffect(container, state.activeId, switchingTo) {
        val target = switchingTo ?: return@LaunchedEffect
        if (container !== previousContainer && state.activeId == target) {
            switchingTo = null
            previousContainer = null
            open = false
        }
    }

    if (!open) {
        content()
        return
    }

    SumizuriTheme(ThemeMode.System, ThemePresets.first(), oled = false, oledGlow = false) {
        StartShown(true)
        ProfilePickerScreen(
            profiles = state.profiles,
            switchingTo = switchingTo,
            windowBar = windowBar,
            onPick = { chosen ->
                if (switchingTo != null) return@ProfilePickerScreen
                if (chosen.id == state.activeId) {
                    open = false
                } else {
                    previousContainer = container
                    switchingTo = chosen.id
                    profiles.switchTo(chosen.id)
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfilePickerScreen(
    profiles: List<Profile>,
    switchingTo: Long?,
    windowBar: (@Composable () -> Unit)?,
    onPick: (Profile) -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            windowBar?.invoke()
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            stringResource(Res.string.suki_profile_picker_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            stringResource(Res.string.suki_profile_picker_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        for (profile in profiles) {
                            ProfilePickerTile(
                                profile = profile,
                                busy = switchingTo == profile.id,
                                enabled = switchingTo == null,
                                onClick = { onPick(profile) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePickerTile(profile: Profile, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.width(136.dp),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerHigh,
        border = BorderStroke(1.dp, if (busy) colors.primary else colors.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                ProfileAvatar(profile.name, profile.avatar, 84.dp)
                if (busy) {
                    Surface(
                        Modifier.fillMaxSize(),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = colors.scrim.copy(alpha = 0.45f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            InkCircularProgressIndicator(Modifier.size(32.dp))
                        }
                    }
                }
            }
            Text(
                profile.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
