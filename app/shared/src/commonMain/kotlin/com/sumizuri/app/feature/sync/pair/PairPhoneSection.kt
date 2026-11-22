package com.sumizuri.app.feature.sync.pair

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.feature.sync.Form
import com.sumizuri.app.feature.sync.Working
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.pair_again
import com.sumizuri.app.shared.resources.pair_copy
import com.sumizuri.app.shared.resources.pair_expired
import com.sumizuri.app.shared.resources.pair_hint
import com.sumizuri.app.shared.resources.pair_left
import com.sumizuri.app.shared.resources.pair_qr
import com.sumizuri.app.shared.resources.pair_scan
import com.sumizuri.app.shared.resources.pair_section
import com.sumizuri.app.shared.resources.pair_show
import com.sumizuri.app.ui.kit.art.InkButton
import com.sumizuri.app.ui.kit.art.InkOutlinedButton
import com.sumizuri.app.ui.kit.input.rememberCopyText
import com.sumizuri.app.ui.kit.setting.SettingsSection
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val TickMs = 1_000L

// Asked by the button of the library, so the page shows the code without one more tap.
internal object PairRequest {
    private var asked = false

    fun ask() {
        asked = true
    }

    fun take(): Boolean = asked.also { asked = false }
}

// On a computer that can be the server: the button that shows the code, and the code while it works.
@Composable
internal fun PairPhoneSection(model: PairModel) {
    if (!model.canShow) return
    val ui by model.ui.collectAsStateWithLifecycle()
    // Opened from the button of the library, the code shows at once.
    LaunchedEffect(model) { if (PairRequest.take()) model.show() }
    val copy = rememberCopyText()
    val left by produceState(0, ui.code, ui.until) {
        while (ui.code != null) {
            value = ((ui.until - nowMillis()) / TickMs).toInt().coerceAtLeast(0)
            if (value == 0) break
            delay(TickMs)
        }
    }
    SettingsSection(Res.string.pair_section, Res.string.pair_hint) {
        padded {
            Form(ui.showing) {
                val shown = ui.code
                if (shown != null && left > 0) {
                    // Dark on white whatever the theme, since cameras read that best.
                    Box(Modifier.background(Color.White, RoundedCornerShape(12.dp)).padding(16.dp)) {
                        Image(rememberQrCodePainter(shown), stringResource(Res.string.pair_qr), Modifier.size(240.dp))
                    }
                    Text(stringResource(Res.string.pair_scan), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(Res.string.pair_left, left),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InkOutlinedButton({ copy(shown) }) { Text(stringResource(Res.string.pair_copy)) }
                        InkOutlinedButton(model::show) { Text(stringResource(Res.string.pair_again)) }
                    }
                } else {
                    if (shown !=
                        null
                    ) {
                        Text(stringResource(Res.string.pair_expired), color = MaterialTheme.colorScheme.outline)
                    }
                    InkButton(model::show, enabled = !ui.showing) { Text(stringResource(Res.string.pair_show)) }
                }
                if (ui.showing) Working()
                ui.showProblem?.let { PairProblem(it) }
            }
        }
    }
}
