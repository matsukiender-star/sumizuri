package com.sumizuri.app.data.blocks

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

// A block as the simple editor makes it: what it shows, where the data comes from and how it looks, all picked with taps. It writes the script of the block, and keeps itself in the script under "simple", which the script itself leaves alone, so the simple editor can open the block again.
@Serializable
data class SimpleBlock(
    // number, ring, bar, chart, list, covers, heatmap, text or section.
    val show: String = "number",
    val title: String = "",
    val label: String = "",
    val caption: String = "",
    // For a number, ring or bar: one of SimpleValues, or "formula" for the formula typed.
    val value: String = "stats.streak",
    val formula: String = "",
    val goal: Double = 100.0,
    // For a chart or a list: what is counted, library or reads, by which key, over how many days back for reads, 0 for all of them, and how many of the biggest are kept, 0 for all.
    val source: String = "library",
    val by: String = "genre",
    val days: Int = 0,
    val top: Int = 6,
    // For covers: which titles and how many.
    val titles: String = "recent",
    val count: Int = 12,
    val style: String = "bars",
    // A colour name, or empty for the colour of the theme.
    val color: String = "",
    val weeks: Int = 26,
    val shape: String = "round",
    val text: String = "",
    val textStyle: String = "body",
    val section: String = "figures",
)

// The values a number, ring or bar can show, by what the simple editor calls them, with their formulas.
val SimpleValues: List<Pair<String, String>> = listOf(
    "stats.streak" to "stats.streak",
    "stats.read" to "stats.read",
    "stats.today" to "stats.today",
    "stats.week" to "stats.week",
    "stats.month" to "stats.month",
    "stats.best" to "stats.best",
    "stats.days" to "stats.days",
    "stats.pace" to "round(stats.pace, 1)",
    "stats.library" to "stats.library",
    "stats.favorites" to "stats.favorites",
    "stats.unread" to "stats.unread",
    "finished" to "count(library, status == 'Completed' && unread == 0)",
    "reading" to "count(library, unread > 0)",
)

// The keys a chart or a list counts by, for the library and for reads. Days, weekdays, times, hours and months keep their own order, so they are never cut to the biggest.
val SimpleLibraryKeys = listOf("genre", "kind", "status", "source", "author")
val SimpleReadKeys = listOf("weekday", "day", "time", "hour", "month", "title", "genre", "kind")
private val OrderedKeys = setOf("day", "weekday", "time", "hour", "month")

// The titles covers can show.
val SimpleTitles = listOf("recent", "added", "unread", "favorites", "rating")

private val Pretty = Json { prettyPrint = true }

private fun reads(days: Int) = if (days > 0) "reads($days)" else "reads"

// The formula of the value of a number, ring or bar.
fun SimpleBlock.valueFormula(): String = if (value ==
    "formula"
) {
    formula.ifBlank { "0" }
} else {
    SimpleValues.firstOrNull { it.first == value }?.second ?: "0"
}

// The formula of the series of a chart or a list.
fun SimpleBlock.seriesFormula(): String {
    val counted = if (source == "reads") "countBy(${reads(days)}, '$by')" else "countBy(library, '$by')"
    return if (top > 0 && by !in OrderedKeys) "top($counted, $top)" else counted
}

// The formula of the titles of covers.
fun SimpleBlock.titlesFormula(): String = when (titles) {
    "added" -> "take(sort(library, 'added', 'desc'), $count)"
    "unread" -> "take(sort(filter(library, unread > 0), 'unread', 'desc'), $count)"
    "favorites" -> "take(filter(library, favorite), $count)"
    "rating" -> "take(sort(library, 'rating', 'desc'), $count)"
    else -> "take(titlesOf(${reads(if (days > 0) days else 30)}), $count)"
}

private fun JsonObjectBuilder.putIfSet(key: String, value: String) {
    if (value.isNotBlank()) put(key, value)
}

// The view the block shows, as the script writes it.
private fun SimpleBlock.view(): JsonObject = buildJsonObject {
    when (show) {
        "ring", "bar" -> {
            put(show, valueFormula())
            put("max", goal)
            putIfSet("label", label)
        }

        "chart" -> {
            put("chart", seriesFormula())
            put("style", style)
            putIfSet("label", label)
        }

        "list" -> {
            put("list", seriesFormula())
            putIfSet("label", label)
        }

        "covers" -> {
            put("covers", titlesFormula())
            putIfSet("label", label)
        }

        "heatmap" -> {
            put("heatmap", reads(if (days > 0) days else 365))
            put("weeks", weeks)
            put("shape", shape)
            putIfSet("label", label)
        }

        "text" -> {
            put("text", text)
            put("style", textStyle)
        }

        "section" -> put("section", section)

        else -> {
            put("number", valueFormula())
            putIfSet("label", label)
            putIfSet("caption", caption)
        }
    }
    if (show != "text" && show != "section" && show != "list" && show != "covers") putIfSet("color", color)
}

// The script of the block, with the choices kept in it.
fun SimpleBlock.script(): String = Pretty.encodeToString(
    JsonObject.serializer(),
    buildJsonObject {
        putIfSet("title", title)
        put("view", view())
        put("simple", Json.encodeToJsonElement(SimpleBlock.serializer(), this@script))
    },
)

// The simple block a script was made from, when it was made by the simple editor and has not been changed by hand since. A script of a part of the profile alone, as the profile came with, counts as one too.
fun simpleOf(script: String): SimpleBlock? {
    val root = runCatching { Json.parseToJsonElement(script).jsonObject }.getOrNull() ?: return null
    val kept = root["simple"]?.let {
        runCatching {
            Json.decodeFromJsonElement(
                SimpleBlock.serializer(),
                it,
            )
        }.getOrNull()
    }
    if (kept != null) return kept.takeIf { same(it.script(), root) }
    val view = root["view"] as? JsonObject
    val section = (view?.get("section") as? JsonPrimitive)?.content
    return if (section != null && root.keys == setOf("view") &&
        view.keys == setOf("section")
    ) {
        SimpleBlock(show = "section", section = section.lowercase())
    } else {
        null
    }
}

private fun same(script: String, root: JsonElement): Boolean = runCatching {
    Json.parseToJsonElement(script) == root
}.getOrDefault(false)
