package com.sumizuri.app.feature.reader.controls

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sumizuri.app.data.model.reader.ReaderFont
import com.sumizuri.app.data.model.reader.ReaderSettings
import com.sumizuri.app.data.model.reader.ReaderTextAlign
import com.sumizuri.app.data.model.reader.ReaderWidth
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.reader_wattpad_look
import org.jetbrains.compose.resources.stringResource

// Suki: sets the words the way reading sites like Wattpad do, a plain face a little big with air between lines and paragraphs, in a column of a comfortable width. Every value can still be moved after.
@Composable
internal fun WattpadLookButton(settings: ReaderSettings, onSettings: (ReaderSettings) -> Unit) {
    OutlinedButton(
        {
            onSettings(
                settings.copy(
                    font = ReaderFont.Sans,
                    fontSize = 18,
                    lineHeight = 1.7f,
                    paragraphGap = 18,
                    textAlign = ReaderTextAlign.Left,
                    width = ReaderWidth.Medium,
                ),
            )
        },
        Modifier.fillMaxWidth(),
    ) { Text(stringResource(Res.string.reader_wattpad_look)) }
}
