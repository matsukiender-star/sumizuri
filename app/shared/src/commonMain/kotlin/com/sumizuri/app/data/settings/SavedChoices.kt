package com.sumizuri.app.data.settings

import com.sumizuri.app.data.text.enumNamed

// The setting rows kept under one prefix, read back as the kind of value each holds. A value that is missing or cannot be read gives the fallback, and a number is kept inside its range.
class SavedChoices(val rows: Map<String, String>) {
    operator fun get(name: String): String? = rows[name]

    fun flag(name: String, fallback: Boolean): Boolean = rows[name]?.toBooleanStrictOrNull() ?: fallback

    fun whole(name: String, fallback: Int, range: IntRange): Int =
        rows[name]?.toIntOrNull()?.coerceIn(range) ?: fallback

    fun whole(name: String, fallback: Int, range: ClosedFloatingPointRange<Float>): Int = whole(
        name,
        fallback,
        range.start.toInt()..range.endInclusive.toInt(),
    )

    fun fraction(name: String, fallback: Float, range: ClosedFloatingPointRange<Float>): Float =
        rows[name]?.toFloatOrNull()?.coerceIn(range) ?: fallback

    inline fun <reified E : Enum<E>> choice(name: String, fallback: E): E = enumNamed<E>(get(name)) ?: fallback
}
