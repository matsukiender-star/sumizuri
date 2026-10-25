package com.sumizuri.app.data.blocks

import com.sumizuri.app.data.model.Title
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.script_err_need_list
import com.sumizuri.app.shared.resources.script_err_not_number
import org.jetbrains.compose.resources.StringResource
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

// A chapter that was read: the title it belongs to and when, in epoch milliseconds.
data class ReadEvent(val titleId: Long, val at: Long)

// One labelled number of a series, such as a genre and how many titles have it. A label the app names, such as a weekday, also carries its name in the strings so it shows in the language of the app.
data class Point(val label: String, val value: Double, val name: StringResource? = null)

// A value on a wire.
sealed interface Value {
    data class Num(val value: Double) : Value
    data class Txt(val value: String) : Value
    data class Flag(val value: Boolean) : Value
    data class Titles(val items: List<Title>) : Value
    data class Reads(val items: List<ReadEvent>) : Value
    data class Series(val points: List<Point>) : Value
}

// Something a script could not do, said so the person can mend it: words of the app with the pieces of the script they name, and the place in the script, such as view.row[1].chart, once it is known.
class ScriptError(val text: StringResource, val args: List<Any>, val at: String) : Exception(text.key) {
    constructor(text: StringResource, vararg args: Any) : this(text, args.toList(), "")

    // The same error at a place, unless it already knows one deeper in.
    fun placed(place: String): ScriptError = if (at.isEmpty()) ScriptError(text, args, place) else this

    fun problem(): BlockView.Problem = BlockView.Problem(text, args, at)
}

// The value as a number: a list counts its items and a series adds up its values.
fun Value.asNumber(): Double = when (this) {
    is Value.Num -> value

    is Value.Flag -> if (value) 1.0 else 0.0

    is Value.Txt -> value.trim().replace(',', '.').toDoubleOrNull()
        ?: throw ScriptError(Res.string.script_err_not_number, value)

    is Value.Titles -> items.size.toDouble()

    is Value.Reads -> items.size.toDouble()

    is Value.Series -> points.sumOf { it.value }
}

fun Value.asText(): String = when (this) {
    is Value.Num -> numberText(value)
    is Value.Txt -> value
    is Value.Flag -> value.toString()
    is Value.Titles -> items.joinToString(", ") { it.name }
    is Value.Reads -> items.size.toString()
    is Value.Series -> points.joinToString(", ") { "${it.label} ${numberText(it.value)}" }
}

fun Value.asBool(): Boolean = when (this) {
    is Value.Flag -> value
    is Value.Num -> value != 0.0
    is Value.Txt -> value.isNotBlank() && value != "false"
    is Value.Titles -> items.isNotEmpty()
    is Value.Reads -> items.isNotEmpty()
    is Value.Series -> points.isNotEmpty()
}

// How many items a list holds, for a value that is one.
fun Value.size(): Int = when (this) {
    is Value.Titles -> items.size
    is Value.Reads -> items.size
    is Value.Series -> points.size
    is Value.Txt -> value.length
    else -> throw ScriptError(Res.string.script_err_need_list)
}

// A number as people write it: whole numbers without decimals, and others with up to two.
fun numberText(value: Double, digits: Int = 2): String {
    if (value.isNaN()) return "—"
    if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
    var scale = 1.0
    repeat(digits.coerceIn(0, 6)) { scale *= 10 }
    val rounded = (value * scale).roundToLong() / scale
    if (rounded == floor(rounded) && abs(rounded) < 1e15) return rounded.toLong().toString()
    return rounded.toString()
}

// A number rounded to a step of one over scale, with halves going up as people round, where the rounding of Kotlin takes them to the even neighbour.
internal fun halfUp(value: Double, scale: Double): Double = floor(value * scale + 0.5) / scale
