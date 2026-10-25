package com.sumizuri.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.data.settings.SettingKey
import com.sumizuri.app.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// The steps of the first run, in the order they come.
internal enum class OnboardingStep { Welcome, Look, Profile, Library, Reading, Updates, Done }

// The step the person is on, and whether they got there going forward, which sets the side the next page comes in from.
internal data class OnboardingState(val step: OnboardingStep = OnboardingStep.Welcome, val forward: Boolean = true)

// Moves through the first steps, and keeps that they were gone through or skipped, so they show once.
internal class OnboardingViewModel(private val settings: SettingsRepository) : ViewModel() {
    private val mutable = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = mutable

    fun next() = mutable.update { now ->
        val steps = OnboardingStep.entries
        now.copy(step = steps[(now.step.ordinal + 1).coerceAtMost(steps.lastIndex)], forward = true)
    }

    fun back() = mutable.update { now ->
        now.copy(
            step = OnboardingStep.entries[(now.step.ordinal - 1).coerceAtLeast(0)],
            forward = false,
        )
    }

    fun finish() {
        viewModelScope.launch { settings.setFlag(SettingKey.OnboardingDone, true) }
    }
}
