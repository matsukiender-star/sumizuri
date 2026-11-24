package com.sumizuri.app.feature.settings.data

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.download.downloadFolder
import com.sumizuri.app.data.profile.LocalProfiles
import com.sumizuri.app.data.storage.AppFolder
import com.sumizuri.app.data.storage.DownloadRoot
import com.sumizuri.app.data.storage.MoveProblem
import com.sumizuri.app.data.storage.pickFolder
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.suki_storage_busy
import com.sumizuri.app.shared.resources.suki_storage_card
import com.sumizuri.app.shared.resources.suki_storage_change
import com.sumizuri.app.shared.resources.suki_storage_downloads
import com.sumizuri.app.shared.resources.suki_storage_failed
import com.sumizuri.app.shared.resources.suki_storage_inside
import com.sumizuri.app.shared.resources.suki_storage_internal
import com.sumizuri.app.shared.resources.suki_storage_moving
import com.sumizuri.app.shared.resources.suki_storage_not_empty
import com.sumizuri.app.shared.resources.suki_storage_reset
import com.sumizuri.app.ui.kit.setting.ChoiceSetting
import com.sumizuri.app.ui.kit.setting.SettingIcon
import com.sumizuri.app.ui.kit.setting.SettingRow
import kotlinx.coroutines.launch
import okio.Path
import org.jetbrains.compose.resources.stringResource

// Where downloads, backups and updates go, for every profile of this device: any folder on a computer, the phone or one of its cards on a phone. Changing it moves what is there.
@Composable
internal fun DownloadPlaceRow(cards: List<Path>?) {
    val profiles = LocalProfiles.current
    val container = LocalAppContainer.current
    val chosen by DownloadRoot.chosen.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var moving by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var problem by remember { mutableStateOf<MoveProblem?>(null) }
    val title = stringResource(Res.string.suki_storage_downloads)

    fun change(to: Path?) {
        if (moving != null || to == chosen) return
        val registry = profiles.registry
        val all = registry.state.value.let { it.profiles + it.trash }
        moving = 0 to 0
        problem = null
        scope.launch {
            problem =
                DownloadRoot.change(
                    to,
                    all.map { registry.fileOf(it).toString() },
                    registry.profilesFolder.parent!!,
                ) { done, total ->
                    moving = done to total
                }
            moving = null
        }
    }

    val status = moving?.let { (done, total) -> stringResource(Res.string.suki_storage_moving, done, total) }
        ?: problem?.let { stringResource(it.text()) }
    if (cards != null) {
        Column {
            ChoiceSetting(title, listOf<Path?>(null) + cards, chosen, { place ->
                if (place ==
                    null
                ) {
                    stringResource(Res.string.suki_storage_internal)
                } else {
                    stringResource(
                        Res.string.suki_storage_card,
                        cards.indexOf(place) + 1,
                    )
                }
            }) { change(it) }
            status?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        return
    }
    // The folder the app writes in, so the person sees where to look for the files.
    val shown = chosen?.let { it / AppFolder }?.toString() ?: downloadFolder(container.databasePath).parent.toString()
    SettingRow(
        title,
        subtitle = status ?: shown,
        subtitleLines = 3,
        leading = { SettingIcon(Icons.Outlined.Download) },
        trailing = {
            Row {
                if (chosen !=
                    null
                ) {
                    TextButton(
                        { change(null) },
                        enabled = moving == null,
                    ) { Text(stringResource(Res.string.suki_storage_reset)) }
                }
                TextButton({ scope.launch { pickFolder(title, chosen)?.let(::change) } }, enabled = moving == null) {
                    Text(stringResource(Res.string.suki_storage_change))
                }
            }
        },
    )
}

private fun MoveProblem.text() = when (this) {
    MoveProblem.InsideData -> Res.string.suki_storage_inside
    MoveProblem.Busy -> Res.string.suki_storage_busy
    MoveProblem.NotEmpty -> Res.string.suki_storage_not_empty
    MoveProblem.Failed -> Res.string.suki_storage_failed
}
