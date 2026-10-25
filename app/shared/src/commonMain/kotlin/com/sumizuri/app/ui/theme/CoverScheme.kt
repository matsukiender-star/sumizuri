package com.sumizuri.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.sumizuri.app.ui.kit.cover.CoverPalette
import com.sumizuri.app.ui.theme.spec.CoverColorMode
import com.sumizuri.app.ui.theme.spec.ThemePageColors
import kotlin.math.max
import kotlin.math.min

// How far the page and the cards lean toward the cover in the tinted and the immersive look, before the strength the theme sets.
private const val TintPage = 0.10f
private const val TintCards = 0.14f
private const val DeepPage = 0.72f
private const val DeepCards = 0.62f

// How far text in the accent must stand apart from the page, as the web guidelines ask of text.
private const val TextContrast = 4.5f

// The color scheme a page takes from the palette of its cover. Each accent comes from a color of the cover, the main one, then the others when the cover has them, so the glow behind the page moves in the colors of the cover. The accents are made to read on the page the cover tints, and only their hue and strength come from the cover, since the brightness is set by the light or dark theme.
internal fun ColorScheme.followingCover(palette: CoverPalette, dark: Boolean, look: ThemePageColors): ColorScheme {
    if (look.mode == CoverColorMode.Off) return this
    val used = palette.colors.take(look.colors.coerceIn(1, 3))
    val main = Tone.of(used[0], look.vibrance)
    // A cover with one color still gives three accents, the others a little round the color wheel from it.
    val second = used.getOrNull(1)?.let { Tone.of(it, look.vibrance) } ?: main.turned(28f, 0.45f)
    val third = used.getOrNull(2)?.let { Tone.of(it, look.vibrance) } ?: main.turned(-40f, 0.6f)
    val leaning = when (look.mode) {
        CoverColorMode.Tinted -> lean(
            main.tint(dark),
            main.tint(dark),
            TintPage * look.strength,
            TintCards * look.strength,
        )

        CoverColorMode.Immersive -> lean(
            main.deepPage(dark),
            main.deepCards(dark),
            DeepPage * look.strength,
            DeepCards * look.strength,
        )

        else -> this
    }
    val page = leaning.background
    fun accent(tone: Tone, light: Float, darkLight: Float) =
        readableOn(tone.at(if (dark) darkLight else light), page, TextContrast)
    val primary = accent(main, 0.38f, 0.76f)
    val secondary = accent(second.calmer(0.7f), 0.40f, 0.74f)
    val tertiary = accent(third, 0.40f, 0.74f)
    fun container(tone: Tone) = if (dark) tone.at(0.26f, 0.75f) else tone.at(0.88f, 0.8f)
    val primaryContainer = container(main)
    val secondaryContainer = container(second.calmer(0.6f))
    val tertiaryContainer = container(third)
    return leaning.copy(
        primary = primary,
        onPrimary = textOn(primary),
        primaryContainer = primaryContainer,
        onPrimaryContainer = textOn(primaryContainer),
        secondary = secondary,
        onSecondary = textOn(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = textOn(secondaryContainer),
        tertiary = tertiary,
        onTertiary = textOn(tertiary),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = textOn(tertiaryContainer),
        surfaceTint = primary,
    )
}

// The page leaning toward one tint and its cards toward another, so the cards keep standing out from the page.
private fun ColorScheme.lean(pageTint: Color, cardTint: Color, page: Float, cards: Float): ColorScheme {
    fun onPage(base: Color) = lerp(base, pageTint, page.coerceIn(0f, 0.9f))
    fun onCard(base: Color) = lerp(base, cardTint, cards.coerceIn(0f, 0.9f))
    return copy(
        background = onPage(background),
        surface = onPage(surface),
        surfaceVariant = onCard(surfaceVariant),
        surfaceContainerLowest = onPage(surfaceContainerLowest),
        surfaceContainerLow = onPage(surfaceContainerLow),
        surfaceContainer = onCard(surfaceContainer),
        surfaceContainerHigh = onCard(surfaceContainerHigh),
        surfaceContainerHighest = onCard(surfaceContainerHighest),
        outlineVariant = onCard(outlineVariant),
    )
}

// Dark text on a light color and light text on a dark one, each with a trace of the color so it belongs to it.
private fun textOn(color: Color): Color {
    val dark = lerp(Color.Black, color, 0.15f)
    val light = lerp(Color.White, color, 0.08f)
    return if (contrastOf(dark, color) >= contrastOf(light, color)) dark else light
}

// A hue and how strong it is, from which the colors of the page are made at the brightness each one needs.
private class Tone(val hue: Float, val strength: Float) {
    fun at(lightness: Float, scale: Float = 1f): Color = Color.hsl(hue, (strength * scale).coerceIn(0f, 1f), lightness)

    // The tint the page leans toward, deep in the dark theme and pale in the light one.
    fun tint(dark: Boolean): Color = if (dark) at(0.3f, 0.8f) else at(0.84f, 0.7f)

    // The deep page of the immersive look and the cards on it: a dark page well into the color with lighter cards, or a soft colored page with paler cards.
    fun deepPage(dark: Boolean): Color = if (dark) at(0.13f, 0.95f) else at(0.92f, 0.85f)

    fun deepCards(dark: Boolean): Color = if (dark) at(0.22f, 0.85f) else at(0.96f, 0.7f)

    fun calmer(by: Float) = Tone(hue, strength * by)

    fun turned(degrees: Float, scale: Float) = Tone(((hue + degrees) % 360f + 360f) % 360f, strength * scale)

    companion object {
        // A faint color is made stronger and a loud one calmer, so every cover gives colors that read well, then the vibrance of the theme is laid over that.
        fun of(color: Color, vibrance: Float): Tone {
            val top = max(color.red, max(color.green, color.blue))
            val bottom = min(color.red, min(color.green, color.blue))
            val spread = top - bottom
            if (spread == 0f) return Tone(0f, 0f)
            val hue = when (top) {
                color.red -> ((color.green - color.blue) / spread) % 6f
                color.green -> (color.blue - color.red) / spread + 2f
                else -> (color.red - color.green) / spread + 4f
            } * 60f
            val strength = (spread / top).coerceIn(0.35f, 0.8f) * vibrance
            return Tone((hue + 360f) % 360f, strength.coerceIn(0f, 1f))
        }
    }
}
