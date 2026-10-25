package com.sumizuri.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import com.sumizuri.app.data.model.reader.ReaderFont
import com.sumizuri.app.feature.reader.novel.family
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.inter_bold
import com.sumizuri.app.shared.resources.inter_medium
import com.sumizuri.app.shared.resources.inter_regular
import com.sumizuri.app.shared.resources.inter_semibold
import com.sumizuri.app.ui.theme.spec.ThemeTypography
import org.jetbrains.compose.resources.Font

// The names of the font families a theme can use, with the empty name for Inter, the font of the app.
val ThemeFonts = listOf("", "device", "sans", "serif", "mono", "dyslexic")

// Inter, the plain font most websites use, so the app reads the same on every device.
@Composable
fun interFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
)

// The family a font name stands for, or null for the font of the device.
@Composable
fun fontFamilyOf(name: String): FontFamily? = when (name) {
    "" -> interFamily()
    "device" -> null
    "sans" -> FontFamily.SansSerif
    "serif" -> FontFamily.Serif
    "mono" -> FontFamily.Monospace
    "dyslexic" -> ReaderFont.OpenDyslexic.family()
    else -> null
}

// The text styles with the fonts, the size and the weights a theme chose: the display family for the titles and headings, the body family for the rest.
@Composable
fun themedTypography(choice: ThemeTypography): Typography {
    val heading = fontFamilyOf(choice.displayFont)
    val body = fontFamilyOf(choice.bodyFont)
    val base = Typography()
    fun size(unit: TextUnit): TextUnit = if (unit.isSpecified) unit * choice.textScale else unit
    fun TextStyle.themed(family: FontFamily?, weight: Int?): TextStyle = copy(
        fontFamily = family ?: fontFamily,
        fontWeight = weight?.let { FontWeight(it) } ?: fontWeight,
        fontSize = size(fontSize),
        lineHeight = size(lineHeight),
    )
    fun TextStyle.head() = themed(heading, choice.headingWeight)
    fun TextStyle.text() = themed(body, choice.bodyWeight)
    return Typography(
        displayLarge = base.displayLarge.head(), displayMedium = base.displayMedium.head(), displaySmall = base.displaySmall.head(),
        headlineLarge = base.headlineLarge.head(), headlineMedium = base.headlineMedium.head(), headlineSmall = base.headlineSmall.head(),
        titleLarge = base.titleLarge.head(), titleMedium = base.titleMedium.head(), titleSmall = base.titleSmall.head(),
        bodyLarge = base.bodyLarge.text(), bodyMedium = base.bodyMedium.text(), bodySmall = base.bodySmall.text(),
        labelLarge = base.labelLarge.text(), labelMedium = base.labelMedium.text(), labelSmall = base.labelSmall.text(),
    )
}
