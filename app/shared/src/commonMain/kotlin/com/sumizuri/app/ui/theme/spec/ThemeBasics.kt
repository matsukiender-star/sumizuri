package com.sumizuri.app.ui.theme.spec

import kotlinx.serialization.Serializable

// The fonts, the size of text and how heavy it is. A font is empty for the one of the device, or the name of a family the app has.
@Serializable
data class ThemeTypography(
    val displayFont: String = "",
    val bodyFont: String = "",
    val textScale: Float = 1f,
    // The weight of headings and of body text as a number from 100 to 900, or null to keep what the family has.
    val headingWeight: Int? = null,
    val bodyWeight: Int? = null,
)

// How much room there is around things.
@Serializable
data class ThemeLayout(val density: LayoutDensity = LayoutDensity.Comfortable, val spacing: Float = 1f)

// What is painted besides the pieces themselves: how strong the moving background behind every screen is, from none to three times as strong, shadows under covers, and a dark that is black.
@Serializable
data class ThemeEffects(val bloom: Float = 1f, val coverShadow: Boolean = false, val oledDark: Boolean = false)

// Small choices about the controls.
@Serializable
data class ThemeControls(val switchIcons: Boolean = false)
