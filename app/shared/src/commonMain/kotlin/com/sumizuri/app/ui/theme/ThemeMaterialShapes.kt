package com.sumizuri.app.ui.theme

import androidx.compose.material3.Shapes
import com.sumizuri.app.ui.theme.spec.ShapeStyle
import com.sumizuri.app.ui.theme.spec.ThemeShapes

// The shapes Material pieces take their corners from, from the corners the theme gave each kind of piece: chips are the smallest, then buttons, cards, rows, and dialogs the largest. Covers, cards and menus of every size take the small one too, so buttons round at the ends leave it to the corners of rows: half the height as corners would turn a tall piece into a capsule.
internal fun ThemeShapes.toMaterial(): Shapes = Shapes(
    extraSmall = chip.toShape(),
    small = (if (button.style == ShapeStyle.Pill) item else button).toShape(),
    medium = card.toShape(),
    large = item.toShape(),
    extraLarge = dialog.toShape(),
)
