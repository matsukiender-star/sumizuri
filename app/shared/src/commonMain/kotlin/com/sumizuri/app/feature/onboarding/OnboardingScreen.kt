package com.sumizuri.app.feature.onboarding

import kotlinx.coroutines.delay
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.focusGroup
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.settings.SettingKey
import com.sumizuri.app.data.sync.rememberNoticePermission
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.onboarding_back
import com.sumizuri.app.shared.resources.onboarding_finish
import com.sumizuri.app.shared.resources.onboarding_next
import com.sumizuri.app.shared.resources.onboarding_skip
import com.sumizuri.app.shared.resources.onboarding_start
import com.sumizuri.app.ui.kit.art.MainButton
import com.sumizuri.app.ui.kit.art.QuietButton
import com.sumizuri.app.ui.kit.art.StretchDot
import com.sumizuri.app.ui.kit.art.rise
import com.sumizuri.app.ui.theme.background.ambientBackground
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// The steps are never wider than this, so they read well on a wide window too.
private val ContentWidth = 640.dp

// How long a step takes to come in before the focus can go to its buttons.
private const val StepFocusDelayMs = 400L

// The first steps after installing, over the whole app: a welcome, the look, the profile, a backup to bring, how to read, new chapters, and the end. It stays until the person finishes or skips it, and the library it leaves them on is behind it all along.
@Composable
internal fun OnboardingScreen(onMigrate: (List<Long>) -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val model = viewModel { OnboardingViewModel(container.settings) }
    val state by model.state.collectAsStateWithLifecycle()
    val askNotices = rememberNoticePermission()
    val scope = rememberCoroutineScope()
    val backState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        backState,
        isBackEnabled =
        state.step != OnboardingStep.Welcome && state.step != OnboardingStep.Done,
    ) { model.back() }
    // Leaving the step about new chapters asks for the yes the system wants for notices, once, while they are on.
    val next: () -> Unit = {
        if (state.step == OnboardingStep.Updates) {
            scope.launch {
                val settings = container.settings
                if (settings.flag(SettingKey.UpdateNotify, true).first() &&
                    !settings.flag(SettingKey.NoticeAsked, false).first()
                ) {
                    settings.setFlag(SettingKey.NoticeAsked, true)
                    askNotices()
                }
            }
        }
        model.next()
    }
    // Stands in the way of every touch, so nothing reaches the app under it while it shows. It only listens and takes none of them, since taking them would also cancel a press on a button of its own whenever the pointer or the page moved during it.
    // The focus of a TV remote is kept inside the steps too, so the arrows never wander to the library behind.
    val trap = remember { FocusRequester() }
    // Each step brings new buttons and the old one with the focus goes, so the focus is taken back once the step has come in.
    LaunchedEffect(state.step) {
        delay(StepFocusDelayMs)
        trap.requestFocus()
    }
    Box(
        modifier.fillMaxSize().ambientBackground().pointerInput(Unit) {
            awaitPointerEventScope { while (true) awaitPointerEvent() }
        }.focusRequester(trap).focusProperties { onExit = { cancelFocusChange() } }.focusGroup(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing,
                ).padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = ContentWidth).fillMaxSize()) {
                TopRow(state.step, model::finish)
                AnimatedContent(
                    state,
                    Modifier.weight(1f).fillMaxWidth(),
                    transitionSpec = {
                        val forward = targetState.forward
                        val enter =
                            slideInHorizontally(tween(440, easing = FastOutSlowInEasing)) {
                                if (forward) it / 4 else -it / 4
                            } + fadeIn(tween(320, delayMillis = 80))
                        val exit =
                            slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) {
                                if (forward) -it / 5 else it / 5
                            } + fadeOut(tween(180))
                        enter togetherWith exit using SizeTransform(clip = false)
                    },
                    contentKey = { it.step },
                    label = "step",
                ) { shown ->
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        when (shown.step) {
                            OnboardingStep.Welcome -> WelcomeStep()

                            OnboardingStep.Look -> LookStep()

                            OnboardingStep.Profile -> ProfileStep()

                            OnboardingStep.Library -> LibraryStep(onMigrate = { waiting ->
                                model.finish()
                                onMigrate(waiting)
                            })

                            OnboardingStep.Reading -> ReadingStep()

                            OnboardingStep.Updates -> UpdatesStep()

                            OnboardingStep.Done -> DoneStep()
                        }
                    }
                }
                BottomRow(state.step, model::back, next, model::finish)
            }
        }
    }
}

// The dots of the steps between the welcome and the end, the one the person is on drawn out, and the way to skip the rest.
@Composable
private fun TopRow(step: OnboardingStep, onSkip: () -> Unit) {
    val middle = OnboardingStep.entries.drop(1).dropLast(1)
    Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            androidx.compose.animation.AnimatedVisibility(step in middle, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (each in middle) StretchDot(each == step, done = each.ordinal < step.ordinal)
                }
            }
        }
        if (step != OnboardingStep.Done) TextButton(onSkip) { Text(stringResource(Res.string.onboarding_skip)) }
    }
}

@Composable
private fun BottomRow(step: OnboardingStep, onBack: () -> Unit, onNext: () -> Unit, onFinish: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (step) {
            OnboardingStep.Welcome -> {
                Spacer(Modifier.weight(1f))
                MainButton(stringResource(Res.string.onboarding_start), onNext)
            }

            OnboardingStep.Done -> {
                Spacer(Modifier.weight(1f))
                MainButton(stringResource(Res.string.onboarding_finish), onFinish)
            }

            else -> {
                QuietButton(stringResource(Res.string.onboarding_back), onBack)
                Spacer(Modifier.weight(1f))
                MainButton(stringResource(Res.string.onboarding_next), onNext)
            }
        }
    }
}

// The title of a step and the line under it, rising in first.
@Composable
internal fun StepHeader(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.rise(0), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text,
            Modifier.rise(1),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
