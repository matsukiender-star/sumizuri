package com.sumizuri.app.ui.theme.spec

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

// The six colors a person sets, as 0xAARRGGBB numbers. A role left null takes the one of the default theme, and every other color of the scheme is worked out from these.
@Serializable
data class ThemeColors(
    val primary: Long? = null,
    val primaryContainer: Long? = null,
    val secondary: Long? = null,
    val tertiary: Long? = null,
    val error: Long? = null,
    val surface: Long? = null,
) {
    // The color of a role by its name, or null when it was not set.
    operator fun get(role: String): Long? = when (role) {
        "primary" -> primary
        "primaryContainer" -> primaryContainer
        "secondary" -> secondary
        "tertiary" -> tertiary
        "error" -> error
        "surface" -> surface
        else -> null
    }

    // The same colors with one role changed.
    fun with(role: String, color: Long?): ThemeColors = when (role) {
        "primary" -> copy(primary = color)
        "primaryContainer" -> copy(primaryContainer = color)
        "secondary" -> copy(secondary = color)
        "tertiary" -> copy(tertiary = color)
        "error" -> copy(error = color)
        "surface" -> copy(surface = color)
        else -> this
    }

    companion object {
        val Roles = listOf("primary", "primaryContainer", "secondary", "tertiary", "error", "surface")
    }
}

fun Long.toColor(): Color = Color(this.toInt())

fun Color.toLong(): Long = (value shr 32).toLong() and 0xFFFFFFFFL
