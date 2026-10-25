package com.sumizuri.app.ui.theme

import androidx.compose.material3.Shapes
import com.sumizuri.app.ui.theme.spec.ThemeShapes

// The shapes Material pieces take their corners from, from the corners the theme gave each kind of piece: chips are the smallest, then buttons, cards, rows, and dialogs the largest.
internal fun ThemeShapes.toMaterial(): Shapes = Shapes(
    extraSmall = chip.toShape(),
    small = button.toShape(),
    medium = card.toShape(),
    large = item.toShape(),
    extraLarge = dialog.toShape(),
)
