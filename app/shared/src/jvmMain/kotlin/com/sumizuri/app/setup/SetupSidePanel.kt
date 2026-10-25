package com.sumizuri.app.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.sumizuri.app.BuildInfo
import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.core.log.FileLogSink
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.setup_close
import com.sumizuri.app.shared.resources.setup_fact_version
import com.sumizuri.app.shared.resources.setup_minimize
import com.sumizuri.app.shared.resources.setup_step_done
import com.sumizuri.app.shared.resources.setup_step_folder
import com.sumizuri.app.shared.resources.setup_step_install
import com.sumizuri.app.shared.resources.setup_step_library
import com.sumizuri.app.shared.resources.setup_step_remove
import com.sumizuri.app.shared.resources.setup_step_removed
import com.sumizuri.app.shared.resources.setup_step_shortcuts
import com.sumizuri.app.shared.resources.setup_step_welcome
import com.sumizuri.app.shared.resources.setup_title
import com.sumizuri.app.ui.kit.art.AnimatedLogo
import com.sumizuri.app.ui.kit.art.rise
import com.sumizuri.app.ui.kit.window.appIcon
import com.sumizuri.app.ui.theme.SumizuriTheme
import com.sumizuri.app.ui.theme.ThemeMode
import com.sumizuri.app.ui.theme.background.ambientBackground
import com.sumizuri.app.ui.theme.spec.ThemePresets
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import java.io.File
import kotlin.system.exitProcess
import java.awt.Window as AwtWindow

@Composable
internal fun SidePanel(model: SetupModel) {
    Box(Modifier.fillMaxSize().clipToBounds().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))) {
        Column(Modifier.fillMaxSize().padding(28.dp)) {
            AnimatedLogo(60.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                "Sumizuri",
                Modifier.rise(1),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(Res.string.setup_fact_version, model.version),
                Modifier.rise(2),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.height(34.dp))
            StepRail(model)
        }
    }
}

// The steps of the way the person goes, the one they are on drawn out as a bar, and the ones behind them ticked.
@Composable
private fun StepRail(model: SetupModel) {
    val rail = model.rail
    val on = when (model.step) {
        SetupStep.Failed -> SetupStep.Working
        SetupStep.Working -> if (model.mode == SetupMode.Uninstall) SetupStep.Remove else SetupStep.Working
        else -> model.step
    }
    val current = rail.indexOf(on).coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rail.forEachIndexed { index, step ->
            key(step) {
                val state = when {
                    index < current -> PhaseState.Done
                    index == current -> PhaseState.Running
                    else -> PhaseState.Waiting
                }
                RailRow(step, state, Modifier.rise(3 + index))
            }
        }
    }
}

@Composable
private fun RailRow(step: SetupStep, state: PhaseState, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val active = state == PhaseState.Running
    val width by animateDpAsState(
        if (active) 22.dp else 8.dp,
        spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "dot",
    )
    val dot by animateColorAsState(
        if (state ==
            PhaseState.Waiting
        ) {
            colors.outline.copy(alpha = 0.55f)
        } else {
            colors.primary
        },
        label = "dot color",
    )
    val faded = colors.onSurfaceVariant.copy(alpha = if (state == PhaseState.Done) 0.85f else 0.6f)
    val text by animateColorAsState(if (active) colors.onSurface else faded, label = "text")
    Row(modifier.height(34.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(32.dp), contentAlignment = Alignment.CenterStart) {
            AnimatedContent(
                state == PhaseState.Done,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = 0.5f)) + fadeIn()) togetherWith
                        (scaleOut() + fadeOut())
                },
                label = "mark",
            ) { done ->
                if (done) {
                    Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = dot)
                } else {
                    Box(Modifier.size(width, 8.dp).clip(CircleShape).background(dot))
                }
            }
        }
        Text(
            stringResource(step.label()),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = text,
        )
    }
}

private fun SetupStep.label(): StringResource = when (this) {
    SetupStep.Welcome -> Res.string.setup_step_welcome
    SetupStep.Folder -> Res.string.setup_step_folder
    SetupStep.Library -> Res.string.setup_step_library
    SetupStep.Shortcuts -> Res.string.setup_step_shortcuts
    SetupStep.Working, SetupStep.Failed -> Res.string.setup_step_install
    SetupStep.Done -> Res.string.setup_step_done
    SetupStep.Remove -> Res.string.setup_step_remove
    SetupStep.Removed -> Res.string.setup_step_removed
}
