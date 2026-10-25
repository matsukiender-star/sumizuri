package com.sumizuri.app.ui.theme.spec

import kotlinx.serialization.Serializable

// A whole theme: its colors for the light and for the dark mode, the corners of its pieces, and every other choice. The id of a built in theme starts with builtin and the id of one a person made with custom.
@Serializable
data class AppThemeSpec(
    val id: String,
    val name: String,
    val author: String = "",
    val light: ThemeColors = ThemeColors(),
    val dark: ThemeColors = ThemeColors(),
    val shapes: ThemeShapes = ThemeShapes(),
    val options: ThemeOptions = ThemeOptions(),
) {
    val isBuiltIn: Boolean get() = id.startsWith(BuiltInPrefix)

    companion object {
        const val BuiltInPrefix = "builtin:"
        const val CustomPrefix = "custom:"
        const val DefaultId = "builtin:ink"
    }
}
