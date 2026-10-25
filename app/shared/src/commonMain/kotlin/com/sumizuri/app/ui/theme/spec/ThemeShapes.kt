package com.sumizuri.app.ui.theme.spec

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable

// The corners of every kind of piece and the weight of the hairlines, which together give a theme its character. The first values are the round look of the first run of the app: soft even corners on cards and rows, a larger one on dialogs and a pill for the bar of the navigation.
@Serializable
data class ThemeShapes(
    val card: ShapeSpec = ShapeSpec(ShapeStyle.Uniform, 16f),
    val item: ShapeSpec = ShapeSpec(ShapeStyle.Uniform, 14f),
    val cover: ShapeSpec = ShapeSpec(ShapeStyle.Uniform, 10f),
    val button: ShapeSpec = ShapeSpec(ShapeStyle.Uniform, 12f),
    val chip: ShapeSpec = ShapeSpec(ShapeStyle.Uniform, 10f),
    val iconTile: ShapeSpec = ShapeSpec(ShapeStyle.Uniform, 10f),
    val dialog: ShapeSpec = ShapeSpec(ShapeStyle.Uniform, 24f),
    val navBar: ShapeSpec = ShapeSpec(ShapeStyle.Pill, 0f),
    val sheetRadius: Float = 24f,
    val borderWidth: Float = 1f,
)

// How the corners of a piece are cut: two opposite corners large and the other two small like a leaf, all the same, or fully round.
@Serializable
enum class ShapeStyle { Asymmetric, Uniform, Pill }

// The corners of one kind of piece: the style, the large radius and, for the leaf cut, the small one, in dp.
@Serializable
data class ShapeSpec(val style: ShapeStyle, val large: Float, val small: Float = 0f) {
    fun toShape(): CornerBasedShape = when (style) {
        ShapeStyle.Asymmetric -> RoundedCornerShape(
            topStart = large.dp,
            topEnd = small.dp,
            bottomEnd = large.dp,
            bottomStart = small.dp,
        )

        ShapeStyle.Uniform -> RoundedCornerShape(large.dp)

        ShapeStyle.Pill -> RoundedCornerShape(50)
    }

    // The same cut a little smaller or larger, for pieces that sit inside one another.
    fun scaled(by: Float): ShapeSpec = copy(large = large * by, small = small * by)
}
