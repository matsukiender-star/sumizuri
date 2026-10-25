package com.sumizuri.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.data.model.reader.ReaderSettings
import com.sumizuri.app.data.settings.ReaderSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// The choices of the reader that hold for every title, which the reading pages of the settings change. They are the same ones the reader sheet sets when it is on all titles.
class GlobalReaderViewModel(private val repository: ReaderSettingsRepository) : ViewModel() {
    val settings: StateFlow<ReaderSettings> = repository
        .observe(NoTitle)
        .map { it.copy(override = false) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReaderSettings())

    // Applies a change to the choices as they are now and keeps all of them, for every title.
    fun update(change: (ReaderSettings) -> ReaderSettings) {
        val next = change(settings.value)
        viewModelScope.launch {
            repository.save(NoTitle, next)
            repository.saveGlobal(next)
            repository.setDefaultMode(next.mode)
        }
    }

    private companion object {
        // The settings have no title, so a number that no title has makes the reader settings of all of them show.
        const val NoTitle = 0L
    }
}
