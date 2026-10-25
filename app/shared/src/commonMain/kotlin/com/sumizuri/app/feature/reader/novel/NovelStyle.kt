package com.sumizuri.app.feature.reader.novel

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.sumizuri.app.data.model.reader.ReaderFont
import com.sumizuri.app.data.model.reader.ReaderSettings
import com.sumizuri.app.data.model.reader.ReaderTextAlign
import com.sumizuri.app.feature.reader.controls.ink
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.opendyslexic_bold
import com.sumizuri.app.shared.resources.opendyslexic_bold_italic
import com.sumizuri.app.shared.resources.opendyslexic_italic
import com.sumizuri.app.shared.resources.opendyslexic_regular
import org.jetbrains.compose.resources.Font

// The family of a typeface, or null for the one the system gives.
@Composable
fun ReaderFont.family(): FontFamily? = when (this) {
    ReaderFont.System -> null

    ReaderFont.OpenDyslexic -> FontFamily(
        Font(Res.font.opendyslexic_regular, FontWeight.Normal, FontStyle.Normal),
        Font(Res.font.opendyslexic_bold, FontWeight.Bold, FontStyle.Normal),
        Font(Res.font.opendyslexic_italic, FontWeight.Normal, FontStyle.Italic),
        Font(Res.font.opendyslexic_bold_italic, FontWeight.Bold, FontStyle.Italic),
    )
}

// How the words of a chapter are set, from the choices of the reader.
@Composable
fun novelStyle(settings: ReaderSettings, ink: Color): TextStyle = TextStyle(
    color = ink,
    fontFamily = settings.font.family(),
    fontSize = settings.fontSize.sp,
    lineHeight = (settings.fontSize * settings.lineHeight).sp,
    textAlign = if (settings.textAlign == ReaderTextAlign.Justify) TextAlign.Justify else TextAlign.Start,
)
