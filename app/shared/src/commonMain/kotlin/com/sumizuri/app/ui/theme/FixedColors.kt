package com.sumizuri.app.ui.theme

import androidx.compose.ui.graphics.Color

// Colors that stay the same in every theme, because they mean the same thing everywhere or sit on something the theme does not paint.
object FixedColors {
    // A title on a break and one that is finished, laid softly behind their words. Lavender and sky blue, so nothing in the default look is orange or green.
    val Hiatus = Color(0xFFA99BE0)
    val Completed = Color(0xFF64A6E8)

    // The star beside a rating.
    val Rating = Color(0xFFEBCB6B)

    // The amber of the reader's warm light.
    val WarmLight = Color(0xFFFF9A3C)

    // Text on a reader background the person picked: light on a dark one, dark on a light one.
    val InkOnDark = Color(0xFFE6E6E6)
    val InkOnLight = Color(0xFF1B1B1B)

    // The dark picture behind the sample subtitle.
    val VideoStill = Color(0xFF263238)
}
