package com.sumizuri.app.data.blocks

import com.sumizuri.app.data.model.Title
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.script_err_count_by
import com.sumizuri.app.shared.resources.script_err_inputs
import com.sumizuri.app.shared.resources.script_err_need_series
import com.sumizuri.app.shared.resources.script_err_no_context
import com.sumizuri.app.shared.resources.script_err_read_keys
import com.sumizuri.app.shared.resources.script_err_sort
import com.sumizuri.app.shared.resources.script_err_sort_titles
import com.sumizuri.app.shared.resources.script_err_title_keys
import com.sumizuri.app.shared.resources.script_err_titles_of

// The keys a list can be counted by with countBy.
private val TitleKeys = setOf("genre", "kind", "status", "source", "author")
private val ReadKeys = setOf("day", "weekday", "time", "hour", "month", "title") + TitleKeys

// The names an item of a list has inside a condition: a title its name, author, kind, status, genres, source, unread, favorite, rating, chapters and added. A read has its day, hour, weekday, daysAgo and at, with the names of its title. A point of a series has its label and value.
internal fun itemFields(item: Any, context: BlockContext?): (String) -> Value? = when (item) {
    is Title -> { name -> titleField(item, name) }

    is ReadEvent -> { name -> readField(item, name, context) }

    is Point -> { name ->
        if (name == "label") {
            Value.Txt(item.label)
        } else if (name == "value") {
            Value.Num(item.value)
        } else {
            null
        }
    }

    else -> { _ -> null }
}

private fun titleField(title: Title, name: String): Value? = when (name) {
    "name", "title" -> Value.Txt(title.name)
    "author" -> Value.Txt(title.author)
    "kind" -> Value.Txt(title.kind.name)
    "status" -> Value.Txt(title.status.name)
    "genres" -> Value.Txt(title.genres.joinToString(", "))
    "source" -> Value.Txt(title.sourceId)
    "unread" -> Value.Num(title.unread.toDouble())
    "favorite" -> Value.Flag(title.favorite)
    "rating" -> Value.Num(title.rating ?: 0.0)
    "chapters" -> Value.Num(title.chapterCount.toDouble())
    "added" -> Value.Num(title.addedAt.toDouble())
    else -> null
}

private fun readField(read: ReadEvent, name: String, context: BlockContext?): Value? {
    if (context == null) return null
    return when (name) {
        "at" -> Value.Num(read.at.toDouble())
        "day" -> Value.Num(context.dayOf(read.at).toDouble())
        "daysAgo" -> Value.Num((context.today - context.dayOf(read.at)).toDouble())
        "hour" -> Value.Num(context.hourOf(read.at).toDouble())
        "weekday" -> Value.Num(weekdayIndex(context.dayOf(read.at)) + 1.0)
        else -> context.titleById[read.titleId]?.let { titleField(it, name) }
    }
}

private fun itemsOf(value: Value): List<Any> = when (value) {
    is Value.Titles -> value.items
    is Value.Reads -> value.items
    is Value.Series -> value.points
    else -> throw ScriptError(Res.string.script_err_need_series)
}

// A list of the same kind as another, made of some of its items.
private fun sameKind(like: Value, items: List<Any>): Value = when (like) {
    is Value.Titles -> Value.Titles(items.filterIsInstance<Title>())
    is Value.Reads -> Value.Reads(items.filterIsInstance<ReadEvent>())
    else -> Value.Series(items.filterIsInstance<Point>())
}

private fun FormulaRun.needContext(): BlockContext = context ?: throw ScriptError(Res.string.script_err_no_context)

// The functions on lists, or null when the call is not one of them. Conditions and values per item are worked out for each item, with its names.
internal fun FormulaRun.listCall(expr: Expr.Call): Value? {
    val args = expr.args
    fun arg(index: Int): Value =
        eval(args.getOrNull(index) ?: throw ScriptError(Res.string.script_err_inputs, expr.name, index + 1))
    fun perItem(list: Value, index: Int): List<Pair<Any, Value>> {
        val part = args.getOrNull(index) ?: throw ScriptError(Res.string.script_err_inputs, expr.name, index + 1)
        return itemsOf(list).map { item -> item to forItem(item).eval(part) }
    }
    return when (expr.name) {
        // The chapters read in the last few days, today counting as one. 'today' is today alone, and no range takes them all.
        "reads" -> {
            val context = needContext()
            val range = args.getOrNull(0)?.let(::eval)
            val days = when {
                range == null -> null
                range is Value.Txt && range.value.equals("all", ignoreCase = true) -> null
                range is Value.Txt && range.value.equals("today", ignoreCase = true) -> 1
                else -> range.asNumber().toInt().coerceAtLeast(1)
            }
            Value.Reads(
                if (days ==
                    null
                ) {
                    context.reads
                } else {
                    (context.today - days + 1).let { first ->
                        context.reads.filter {
                            context.dayOf(it.at) >=
                                first
                        }
                    }
                },
            )
        }

        // The titles of some reads, the newest read first, each title once.
        "titlesof" -> {
            val context = needContext()
            val reads = arg(0) as? Value.Reads ?: throw ScriptError(Res.string.script_err_titles_of)
            Value.Titles(
                reads.items
                    .sortedByDescending { it.at }
                    .mapNotNull { context.titleById[it.titleId] }
                    .distinctBy { it.id },
            )
        }

        "filter" -> {
            val list = arg(0)
            sameKind(list, perItem(list, 1).filter { (_, keep) -> keep.asBool() }.map { it.first })
        }

        "count" -> {
            val list = arg(0)
            Value.Num(
                if (args.size >
                    1
                ) {
                    perItem(list, 1).count { (_, keep) -> keep.asBool() }.toDouble()
                } else {
                    list.size().toDouble()
                },
            )
        }

        "sumof" -> Value.Num(perItem(arg(0), 1).sumOf { it.second.asNumber() })

        "avgof" -> Value.Num(
            perItem(arg(0), 1).map { it.second.asNumber() }.let { if (it.isEmpty()) 0.0 else it.average() },
        )

        "maxof" -> Value.Num(perItem(arg(0), 1).maxOfOrNull { it.second.asNumber() } ?: 0.0)

        "minof" -> Value.Num(perItem(arg(0), 1).minOfOrNull { it.second.asNumber() } ?: 0.0)

        "countby" -> {
            val list = arg(0)
            val key = arg(1).asText().lowercase()
            when (list) {
                is Value.Titles -> if (key in
                    TitleKeys
                ) {
                    Value.Series(groupTitles(list.items, key))
                } else {
                    throw ScriptError(Res.string.script_err_title_keys, TitleKeys.joinToString())
                }

                is Value.Reads -> if (key in
                    ReadKeys
                ) {
                    Value.Series(groupReads(needContext(), list.items, key))
                } else {
                    throw ScriptError(Res.string.script_err_read_keys, ReadKeys.joinToString())
                }

                else -> throw ScriptError(Res.string.script_err_count_by)
            }
        }

        // The biggest few of a series, or the first few of any other list.
        "top" -> {
            val list = arg(0)
            val count = arg(1).asNumber().toInt().coerceAtLeast(0)
            if (list is Value.Series) {
                Value.Series(
                    list.points
                        .sortedByDescending {
                            it.value
                        }.take(count),
                )
            } else {
                sameKind(list, itemsOf(list).take(count))
            }
        }

        "take" -> {
            val list = arg(0)
            sameKind(list, itemsOf(list).take(arg(1).asNumber().toInt().coerceAtLeast(0)))
        }

        "reverse" -> arg(0).let { list -> sameKind(list, itemsOf(list).asReversed()) }

        "sort" -> sort(arg(0), args.getOrNull(1)?.let(::eval)?.asText(), args.getOrNull(2)?.let(::eval)?.asText())

        // The label of the first or last point of a series, or the name of the first or last title.
        "first", "last" -> {
            val items = itemsOf(arg(0))
            when (val item = if (expr.name == "first") items.firstOrNull() else items.lastOrNull()) {
                is Point -> Value.Txt(item.label)

                is Title -> Value.Txt(item.name)

                is ReadEvent -> Value.Txt(
                    context
                        ?.titleById
                        ?.get(item.titleId)
                        ?.name
                        .orEmpty(),
                )

                else -> Value.Txt("")
            }
        }

        else -> null
    }
}

// Sorts titles by name, added, unread, rating, chapters or last read, a series by value or label, and reads by time. Names and labels go up unless told 'desc', and the rest go down unless told 'asc'.
private fun FormulaRun.sort(list: Value, by: String?, order: String?): Value {
    val key = by?.lowercase()
    val ascending = when (order?.lowercase()) {
        "asc" -> true
        "desc" -> false
        else -> (key == null && list is Value.Titles) || key == "name" || key == "label"
    }
    val sorted: List<Any> = when (list) {
        is Value.Titles -> {
            val lastRead by lazy {
                HashMap<Long, Long>().also { last ->
                    context?.reads?.forEach {
                        if (it.at >
                            (last[it.titleId] ?: 0)
                        ) {
                            last[it.titleId] = it.at
                        }
                    }
                }
            }
            when (key ?: "name") {
                "name" -> list.items.sortedBy { it.name.lowercase() }
                "added" -> list.items.sortedBy { it.addedAt }
                "unread" -> list.items.sortedBy { it.unread }
                "rating" -> list.items.sortedBy { it.rating ?: -1.0 }
                "chapters" -> list.items.sortedBy { it.chapterCount }
                "last" -> list.items.sortedBy { lastRead[it.id] ?: 0 }
                else -> throw ScriptError(Res.string.script_err_sort_titles)
            }
        }

        is Value.Series -> if (key ==
            "label"
        ) {
            list.points.sortedBy { it.label.lowercase() }
        } else {
            list.points.sortedBy { it.value }
        }

        is Value.Reads -> list.items.sortedBy { it.at }

        else -> throw ScriptError(Res.string.script_err_sort)
    }
    return sameKind(list, if (ascending) sorted else sorted.asReversed())
}
