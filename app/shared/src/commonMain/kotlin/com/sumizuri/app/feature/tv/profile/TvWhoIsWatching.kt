package com.sumizuri.app.feature.tv.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.sumizuri.app.data.profile.LocalProfiles
import com.sumizuri.app.data.profile.Profile
import com.sumizuri.app.feature.profile.ProfileAvatar
import com.sumizuri.app.feature.tv.onboarding.firstFocus
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.profile_new
import com.sumizuri.app.shared.resources.suki_tv_manage
import com.sumizuri.app.shared.resources.tv_profile_done
import com.sumizuri.app.shared.resources.tv_profile_title
import com.sumizuri.app.ui.motion.AppMotion
import com.sumizuri.app.ui.motion.motionMs
import com.sumizuri.app.ui.theme.background.ambientBackground
import com.sumizuri.app.ui.tv.LocalTv
import com.sumizuri.app.ui.tv.TvButton
import com.sumizuri.app.ui.tv.TvOverlay
import com.sumizuri.app.ui.tv.tvCard
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.stringResource

// How big a profile is in the picker.
private val Face = 132.dp

// Asks for the name of a profile just made on a TV, over the whole app, since making it opens it at its library.
internal object NewProfileName {
    val ask = MutableStateFlow(false)
}

// The name panel over everything once a new profile has opened.
@Composable
internal fun TvNewProfileName() {
    val asking by NewProfileName.ask.collectAsStateWithLifecycle()
    if (!asking) return
    Box(Modifier.fillMaxSize().ambientBackground()) {
        TvProfilePanel(TvPanel.Name, onClose = { NewProfileName.ask.value = false }, onOpenPage = {})
    }
}

// Whether the person already picked who is watching since the app started, so switching to that profile does not ask again.
internal object WhoAsked {
    var done = false

    // Asks again on the next frame, even with one profile, for trying the screen while making the app.
    var preview by mutableStateOf(false)
}

// When a TV with two or more profiles starts, it first asks who is watching, with each profile as a big round picture in one row and the one used last in focus. OK opens that profile. A TV with one profile goes straight to its library. As on a streaming app, a profile can be added from here, and Manage profiles turns OK into changing one, its name, picture or deleting it, without going through the profile page.
@Composable
internal fun TvWhoIsWatching() {
    if (!LocalTv.current) return
    val profiles = LocalProfiles.current
    val registry by profiles.state.collectAsStateWithLifecycle()
    var shown by remember { mutableStateOf(!WhoAsked.done && registry.profiles.size >= 2) }
    var managing by remember { mutableStateOf(false) }
    // The profile being changed, or a new one when adding, with the editor over the picker.
    var editing by remember { mutableStateOf<Profile?>(null) }
    var adding by remember { mutableStateOf(false) }
    SideEffect { WhoAsked.done = true }
    LaunchedEffect(WhoAsked.preview) {
        if (WhoAsked.preview) {
            shown = true
            WhoAsked.preview = false
        }
    }
    TvNewProfileName()
    AnimatedVisibility(shown, enter = fadeIn(tween(0)), exit = fadeOut(tween(motionMs(AppMotion.Page)))) {
        TvOverlay(Modifier.ambientBackground()) {
            if (editing != null || adding) {
                TvProfileEditor(editing) {
                    editing = null
                    adding = false
                }
                return@TvOverlay
            }
            NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), isBackEnabled = managing) {
                managing = false
            }
            val first = firstFocus()
            Column(
                Modifier.fillMaxSize().padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(Res.string.tv_profile_title), fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    Modifier.padding(top = 44.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                ) {
                    registry.profiles.forEach { profile ->
                        Pick(
                            profile,
                            managing,
                            if (profile.id ==
                                registry.activeId
                            ) {
                                Modifier.focusRequester(first)
                            } else {
                                Modifier
                            },
                        ) {
                            if (managing) {
                                editing = profile
                            } else {
                                shown = false
                                if (profile.id != registry.activeId) profiles.switchTo(profile.id)
                            }
                        }
                    }
                    AddPick { adding = true }
                }
                TvButton(
                    stringResource(if (managing) Res.string.tv_profile_done else Res.string.suki_tv_manage),
                    { managing = !managing },
                    Modifier.padding(top = 36.dp),
                    main = false,
                )
            }
        }
    }
}

// One profile as a big round picture with its name under it.
@Composable
private fun Pick(profile: Profile, managing: Boolean, modifier: Modifier, onPick: () -> Unit) {
    Column(Modifier.width(Face + 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier.size(Face + 12.dp).tvCard(onPick, CircleShape).padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            ProfileAvatar(profile.name, profile.avatar, Face)
            // While managing, a pencil over every picture says OK changes it.
            if (managing) {
                Box(
                    Modifier.size(Face).background(Color.Black.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.Edit, null, Modifier.size(40.dp), tint = Color.White) }
            }
        }
        Label(profile.name)
    }
}

// The way to add a profile, as the last picture in the row.
@Composable
private fun AddPick(onPick: () -> Unit) {
    Column(Modifier.width(Face + 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(Face + 12.dp).tvCard(onPick, CircleShape).padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(Face).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Add, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) }
        }
        Label(stringResource(Res.string.profile_new))
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        Modifier.fillMaxWidth().padding(top = 14.dp),
        fontSize = 20.sp,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}
