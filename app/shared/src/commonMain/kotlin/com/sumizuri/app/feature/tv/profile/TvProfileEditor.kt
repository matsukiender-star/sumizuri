package com.sumizuri.app.feature.tv.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.sumizuri.app.data.profile.LocalProfiles
import com.sumizuri.app.data.profile.Profile
import com.sumizuri.app.feature.profile.PresetAvatarBadge
import com.sumizuri.app.feature.profile.PresetAvatars
import com.sumizuri.app.feature.profile.ProfileAvatar
import com.sumizuri.app.feature.tv.onboarding.firstFocus
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.cancel
import com.sumizuri.app.shared.resources.profile_create
import com.sumizuri.app.shared.resources.profile_delete
import com.sumizuri.app.shared.resources.profile_delete_title
import com.sumizuri.app.shared.resources.profile_edit
import com.sumizuri.app.shared.resources.profile_name
import com.sumizuri.app.shared.resources.profile_new
import com.sumizuri.app.shared.resources.profile_save
import com.sumizuri.app.shared.resources.suki_tv_photo
import com.sumizuri.app.shared.resources.suki_tv_picture
import com.sumizuri.app.shared.resources.tv_profile_numbered
import com.sumizuri.app.ui.tv.TvButton
import com.sumizuri.app.ui.tv.TvLinkRow
import com.sumizuri.app.ui.tv.TvTextEntry
import com.sumizuri.app.ui.tv.tvCard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// Making a profile, when none is given, or changing one: its name, its picture from the ones that come with the app or a photo sent from a phone, and deleting one that is not open. Nothing is kept until the first button, so Back or Cancel leave the profile as it was.
@Composable
internal fun TvProfileEditor(profile: Profile?, onClose: () -> Unit) {
    val profiles = LocalProfiles.current
    val registry by profiles.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val numbered = stringResource(Res.string.tv_profile_numbered, registry.profiles.size + 1)
    var name by remember { mutableStateOf(profile?.name ?: numbered) }
    var avatar by remember { mutableStateOf(profile?.avatar) }
    var typing by remember { mutableStateOf(false) }
    var photo by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    // A photo from the phone that is not kept in the end is removed again. Only pictures in the avatars folder are ever removed.
    val use: (String?) -> Unit = { next ->
        if (avatar != profile?.avatar) profiles.registry.deleteAvatar(avatar)
        avatar = next
    }
    val cancel = {
        use(profile?.avatar)
        onClose()
    }
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), isBackEnabled = !typing && !photo) {
        cancel()
    }
    if (photo) {
        TvPhotoDrop(onPhoto = {
            use(it)
            photo = false
        }, onClose = { photo = false })
        return
    }
    if (typing) {
        TvTextEntry(stringResource(Res.string.profile_name), null, name, false) {
            name = it.trim().ifEmpty { name }
            typing = false
        }
        return
    }
    val save = {
        if (!busy) {
            busy = true
            if (profile == null) {
                scope.launch {
                    profiles.create(name, avatar)
                    onClose()
                }
            } else {
                if (name != profile.name) profiles.rename(profile.id, name)
                if (avatar != profile.avatar) profiles.setAvatar(profile.id, avatar)
                onClose()
            }
        }
    }
    // The open profile and the only one stay. The first press asks, and the second moves it to the trash.
    val deletable = profile != null && profile.id != registry.activeId && registry.profiles.size > 1
    val first = firstFocus()
    Column(
        Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ProfileAvatar(name, avatar, 96.dp)
            Text(
                stringResource(if (profile == null) Res.string.profile_new else Res.string.profile_edit),
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        TvLinkRow(
            stringResource(Res.string.profile_name),
            name,
            { typing = true },
            Modifier.widthIn(max = 640.dp).focusRequester(first),
        )
        Text(
            stringResource(Res.string.suki_tv_picture),
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Choice(avatar == null, { use(null) }) { ProfileAvatar(name, null, Face) }
            PresetAvatars.forEach { preset ->
                Choice(avatar == preset.key, { use(preset.key) }) { PresetAvatarBadge(preset, Face) }
            }
            Choice(false, { photo = true }) {
                Box(
                    Modifier.size(Face).background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.QrCode2,
                        stringResource(Res.string.suki_tv_photo),
                        Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TvButton(stringResource(if (profile == null) Res.string.profile_create else Res.string.profile_save), save)
            TvButton(stringResource(Res.string.cancel), cancel, main = false)
            if (deletable && profile != null) {
                val label = if (deleting) {
                    stringResource(Res.string.profile_delete_title, profile.name)
                } else {
                    stringResource(Res.string.profile_delete)
                }
                TvButton(label, {
                    if (!deleting) {
                        deleting = true
                    } else if (!busy) {
                        busy = true
                        scope.launch {
                            profiles.delete(profile.id)
                            cancel()
                        }
                    }
                }, main = false)
            }
        }
    }
}

// How big a picture is in the row to pick from.
private val Face = 64.dp

// One picture to pick, in a round card that shows the one in use.
@Composable
private fun Choice(chosen: Boolean, onClick: () -> Unit, face: @Composable () -> Unit) {
    Box(
        Modifier.size(Face + 12.dp).tvCard(onClick, CircleShape, chosen = chosen).padding(6.dp),
        contentAlignment = Alignment.Center,
    ) { face() }
}
