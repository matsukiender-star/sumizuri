package com.sumizuri.app.feature.settings.controls

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sumizuri.app.data.LocalAppContainer
@Composable
fun ControlsRoute(page: ControlsPage, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val model = viewModel(key = "controls_${page.name}") { ControlsViewModel(container.controls) }
    val reader by model.reader.collectAsStateWithLifecycle()
    val app by model.app.collectAsStateWithLifecycle()
    when (page) {
        ControlsPage.ReaderKeys -> ReaderKeysPage(reader, model::onReader, onBack)
        ControlsPage.TapZones -> TapZonesPage(reader, model::onReader, onBack)
        ControlsPage.Shortcuts -> ShortcutsPage(app, model::onApp, onBack)
        ControlsPage.Gestures -> GesturesPage(app, model::onApp, onBack)
    }
}
