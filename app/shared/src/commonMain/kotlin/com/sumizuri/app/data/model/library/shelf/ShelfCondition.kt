package com.sumizuri.app.data.model.library.shelf

import com.sumizuri.app.data.backup.ImportedSources
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.model.Title
import com.sumizuri.app.data.model.library.Category
import com.sumizuri.app.data.model.library.LibraryEntry
import com.sumizuri.app.data.model.library.LibrarySortField
import com.sumizuri.app.data.text.enumNamed

// How many milliseconds a day is, for the age of a title in the library.
private const val DayMs = 86_400_000.0

// One condition of a shelf: what to look at, how to compare it and the value to compare it with. The value is kept as text, which is the name of a choice, a number, a piece of text, or true or false.
data class ShelfCondition(
    val field: ConditionField = ConditionField.Status,
    val op: ConditionOp = ConditionOp.Is,
    val value: String = "",
) {
    // A condition with no value says nothing, so it is left out.
    val complete: Boolean get() = value.isNotBlank()

    // Whether a title meets the condition.
    fun matches(title: Title, context: ShelfContext): Boolean = when (field.value) {
        ValueKind.Choice -> {
            val actual = if (field == ConditionField.Status) title.status.name else title.kind.name
            (actual == value) == (op == ConditionOp.Is)
        }

        ValueKind.Flag -> title.favorite == (value == "true")

        ValueKind.Number -> numeric(title, context)

        ValueKind.Text -> textual(title, context)
    }

    private fun numeric(title: Title, context: ShelfContext): Boolean {
        val wanted = value.trim().toDoubleOrNull() ?: return false
        val actual: Double = when (field) {
            ConditionField.Unread -> title.unread.toDouble()

            ConditionField.Chapters -> title.chapterCount.toDouble()

            ConditionField.Progress -> if (title.chapterCount <=
                0
            ) {
                0.0
            } else {
                (title.chapterCount - title.unread).coerceAtLeast(0) * 100.0 / title.chapterCount
            }

            ConditionField.Rating -> title.rating ?: return op == ConditionOp.IsNot

            // A title never read, or with no chapter dated, is as old as can be.
            ConditionField.Updated -> context.latest[title.id]?.takeIf { it > 0 }?.let { (context.now - it) / DayMs }
                ?: Double.MAX_VALUE

            ConditionField.LastRead -> context.lastRead[title.id]?.takeIf { it > 0 }?.let { (context.now - it) / DayMs }
                ?: Double.MAX_VALUE

            else -> (context.now - title.addedAt) / DayMs
        }
        return when (op) {
            ConditionOp.Is -> actual == wanted
            ConditionOp.IsNot -> actual != wanted
            ConditionOp.Greater -> actual > wanted
            else -> actual < wanted
        }
    }

    private fun textual(title: Title, context: ShelfContext): Boolean {
        val pieces = when (field) {
            ConditionField.Source -> listOf(
                context.sourceNames[title.sourceId] ?: ImportedSources.labelOf(title.sourceId) ?: title.sourceId,
            )

            ConditionField.Author -> listOf(title.author)

            ConditionField.Genre -> title.genres

            else -> listOf(title.name)
        }
        val wanted = value.trim()
        return when (op) {
            ConditionOp.Contains -> pieces.any { it.contains(wanted, ignoreCase = true) }
            ConditionOp.NotContains -> pieces.none { it.contains(wanted, ignoreCase = true) }
            ConditionOp.Is -> pieces.any { it.equals(wanted, ignoreCase = true) }
            else -> pieces.none { it.equals(wanted, ignoreCase = true) }
        }
    }
}

// The conditions of a shelf as text, one condition to a line with its three parts between bars, so a shelf keeps them in one column. A bar, a line break or a percent sign in a value is written as a percent sign and a code.
object ShelfConditions {
    fun encode(list: List<ShelfCondition>): String =
        list.joinToString("\n") { "${it.field.name}|${it.op.name}|${escape(it.value)}" }

    fun decode(text: String): List<ShelfCondition> = text.lines().mapNotNull { line ->
        val parts = line.split("|")
        if (parts.size != 3) return@mapNotNull null
        val field = enumNamed<ConditionField>(parts[0]) ?: return@mapNotNull null
        val op = enumNamed<ConditionOp>(parts[1])?.takeIf { it in field.ops } ?: field.ops.first()
        ShelfCondition(field, op, unescape(parts[2]))
    }

    private fun escape(value: String): String = value.replace("%", "%25").replace("|", "%7C").replace("\n", "%0A")

    private fun unescape(value: String): String = value.replace("%0A", "\n").replace("%7C", "|").replace("%25", "%")
}

// What the conditions of a shelf need to know besides a title: the names of the installed sources, the time now in milliseconds, and when each title was last read and had its newest chapter, for the conditions that ask.
class ShelfContext(
    val sourceNames: Map<String, String>,
    val now: Long,
    val lastRead: Map<Long, Long> = emptyMap(),
    val latest: Map<Long, Long> = emptyMap(),
) {
    companion object {
        // The context with the times the entries of the library carry.
        fun of(sourceNames: Map<String, String>, now: Long, entries: List<LibraryEntry>) = ShelfContext(
            sourceNames,
            now,
            entries.filter { it.lastRead > 0 }.associate { it.title.id to it.lastRead },
            entries.filter { it.latest > 0 }.associate { it.title.id to it.latest },
        )
    }
}

// The times the shelves ask about, which the library then reads for them.
fun List<Category>.timesAsked(): Set<LibrarySortField> = buildSet {
    val fields = this@timesAsked.flatMap { shelf -> shelf.conditions.filter { it.complete }.map { it.field } }
    if (ConditionField.Updated in fields) add(LibrarySortField.LatestChapter)
    if (ConditionField.LastRead in fields) add(LibrarySortField.LastRead)
}

// The shelves that show for a kind of title: the ones made for it and the ones made for every kind. With no kind, which is all the titles, every shelf shows.
fun List<Category>.forKind(kind: Kind?): List<Category> = if (kind ==
    null
) {
    this
} else {
    filter { it.kind == null || it.kind == kind }
}
