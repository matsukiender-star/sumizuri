package com.sumizuri.app.ui.kit.cover

import androidx.compose.ui.graphics.Color
import com.sumizuri.app.ui.kit.cover.focus.CoverPixels
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// The hues are told apart in this many groups.
private const val HueGroups = 24

// Less than this part of the picture having a color at all means the cover is grey, and it has no color to give.
private const val EnoughColored = 0.04

// A hue group must hold this part of the picture to be picked, so a small bright logo or spark does not win.
private const val EnoughInGroup = 0.03

// Skin is soft orange, and a page painted from it looks muddy, so pixels of that kind count for little.
private const val SkinWeight = 0.12

// Two colors of a palette are at least this far apart round the color wheel, so the second and third are not the first again.
private const val ApartDegrees = 38.0

// A second or third color must weigh at least this part of the first, so a speck does not become an accent of the page.
private const val EnoughBeside = 0.12

// The colors of a cover, the one that is most of it first and up to two more that are clearly different.
data class CoverPalette(val colors: List<Color>) {
    val main: Color get() = colors.first()

    // The palette as text to keep between runs, each color as a number.
    fun toText(): String = colors.joinToString(",") { (it.value shr 32).toLong().toString() }

    companion object {
        fun fromText(text: String): CoverPalette? {
            val colors = text.split(',').mapNotNull { it.toLongOrNull()?.let { argb -> Color(argb.toInt()) } }
            return if (colors.isEmpty()) null else CoverPalette(colors)
        }
    }
}

// The palette of a cover. Dark, pale and grey pixels are skipped, skin tones count for little, and each pixel counts as much as it is bright and very vivid, so clothes, eyes and sky beat skin, hair and a dull background. Null when the cover is grey.
fun coverPaletteOf(cover: CoverPixels): CoverPalette? {
    val weight = DoubleArray(HueGroups)
    val across = DoubleArray(HueGroups)
    val along = DoubleArray(HueGroups)
    val saturation = DoubleArray(HueGroups)
    val value = DoubleArray(HueGroups)
    val count = IntArray(HueGroups)
    var colored = 0
    for (argb in cover.argb) {
        if ((argb ushr 24) < 128) continue
        val r = ((argb shr 16) and 0xff) / 255.0
        val g = ((argb shr 8) and 0xff) / 255.0
        val b = (argb and 0xff) / 255.0
        val top = max(r, max(g, b))
        val spread = top - min(r, min(g, b))
        val s = if (top == 0.0) 0.0 else spread / top
        if (top < 0.2 || s < 0.15 || (s < 0.3 && top > 0.9)) continue
        val hue = when (top) {
            r -> (((g - b) / spread) % 6 + 6) % 6
            g -> (b - r) / spread + 2
            else -> (r - g) / spread + 4
        } * 60.0
        val group = (hue / (360.0 / HueGroups)).toInt() % HueGroups
        val w = s * s * top * (if (isSkin(hue, s, top)) SkinWeight else 1.0)
        colored++
        count[group]++
        weight[group] += w
        // The hue goes round, so its groups are averaged as arrows and not as numbers.
        val radians = hue * kotlin.math.PI / 180.0
        across[group] += cos(radians) * w
        along[group] += sin(radians) * w
        saturation[group] += s * w
        value[group] += top * w
    }
    if (colored < cover.argb.size * EnoughColored) return null
    // A group counts with its two neighbours, so a hue that falls on the edge of two groups is not split in half.
    fun reach(it: Int) = weight[it] + (weight[(it + 1) % HueGroups] + weight[(it + HueGroups - 1) % HueGroups]) * 0.5
    fun near(it: Int) = count[it] + count[(it + 1) % HueGroups] + count[(it + HueGroups - 1) % HueGroups]
    fun hueOf(group: Int) = (atan2(along[group], across[group]) * 180.0 / kotlin.math.PI + 360.0) % 360.0
    val big = (0 until HueGroups).filter { near(it) >= cover.argb.size * EnoughInGroup }
    val ranked = big.ifEmpty { (0 until HueGroups).toList() }.filter { weight[it] > 0.0 }.sortedByDescending(::reach)
    val first = ranked.firstOrNull() ?: return null
    val picked = mutableListOf(first)
    for (group in ranked.drop(1)) {
        if (picked.size == 3) break
        if (reach(group) < reach(first) * EnoughBeside) break
        val far = picked.all { apart(hueOf(it), hueOf(group)) >= ApartDegrees }
        if (far) picked += group
    }
    val colors = picked.map { group ->
        Color.hsv(
            hueOf(group).toFloat(),
            (saturation[group] / weight[group]).toFloat(),
            (value[group] / weight[group]).toFloat(),
        )
    }
    return CoverPalette(colors)
}

// How far apart two hues are round the color wheel, from 0 to 180 degrees.
private fun apart(one: Double, two: Double): Double {
    val gap = abs(one - two) % 360.0
    return if (gap > 180.0) 360.0 - gap else gap
}

// Whether a hue, saturation and brightness are the soft orange of skin.
private fun isSkin(hue: Double, s: Double, top: Double): Boolean = hue in 8.0..36.0 && s in 0.15..0.55 && top > 0.5
