package com.sumizuri.app.ui.theme.spec

// The four characters the corners of a theme can have, which set the corners of every piece together. Round is the look the app starts with, and buttons are round at the ends in all of them.
enum class ShapeCharacter { Sharp, Soft, Round, Leaf }

// The corners of every piece for a character, each in proportion to the size of the character. The leaf cut has two large corners and two small ones.
fun shapesOf(character: ShapeCharacter, base: ThemeShapes = ThemeShapes()): ThemeShapes {
    fun leaf(large: Float, small: Float) = ShapeSpec(ShapeStyle.Asymmetric, large, small)
    return when (character) {
        ShapeCharacter.Round -> ThemeShapes(borderWidth = base.borderWidth)

        ShapeCharacter.Leaf -> base.copy(
            card = leaf(
                14f,
                4f,
            ),
            item = leaf(12f, 4f), cover = leaf(10f, 3f), button = leaf(10f, 3f), chip = leaf(10f, 3f),
            iconTile = ShapeSpec(
                ShapeStyle.Uniform,
                10f,
            ),
            dialog = leaf(20f, 4f), navBar = ShapeSpec(ShapeStyle.Pill, 0f), sheetRadius = 20f,
        )

        else -> {
            val size = if (character == ShapeCharacter.Sharp) 4f else 12f
            fun spec(scale: Float = 1f): ShapeSpec = ShapeSpec(ShapeStyle.Uniform, size * scale)
            base.copy(
                card = spec(),
                item = spec(0.85f),
                cover = spec(0.7f),
                button = spec(0.8f),
                chip = spec(0.7f),
                iconTile = spec(0.7f),
                dialog = spec(1.2f),
                navBar = spec(1.2f),
                sheetRadius = size * 1.4f,
            )
        }
    }
}
