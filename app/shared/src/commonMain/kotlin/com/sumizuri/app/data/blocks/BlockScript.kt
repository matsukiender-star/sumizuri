package com.sumizuri.app.data.blocks

import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.script_err_chart_series
import com.sumizuri.app.shared.resources.script_err_chart_style
import com.sumizuri.app.shared.resources.script_err_covers
import com.sumizuri.app.shared.resources.script_err_heatmap
import com.sumizuri.app.shared.resources.script_err_heatmap_shape
import com.sumizuri.app.shared.resources.script_err_if_then
import com.sumizuri.app.shared.resources.script_err_json
import com.sumizuri.app.shared.resources.script_err_object
import com.sumizuri.app.shared.resources.script_err_parts
import com.sumizuri.app.shared.resources.script_err_section
import com.sumizuri.app.shared.resources.script_err_value
import com.sumizuri.app.shared.resources.script_err_view_kind
import com.sumizuri.app.shared.resources.script_err_view_object
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

// What a script gave: what the block shows, and the title it asked for.
class ScriptResult(val view: BlockView, val title: String)

// The kinds of view a script can ask for, each by the key that holds its value.
private val ViewKeys =
    listOf("number", "ring", "bar", "chart", "list", "covers", "heatmap", "text", "stack", "row", "section", "if")

private val Placeholder = Regex("\\{([^{}]+)\\}")

// Runs the script of a block. A script is a JSON object: a title, values worked out in order under let, each a formula or a plain number, and the view, which shows them. Text such as a label may hold a formula in braces, as "{done} of {goal}". Anything that goes wrong is shown in place of the view, with where it went wrong.
fun runScript(script: String, context: BlockContext): ScriptResult {
    if (script.isBlank()) return ScriptResult(BlockView.Empty, "")
    val root = try {
        Json.parseToJsonElement(script) as? JsonObject
            ?: return ScriptResult(BlockView.Problem(Res.string.script_err_object), "")
    } catch (error: Exception) {
        return ScriptResult(
            BlockView.Problem(
                Res.string.script_err_json,
                listOf(
                    error.message
                        ?.lineSequence()
                        ?.firstOrNull()
                        .orEmpty(),
                ),
            ),
            "",
        )
    }
    val run = ScriptRun(context)
    (root["let"] as? JsonObject)?.forEach { (name, element) ->
        try {
            run.values[name] = run.value(element)
        } catch (error: ScriptError) {
            return ScriptResult(error.placed("let \"$name\"").problem(), "")
        }
    }
    val title = try {
        (root["title"] as? JsonPrimitive)?.content?.let(run::text).orEmpty()
    } catch (error: ScriptError) {
        return ScriptResult(error.placed("title").problem(), "")
    }
    val view = root["view"] ?: return ScriptResult(BlockView.Empty, title)
    return ScriptResult(
        try {
            run.view(view, "view")
        } catch (error: ScriptError) {
            error.problem()
        },
        title,
    )
}

// One run of a script: the values it named, over the data of the profile.
private class ScriptRun(val context: BlockContext) {
    val values = LinkedHashMap<String, Value>()

    // The names every script has: library, reads, and the numbers of stats.
    private fun data(name: String): Value? {
        val stats = context.stats
        return when (name) {
            "library" -> Value.Titles(context.titles)
            "reads" -> Value.Reads(context.reads)
            "today" -> Value.Num(context.today.toDouble())
            "stats.read" -> Value.Num(stats.chaptersRead.toDouble())
            "stats.today" -> Value.Num(stats.chaptersToday.toDouble())
            "stats.week" -> Value.Num(stats.chaptersThisWeek.toDouble())
            "stats.month" -> Value.Num(stats.chaptersThisMonth.toDouble())
            "stats.streak" -> Value.Num(stats.currentStreak.toDouble())
            "stats.best" -> Value.Num(stats.longestStreak.toDouble())
            "stats.days" -> Value.Num(stats.activeDays.toDouble())
            "stats.library" -> Value.Num(stats.libraryTitles.toDouble())
            "stats.favorites" -> Value.Num(stats.favorites.toDouble())
            "stats.unread" -> Value.Num(stats.unreadChapters.toDouble())
            "stats.pace" -> Value.Num(stats.perWeek)
            else -> null
        }
    }

    fun formula(source: String): Value = evaluateFormula(source, context) { name -> values[name] ?: data(name) }

    // A value in the script: text is a formula, and a number or yes or no is itself.
    fun value(element: JsonElement): Value = when {
        element is JsonPrimitive && element.isString -> formula(element.content)
        element is JsonPrimitive && element.booleanOrNull != null -> Value.Flag(element.booleanOrNull!!)
        element is JsonPrimitive && element.doubleOrNull != null -> Value.Num(element.doubleOrNull!!)
        else -> throw ScriptError(Res.string.script_err_value)
    }

    // Text with formulas in braces worked out, as "{done} of {goal}".
    fun text(template: String): String =
        Placeholder.replace(template) { match -> formula(match.groupValues[1]).asText() }

    fun view(element: JsonElement, at: String): BlockView {
        val obj = element as? JsonObject ?: throw ScriptError(Res.string.script_err_view_object).placed(at)
        val kind =
            ViewKeys.firstOrNull { it in obj }
                ?: throw ScriptError(Res.string.script_err_view_kind, ViewKeys.joinToString()).placed(at)
        fun label(key: String = "label"): String = (obj[key] as? JsonPrimitive)
            ?.takeIf { it !is JsonNull }
            ?.content
            ?.let(::text)
            .orEmpty()
        fun main(): Value = value(obj.getValue(kind))
        fun number(key: String, fallback: Double): Double = obj[key]?.let(::value)?.asNumber() ?: fallback

        // A setting written as plain text, such as a style or a colour, which is never a formula.
        fun word(key: String): String? = (obj[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
        fun flag(key: String): Boolean? = (obj[key] as? JsonPrimitive)?.booleanOrNull
        return try {
            when (kind) {
                "number" -> BlockView.Figure(main().asText(), label(), label("caption"), word("color"))

                "ring", "bar" -> {
                    val amount = main().asNumber()
                    val max = number("max", 100.0)
                    val share = if (max == 0.0) 0f else (amount / max).toFloat().coerceIn(0f, 1f)
                    if (kind == "bar") {
                        BlockView.Bar(share, "${numberText(amount, 1)} / ${numberText(max, 1)}", label(), word("color"))
                    } else {
                        BlockView.Ring(
                            share,
                            if (word("show") ==
                                "value"
                            ) {
                                numberText(amount, 1)
                            } else {
                                "${numberText(share * 100.0, 0)}%"
                            },
                            label(),
                            word("color"),
                        )
                    }
                }

                "chart" -> {
                    val points = main() as? Value.Series ?: throw ScriptError(Res.string.script_err_chart_series)
                    val style = word("style")
                    val chartKind = if (style == null) {
                        ChartKind.Bars
                    } else {
                        ChartKind.entries.firstOrNull { it.name.equals(style, ignoreCase = true) }
                            ?: throw ScriptError(
                                Res.string.script_err_chart_style,
                                ChartKind.entries.joinToString { it.name.lowercase() },
                            )
                    }
                    val colors = (obj["colors"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()
                    BlockView.Chart(
                        points.points,
                        label(),
                        ChartStyle(
                            kind = chartKind,
                            color = word("color"),
                            colors = colors,
                            values = flag("values"),
                            labels = flag("labels") ?: true,
                            height = obj["height"]
                                ?.let(::value)
                                ?.asNumber()
                                ?.toInt()
                                ?.coerceIn(40, 400),
                            max = obj["max"]?.let(::value)?.asNumber(),
                            smooth = flag("smooth") ?: false,
                        ),
                    )
                }

                "list" -> BlockView.Items(rows(main()), label())

                "covers" -> BlockView.Covers(
                    (main() as? Value.Titles)?.items ?: throw ScriptError(Res.string.script_err_covers),
                    label(),
                )

                "heatmap" -> {
                    val reads = main() as? Value.Reads ?: throw ScriptError(Res.string.script_err_heatmap)
                    val shape = word("shape")?.let { name ->
                        CellShape.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                            ?: throw ScriptError(Res.string.script_err_heatmap_shape)
                    }
                    BlockView.Heatmap(
                        reads.items
                            .groupingBy {
                                context.dayOf(it.at)
                            }.eachCount(),
                        context.today,
                        number("weeks", 26.0).toInt().coerceIn(4, 53),
                        label(),
                        word("color"),
                        shape ?: CellShape.Round,
                    )
                }

                "text" -> {
                    val style = (obj["style"] as? JsonPrimitive)?.content
                    BlockView.Words(
                        text((obj.getValue("text") as? JsonPrimitive)?.content.orEmpty()),
                        WordStyle.entries.firstOrNull { it.name.equals(style, ignoreCase = true) } ?: WordStyle.Body,
                    )
                }

                "stack", "row" -> {
                    val parts = obj.getValue(kind) as? JsonArray ?: throw ScriptError(Res.string.script_err_parts, kind)
                    BlockView.Stack(
                        parts.mapIndexed { index, part -> view(part, "$at.$kind[$index]") },
                        row =
                        kind == "row",
                    )
                }

                "section" -> {
                    val name = (obj.getValue("section") as? JsonPrimitive)?.content.orEmpty()
                    BlockView.Classic(
                        ClassicSection.entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                            ?: throw ScriptError(Res.string.script_err_section),
                    )
                }

                else -> if (main().asBool()) {
                    view(obj["then"] ?: throw ScriptError(Res.string.script_err_if_then), "$at.then")
                } else {
                    obj["else"]?.let { view(it, "$at.else") }
                        ?: BlockView.Empty
                }
            }
        } catch (error: ScriptError) {
            throw error.placed("$at.$kind")
        }
    }

    // The lines of a list: the points of a series with their values, or titles with their unread chapters.
    private fun rows(list: Value): List<ItemRow> = when (list) {
        is Value.Series -> list.points.map { ItemRow(it.label, numberText(it.value), name = it.name) }

        is Value.Titles -> list.items.map { ItemRow(it.name, if (it.unread > 0) it.unread.toString() else "", it) }

        is Value.Reads ->
            list.items
                .sortedByDescending { it.at }
                .mapNotNull { context.titleById[it.titleId] }
                .distinctBy { it.id }
                .map { ItemRow(it.name, "", it) }

        else -> listOf(ItemRow(list.asText(), ""))
    }
}
