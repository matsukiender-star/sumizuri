package com.sumizuri.app.feature.profile.blocks

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Plain colour names a script can use, picked to read well on the dark and the light themes alike.
private val Named = mapOf(
    "red" to Color(0xFFE5554F),
    "orange" to Color(0xFFF08A3E),
    "amber" to Color(0xFFF2B33D),
    "yellow" to Color(0xFFE8CF4A),
    "lime" to Color(0xFF9CCB4B),
    "green" to Color(0xFF4CAF6E),
    "teal" to Color(0xFF2FA59A),
    "cyan" to Color(0xFF3BB6D6),
    "blue" to Color(0xFF4C8DF0),
    "indigo" to Color(0xFF6A72E8),
    "purple" to Color(0xFF9B6BE0),
    "pink" to Color(0xFFE56BA6),
    "brown" to Color(0xFFA77B5A),
    "gray" to Color(0xFF9A9A9A),
    "grey" to Color(0xFF9A9A9A),
)

// A colour from a script: a colour of the theme by its name, a plain name, or #RRGGBB or #AARRGGBB. Anything else, and none, gives the fallback.
@Composable
internal fun viewColor(name: String?, fallback: Color = MaterialTheme.colorScheme.primary): Color {
    val scheme = MaterialTheme.colorScheme
    val key = name?.trim()?.lowercase() ?: return fallback
    return when (key) {
        "primary", "accent" -> scheme.primary
        "secondary" -> scheme.secondary
        "tertiary" -> scheme.tertiary
        "error" -> scheme.error
        "outline" -> scheme.outline
        else -> Named[key] ?: hex(key) ?: fallback
    }
}

private fun hex(text: String): Color? {
    val digits = text.removePrefix("#")
    if (!text.startsWith("#") || (digits.length != 6 && digits.length != 8)) return null
    val value = digits.toLongOrNull(16) ?: return null
    return Color(if (digits.length == 6) value or 0xFF000000 else value)
}

// The colours of the points of a chart in turn: the colours it names, or else shades that start from its colour and go round the theme.
@Composable
internal fun chartPalette(color: String?, colors: List<String>, count: Int): List<Color> {
    if (colors.isNotEmpty()) return colors.map { viewColor(it) }.let { named -> List(count) { named[it % named.size] } }
    val scheme = MaterialTheme.colorScheme
    val first = viewColor(color)
    val turn =
        listOf(
            first,
            scheme.tertiary,
            scheme.secondary,
            first.copy(alpha = 0.6f),
            scheme.tertiary.copy(alpha = 0.6f),
            scheme.secondary.copy(alpha = 0.6f),
            first.copy(alpha = 0.35f),
            scheme.outline,
        )
    return List(count) { turn[it % turn.size] }
}
