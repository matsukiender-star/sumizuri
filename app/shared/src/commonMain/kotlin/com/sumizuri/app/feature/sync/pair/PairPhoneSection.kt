package com.sumizuri.app.feature.sync.pair

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
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
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.options.solid
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
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val words = @Composable {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            }
                        }
                        // Beside the code on a wide page, under it on a phone.
                        if (maxWidth > SideBySide) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PairCode(shown)
                                words()
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                PairCode(shown)
                                words()
                            }
                        }
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

private val SideBySide = 520.dp

// The code in the paper and ink of the theme, with soft corners. A camera reads dark on light best, so a dark theme still gets a light ground, never a plain white box.
@Composable
private fun PairCode(link: String) {
    val scheme = MaterialTheme.colorScheme
    val night = scheme.surface.luminance() < 0.5f
    val paper = if (night) scheme.inverseSurface else scheme.surfaceContainerLowest
    val ink = if (night) scheme.inverseOnSurface else scheme.onSurface
    val painter = rememberQrCodePainter(link, ink) {
        shapes {
            ball = QrBallShape.roundCorners(.3f)
            frame = QrFrameShape.roundCorners(.3f)
            darkPixel = QrPixelShape.roundCorners(.45f)
        }
        colors { dark = QrBrush.solid(ink) }
    }
    val shape = RoundedCornerShape(20.dp)
    Box(Modifier.background(paper, shape).border(1.dp, scheme.outlineVariant, shape).padding(18.dp)) {
        Image(painter, stringResource(Res.string.pair_qr), Modifier.size(200.dp))
    }
}
