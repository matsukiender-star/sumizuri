package com.sumizuri.app.ui.theme.spec

// Colors of a role set in one mode, from the six numbers a preset gives.
private fun colors(surface: Long, primary: Long, container: Long, secondary: Long, tertiary: Long, error: Long) =
    ThemeColors(
        primary =
        primary or 0xFF000000,
        primaryContainer = container or 0xFF000000,
        secondary = secondary or 0xFF000000,
        tertiary =
        tertiary or 0xFF000000,
        error = error or 0xFF000000,
        surface = surface or 0xFF000000,
    )

// The themes that come with the app. They all share the look of its first run, the ink drifting behind every page, round buttons, soft cards and the small stretching marks, and differ only in their colors. Sumizuri is the first and the one the app starts with.
val ThemePresets: List<AppThemeSpec> = listOf(
    // Sumizuri: moonlight over sumi ink, the colors of the icon. Night slate and pale moon gold with silver beside them. By day the ink is the accent and the moon its tint.
    AppThemeSpec(
        AppThemeSpec.DefaultId,
        "Sumizuri",
        light = colors(0xF5F4EF, 0x343A4A, 0xECE5C2, 0x4E5260, 0x6B6A80, 0xB3261E),
        dark = colors(0x0F1114, 0xE8E0B0, 0x2C2F38, 0xB9BFCC, 0x9AA6C2, 0xE58592),
        // The theme the app starts with lies still behind its pages, as calm layered waves, so nothing moves until a person asks for it.
        options = ThemeOptions(background = ThemeBackground(style = BackgroundStyle.Waves, speed = 0f)),
    ),
    // Shikkoku: black lacquer and gold leaf.
    AppThemeSpec(
        "builtin:shikkoku",
        "Shikkoku",
        light = colors(0xF5F1E8, 0x7A5A12, 0xEADFC4, 0x4A4436, 0x8A3B2A, 0xB3261E),
        dark = colors(0x0B0A08, 0xE2B659, 0x3A2E10, 0xC9BFA8, 0xD97A5B, 0xF2A3A0),
    ),
    // Paperback: a well read paperback from a library. Cream paper, bottle green and oxblood.
    AppThemeSpec(
        "builtin:paperback",
        "Paperback",
        light = colors(0xF6F0E1, 0x2E5E4E, 0xD7E6DC, 0x5A4E3C, 0x8B3A2B, 0xA3261E),
        dark = colors(0x1A1712, 0x8FC7AE, 0x23443A, 0xC8BCA6, 0xE0957F, 0xF2A3A0),
    ),
    // Akihabara: neon signs at night. Hot magenta and electric cyan on deep navy.
    AppThemeSpec(
        "builtin:akihabara",
        "Akihabara",
        light = colors(0xF4F2FA, 0xC2187A, 0xF8D5EA, 0x4B4A66, 0x007C91, 0xB3261E),
        dark = colors(0x0B0B14, 0xFF4FB0, 0x5C0E3E, 0xB9B8D6, 0x3BE0F0, 0xFF8A8A),
    ),
    // Sakura: cherry blossom in spring. Blush pink and young leaf green.
    AppThemeSpec(
        "builtin:sakura",
        "Sakura",
        light = colors(0xFBF3F4, 0xC2496B, 0xF9D9E2, 0x6E5A60, 0x5E7A4A, 0xB3261E),
        dark = colors(0x1A1215, 0xF48FB1, 0x5A2335, 0xD8BFC6, 0xA8C98F, 0xF2A3A0),
    ),
    // Matcha: a quiet tea room. Whisked green and cream.
    AppThemeSpec(
        "builtin:matcha",
        "Matcha",
        light = colors(0xF1F3EA, 0x4E7A2E, 0xDCE8C8, 0x55604A, 0x8A6A2E, 0xB3261E),
        dark = colors(0x11150E, 0xA9D17F, 0x2F4520, 0xBCC7AE, 0xD8B878, 0xF2A3A0),
    ),
    // Ukiyo: a woodblock print of the great wave. Prussian blue and ochre.
    AppThemeSpec(
        "builtin:ukiyo",
        "Ukiyo",
        light = colors(0xF3EFE4, 0x1F4E8C, 0xD6E1F2, 0x4A4F5C, 0xB5651D, 0xB3261E),
        dark = colors(0x0D1420, 0x8DB4F0, 0x1C3A66, 0xB7C0D1, 0xF0A35C, 0xF2A3A0),
    ),
    // Shinbun: the morning paper. Black on newsprint grey with one red for what matters.
    AppThemeSpec(
        "builtin:shinbun",
        "Shinbun",
        light = colors(0xF2F2EF, 0x1A1A1A, 0xDCDCD8, 0x4F4F4F, 0x9A1C1C, 0xB3261E),
        dark = colors(0x111111, 0xEDEDED, 0x3A3A3A, 0xB5B5B5, 0xE06666, 0xF2A3A0),
    ),
    // Yugure: dusk, violet into amber.
    AppThemeSpec(
        "builtin:yugure",
        "Yugure",
        light = colors(0xF3F0F9, 0x5B43B8, 0xE3DCF8, 0x5B5670, 0x8A4B14, 0xB3261E),
        dark = colors(0x120F1C, 0xB7A1FF, 0x342766, 0xC5BED9, 0xFFB77A, 0xF2A3A0),
    ),
)
