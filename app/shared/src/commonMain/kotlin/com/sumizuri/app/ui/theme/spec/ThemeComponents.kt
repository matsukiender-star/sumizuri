package com.sumizuri.app.ui.theme.spec

import kotlinx.serialization.Serializable

// How the rows of a list look.
@Serializable
data class ThemeComponents(
    // An arrow at the end of a row that opens a page.
    val listChevron: Boolean = true,
    // A small square with an icon in it at the start of a row.
    val listIconTiles: Boolean = true,
)
