package com.sumizuri.app.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.setup_cancel
import com.sumizuri.app.shared.resources.setup_close
import com.sumizuri.app.shared.resources.setup_copied
import com.sumizuri.app.shared.resources.setup_done_text
import com.sumizuri.app.shared.resources.setup_done_title
import com.sumizuri.app.shared.resources.setup_done_updated
import com.sumizuri.app.shared.resources.setup_failed_text
import com.sumizuri.app.shared.resources.setup_failed_title
import com.sumizuri.app.shared.resources.setup_open
import com.sumizuri.app.shared.resources.setup_phase_close
import com.sumizuri.app.shared.resources.setup_phase_copy
import com.sumizuri.app.shared.resources.setup_phase_finish
import com.sumizuri.app.shared.resources.setup_phase_old
import com.sumizuri.app.shared.resources.setup_phase_shortcuts
import com.sumizuri.app.shared.resources.setup_remove
import com.sumizuri.app.shared.resources.setup_remove_data
import com.sumizuri.app.shared.resources.setup_remove_data_hint
import com.sumizuri.app.shared.resources.setup_remove_text
import com.sumizuri.app.shared.resources.setup_remove_title
import com.sumizuri.app.shared.resources.setup_removed_kept
import com.sumizuri.app.shared.resources.setup_removed_text
import com.sumizuri.app.shared.resources.setup_removed_title
import com.sumizuri.app.shared.resources.setup_retry
import com.sumizuri.app.shared.resources.setup_tip_backup
import com.sumizuri.app.shared.resources.setup_tip_downloads
import com.sumizuri.app.shared.resources.setup_tip_search
import com.sumizuri.app.shared.resources.setup_tip_theme
import com.sumizuri.app.shared.resources.setup_tip_title
import com.sumizuri.app.shared.resources.setup_working_install
import com.sumizuri.app.shared.resources.setup_working_remove
import com.sumizuri.app.shared.resources.setup_working_update
import com.sumizuri.app.ui.kit.art.AnimatedLogo
import com.sumizuri.app.ui.kit.art.DrawnCheck
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import com.sumizuri.app.ui.kit.art.MainButton
import com.sumizuri.app.ui.kit.art.ProgressRing
import com.sumizuri.app.ui.kit.art.QuietButton
import com.sumizuri.app.ui.kit.art.rise
import com.sumizuri.app.ui.kit.setting.SettingsSection
import com.sumizuri.app.ui.kit.setting.SwitchSetting
import com.sumizuri.app.ui.text.byteSizeText
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun DonePage(model: SetupModel, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        DrawnCheck(Modifier.size(128.dp).padding(bottom = 4.dp))
        Text(
            stringResource(if (model.updating) Res.string.setup_done_updated else Res.string.setup_done_title),
            Modifier.rise(2),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(Res.string.setup_done_text, model.appFolder.path, model.dataFolder.path),
            Modifier.padding(top = 8.dp).rise(3),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.rise(5),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainButton(stringResource(Res.string.setup_open), {
                model.openWhenClosed()
                onClose()
            }, Icons.AutoMirrored.Outlined.OpenInNew)
            QuietButton(stringResource(Res.string.setup_close), onClose)
        }
    }
}

// What went wrong, with an icon that shakes once, the reason, and where the log is to look further.
@Composable
internal fun FailedPage(model: SetupModel, log: File, onClose: () -> Unit) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) { shake.animateTo(1f, tween(600)) }
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(8.dp))
        Icon(
            Icons.Outlined.ErrorOutline,
            null,
            Modifier.size(64.dp).graphicsLayer {
                translationX =
                    sin(shake.value * 6 * Math.PI).toFloat() * (1f - shake.value) * 14.dp.toPx()
            },
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(Res.string.setup_failed_title),
            Modifier.rise(1),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        SelectionContainer(Modifier.padding(top = 8.dp).rise(2)) {
            Text(
                stringResource(Res.string.setup_failed_text, model.failure.orEmpty(), log.path),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.rise(4), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (model.mode ==
                SetupMode.Install
            ) {
                MainButton(stringResource(Res.string.setup_retry), model::install, Icons.Rounded.Refresh)
            }
            QuietButton(stringResource(Res.string.setup_close), onClose)
        }
    }
}

// Asks before taking the app away, with the library kept unless the person turns on its removal.
@Composable
internal fun RemovePage(model: SetupModel, onClose: () -> Unit) {
    SetupPage(
        stringResource(Res.string.setup_remove_title),
        stringResource(Res.string.setup_remove_text),
        bottom = {
            MainButton(
                stringResource(Res.string.setup_remove),
                model::remove,
                Icons.Outlined.DeleteOutline,
                danger = true,
            )
            QuietButton(stringResource(Res.string.setup_cancel), onClose)
        },
    ) {
        Box(Modifier.rise(2)) {
            SettingsSection {
                padded {
                    SwitchSetting(
                        stringResource(Res.string.setup_remove_data),
                        model.removeData,
                        { model.removeData = it },
                        stringResource(Res.string.setup_remove_data_hint, model.dataFolder.path),
                        Icons.AutoMirrored.Outlined.LibraryBooks,
                    )
                }
            }
        }
        RunningNotice(model)
    }
}

// The farewell: the icon, a thank you, and where the library stayed when it was kept. Closing starts the removal.
@Composable
internal fun RemovedPage(model: SetupModel, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        AnimatedLogo(72.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(Res.string.setup_removed_title),
            Modifier.rise(1),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            stringResource(Res.string.setup_removed_text),
            Modifier.padding(top = 8.dp).rise(2),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!model.removeData) {
            Text(
                stringResource(Res.string.setup_removed_kept, model.dataFolder.path),
                Modifier.padding(top = 6.dp).rise(3),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.rise(4)) { MainButton(stringResource(Res.string.setup_close), onClose, icon = null) }
    }
}
