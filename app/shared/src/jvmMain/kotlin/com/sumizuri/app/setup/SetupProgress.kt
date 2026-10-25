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

// The install going on: a ring that fills with a number counting up inside it, and the parts of the work, each turning while it runs and ticked once it is done.
@Composable
internal fun WorkingPage(model: SetupModel) {
    val copied by model.copied.collectAsState()
    val removing = model.mode == SetupMode.Uninstall
    val progress = if (removing) 0f else model.progress()
    val title = when {
        removing -> Res.string.setup_working_remove
        model.updating -> Res.string.setup_working_update
        else -> Res.string.setup_working_install
    }
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(title),
            Modifier.rise(0),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(34.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            Box(Modifier.size(156.dp).rise(1), contentAlignment = Alignment.Center) {
                ProgressRing(progress, Modifier.fillMaxSize())
                if (!removing) {
                    val shown by animateFloatAsState(progress, tween(600), label = "percent")
                    Text(
                        "${(shown * 100).roundToInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                model.phases.keys.sortedBy { it.ordinal }.forEachIndexed { index, phase ->
                    PhaseRow(phase, model.phases[phase] ?: PhaseState.Waiting, Modifier.rise(2 + index))
                }
                AnimatedVisibility(
                    model.phases[SetupPhase.Copy] == PhaseState.Running,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        stringResource(
                            Res.string.setup_copied,
                            byteSizeText(copied.first),
                            byteSizeText(copied.second),
                        ),
                        Modifier.padding(start = 34.dp, top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (!removing) Tips(Modifier.rise(7))
    }
}

// What the app can do, one at a time, each sliding up over the one before every few seconds while the install runs.
@Composable
private fun Tips(modifier: Modifier = Modifier) {
    val tips =
        listOf(
            Res.string.setup_tip_search,
            Res.string.setup_tip_backup,
            Res.string.setup_tip_theme,
            Res.string.setup_tip_downloads,
        )
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4_500)
            index = (index + 1) % tips.size
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Lightbulb, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            stringResource(Res.string.setup_tip_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        AnimatedContent(index, transitionSpec = {
            (slideInVertically { it } + fadeIn()) togetherWith
                (slideOutVertically { -it } + fadeOut())
        }, label = "tip") { shown ->
            Text(stringResource(tips[shown]), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PhaseRow(phase: SetupPhase, state: PhaseState, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val tint by animateColorAsState(
        when (state) {
            PhaseState.Running -> colors.onSurface
            PhaseState.Done -> colors.onSurfaceVariant
            PhaseState.Waiting -> colors.outline
        },
        label = "phase",
    )
    Row(
        modifier.height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            AnimatedContent(state, transitionSpec = {
                (
                    scaleIn(spring(dampingRatio = 0.45f, stiffness = 400f)) +
                        fadeIn()
                    ) togetherWith
                    (scaleOut() + fadeOut())
            }, label = "mark") { shown ->
                when (shown) {
                    PhaseState.Waiting -> Box(Modifier.size(8.dp).clip(CircleShape).background(colors.outlineVariant))

                    PhaseState.Running -> InkCircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.primary,
                    )

                    PhaseState.Done -> Icon(
                        Icons.Rounded.CheckCircle,
                        null,
                        Modifier.size(22.dp),
                        tint = colors.primary,
                    )
                }
            }
        }
        Text(
            stringResource(phase.label()),
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            fontWeight = if (state ==
                PhaseState.Running
            ) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            },
        )
    }
}

private fun SetupPhase.label(): StringResource = when (this) {
    SetupPhase.Close -> Res.string.setup_phase_close
    SetupPhase.Old -> Res.string.setup_phase_old
    SetupPhase.Copy -> Res.string.setup_phase_copy
    SetupPhase.Shortcuts -> Res.string.setup_phase_shortcuts
    SetupPhase.Finish -> Res.string.setup_phase_finish
}
