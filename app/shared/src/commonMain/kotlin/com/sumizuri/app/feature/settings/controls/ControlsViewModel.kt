package com.sumizuri.app.feature.settings.controls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sumizuri.app.data.model.controls.AppGestures
import com.sumizuri.app.data.model.controls.ReaderControls
import com.sumizuri.app.data.settings.ControlsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// The keys, taps and gestures the person set, and the way to change them. A change is given as a function of the old value, so the pages never hold a copy that could go stale.
class ControlsViewModel(private val repository: ControlsRepository) : ViewModel() {
    val reader: StateFlow<ReaderControls> = repository.reader.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ReaderControls(),
    )

    val app: StateFlow<AppGestures> = repository.app.stateIn(viewModelScope, SharingStarted.Eagerly, AppGestures())

    fun onReader(change: (ReaderControls) -> ReaderControls) {
        viewModelScope.launch { repository.setReader(change(reader.value)) }
    }

    fun onApp(change: (AppGestures) -> AppGestures) {
        viewModelScope.launch { repository.setApp(change(app.value)) }
    }
}
