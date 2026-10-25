package com.sumizuri.app.feature.theme

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumizuri.app.data.LocalAppContainer
import com.sumizuri.app.data.settings.ThemeRepository
import com.sumizuri.app.ui.theme.spec.AppThemeSpec
import com.sumizuri.app.ui.theme.spec.ThemePresets
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// The theme in use and the themes a person made, with the ways to choose, keep, copy and remove them.
class ThemeViewModel(private val repository: ThemeRepository) : ViewModel() {
    val active: StateFlow<AppThemeSpec> = repository.active.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ThemePresets.first(),
    )

    val custom: StateFlow<List<AppThemeSpec>> = repository.custom.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList(),
    )

    fun select(id: String) {
        viewModelScope.launch { repository.select(id) }
    }

    // Keeps a theme the person made and, unless asked not to, makes it the one in use.
    fun save(theme: AppThemeSpec, use: Boolean = true) {
        viewModelScope.launch {
            repository.save(theme)
            if (use) repository.select(theme.id)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            if (active.value.id == id) repository.select(AppThemeSpec.DefaultId)
            repository.delete(id)
        }
    }
}

// The one model of the themes for the screen it is asked from, made from the themes of the app.
@Composable
fun rememberThemeModel(): ThemeViewModel {
    val container = LocalAppContainer.current
    return viewModel { ThemeViewModel(container.themes) }
}
