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

// The size of the card the setup draws, the room round it for its shadow, and the width of the panel at its side.
private val CardSize = DpSize(880.dp, 580.dp)
private val ShadowRoom = 20.dp
private val SideWidth = 290.dp

@Suppress("FunctionName")
private interface Kernel32Exit : Library {
    fun ExitProcess(code: Int)
}

@Suppress("FunctionName")
private interface User32Show : Library {
    fun ShowWindow(window: Pointer, command: Int): Boolean
}

// Hides a window of another program by its handle.
private fun hideWindow(handle: Long) {
    runCatching { Native.load("user32", User32Show::class.java).ShowWindow(Pointer(handle), 0) }
}

// Runs the setup in a window of its own until it closes, then does what the person chose last, such as opening the app, and ends the process. The setup file of Windows gives the handle of the short window it shows while it unpacks the app, to be hidden once this one is up.
fun runSetup(mode: SetupMode, splash: Long? = null): Nothing {
    val log = File(System.getProperty("java.io.tmpdir"), "sumizuri-setup.log")
    val sink = FileLogSink(log)
    AppLog.sink = sink
    AppLog.info("setup", "the setup of ${BuildInfo.Version} started to ${mode.name.lowercase()}")
    var after: (() -> Unit)? = null
    application(exitProcessOnExit = false) {
        val scope = rememberCoroutineScope()
        val model = remember { SetupModel(mode, scope) }
        val state =
            rememberWindowState(
                size = DpSize(CardSize.width + ShadowRoom * 2, CardSize.height + ShadowRoom * 2),
                position = WindowPosition(Alignment.Center),
            )
        val close = {
            if (model.canClose) {
                after = model.afterClose
                exitApplication()
            }
        }
        Window(
            onCloseRequest = close,
            title = stringResource(Res.string.setup_title),
            icon = appIcon(),
            state = state,
            undecorated = true,
            transparent = true,
            resizable = false,
        ) {
            SumizuriTheme(ThemeMode.System, ThemePresets.first()) {
                SetupFrame(model, log, onMinimize = { state.isMinimized = true }, onClose = close)
            }
            // The window of the setup file goes once this one has drawn, so one takes the place of the other with nothing between.
            if (splash != null) {
                LaunchedEffect(Unit) {
                    withFrameNanos { }
                    hideWindow(splash)
                }
            }
        }
    }
    runCatching { after?.invoke() }.onFailure { AppLog.error("setup", "what was to follow the setup failed", it) }
    sink.flush()
    endProcess()
}

// Ends the process at once. The usual end would have Java write its archive of classes, which the setup, run from a temporary folder, has no use for.
private fun endProcess(): Nothing {
    if (System
            .getProperty("os.name")
            .orEmpty()
            .lowercase()
            .contains("win")
    ) {
        runCatching { Native.load("kernel32", Kernel32Exit::class.java).ExitProcess(0) }
    }
    exitProcess(0)
}

// The setup in its window, where the side panel and the bar move the window.
@Composable
private fun FrameWindowScope.SetupFrame(model: SetupModel, log: File, onMinimize: () -> Unit, onClose: () -> Unit) {
    LaunchedEffect(Unit) {
        while (true) {
            model.runningCount = withContext(Dispatchers.IO) { model.countRunning() }
            delay(1_500)
        }
    }
    SetupCard(
        model,
        window,
        log,
        onMinimize,
        onClose,
    ) { modifier, content -> WindowDraggableArea(modifier) { content() } }
}

// The card of the setup: the panel of ink and steps on the left, and the pages on the right under a bar with the buttons of the window.
@Composable
private fun SetupCard(
    model: SetupModel,
    window: AwtWindow?,
    log: File,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    drag: @Composable (Modifier, @Composable () -> Unit) -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(Modifier.fillMaxSize().padding(ShadowRoom)) {
        Row(
            Modifier
                .fillMaxSize()
                .shadow(
                    26.dp,
                    shape,
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.6f),
                ).clip(shape)
                .ambientBackground()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), shape),
        ) {
            drag(Modifier.width(SideWidth).fillMaxHeight()) { SidePanel(model) }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize()) {
                    drag(Modifier) { TitleBar(model, onMinimize, onClose) }
                    Pages(model, window, log, onClose, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TitleBar(model: SetupModel, onMinimize: () -> Unit, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        BarButton(Icons.Rounded.Remove, stringResource(Res.string.setup_minimize), onMinimize)
        BarButton(
            Icons.Rounded.Close,
            stringResource(Res.string.setup_close),
            onClose,
            enabled = model.canClose,
            danger = true,
        )
    }
}

// A round button of the window bar that lights up under the mouse, red for closing as Windows does.
@Composable
private fun BarButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val lit = hovered && enabled
    val back by animateColorAsState(
        if (!lit) {
            Color.Transparent
        } else if (danger) {
            Color(0xFFE81123)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        },
        label = "back",
    )
    val tint by animateColorAsState(
        if (lit &&
            danger
        ) {
            Color.White
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.35f)
        },
        label = "tint",
    )
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(back)
            .hoverable(source)
            .clickable(source, null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, Modifier.size(18.dp), tint = tint)
    }
}

// The page of the step the person is on. A step forward slides in from the right and a step back from the left.
@Composable
private fun Pages(model: SetupModel, window: AwtWindow?, log: File, onClose: () -> Unit, modifier: Modifier) {
    AnimatedContent(
        model.step,
        modifier.fillMaxSize(),
        transitionSpec = {
            val forward = targetState.ordinal >= initialState.ordinal
            val enter =
                slideInHorizontally(tween(440, easing = FastOutSlowInEasing)) { if (forward) it / 5 else -it / 5 } +
                    fadeIn(tween(320, delayMillis = 80))
            val exit =
                slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { if (forward) -it / 6 else it / 6 } +
                    fadeOut(tween(200))
            enter togetherWith exit using SizeTransform(clip = false)
        },
        label = "page",
    ) { step ->
        Box(Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, bottom = 30.dp)) {
            when (step) {
                SetupStep.Welcome -> WelcomePage(model)
                SetupStep.Folder -> FolderPage(model, window)
                SetupStep.Library -> LibraryPage(model, window)
                SetupStep.Shortcuts -> ShortcutsPage(model)
                SetupStep.Working -> WorkingPage(model)
                SetupStep.Done -> DonePage(model, onClose)
                SetupStep.Failed -> FailedPage(model, log, onClose)
                SetupStep.Remove -> RemovePage(model, onClose)
                SetupStep.Removed -> RemovedPage(model, onClose)
            }
        }
    }
}
