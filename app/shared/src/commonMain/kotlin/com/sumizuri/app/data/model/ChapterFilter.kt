package com.sumizuri.app.data.model

// How the chapter list treats one mark: every chapter, only the chapters with the mark, or only the ones without it.
enum class TriFilter {
    Any,
    Only,
    Without,
    ;

    // The next way, as a tap on the filter goes round them.
    fun next(): TriFilter = entries[(ordinal + 1) % entries.size]

    fun keeps(has: Boolean): Boolean = when (this) {
        Any -> true
        Only -> has
        Without -> !has
    }

    companion object {
        fun of(name: String?): TriFilter = entries.firstOrNull { it.name == name } ?: Any
    }
}

// Which chapters the lists show, kept for every title: by being unread, saved to the device and bookmarked.
data class ChapterFilter(
    val unread: TriFilter = TriFilter.Any,
    val downloaded: TriFilter = TriFilter.Any,
    val bookmarked: TriFilter = TriFilter.Any,
) {
    val active: Boolean get() = unread != TriFilter.Any || downloaded != TriFilter.Any || bookmarked != TriFilter.Any
}

// One way the chapters of a title can differ, and one of its values: the group that put a chapter out, its language, or another label the source gives, whose kind starts with tag:.
data class Facet(val kind: String, val value: String) {
    // The name of a label the source gave, without its mark, or null for the group and the language.
    val label: String? get() = kind.removePrefix(TagMark).takeIf { kind.startsWith(TagMark) }

    companion object {
        const val Group = "group"
        const val Language = "language"
        const val TagMark = "tag:"
    }
}

// Between the name and the value of a label, and between a kind and its value, as kept in the database and the settings.
private const val Between = '\u001F'

// The labels of a chapter as kept in the database: one per line, the name and the value split by an unusual mark. Null when there are none.
fun chapterTagsText(tags: Map<String, String>): String? = tags
    .takeIf { it.isNotEmpty() }
    ?.entries
    ?.sortedBy { it.key }
    ?.joinToString("\n") { (name, value) -> name.replace('\n', ' ') + Between + value.replace('\n', ' ') }

fun chapterTags(text: String?): Map<String, String> {
    if (text.isNullOrEmpty()) return emptyMap()
    return text
        .split('\n')
        .mapNotNull { line ->
            val at = line.indexOf(Between)
            if (at <= 0) null else line.substring(0, at) to line.substring(at + 1)
        }.toMap()
}

// The ways one chapter is told apart from its copies.
fun facetsOf(scanlator: String?, language: String?, tags: Map<String, String>): List<Facet> = buildList {
    scanlator?.let { add(Facet(Facet.Group, it)) }
    language?.let { add(Facet(Facet.Language, it)) }
    for ((name, value) in tags) add(Facet(Facet.TagMark + name, value))
}

// The facets a title hides, as kept in the settings: one per line.
fun facetsText(facets: Set<Facet>): String =
    facets.sortedWith(compareBy({ it.kind }, { it.value })).joinToString("\n") {
        it.kind +
            Between +
            it.value
    }

fun facetsFrom(text: String?): Set<Facet> = text.orEmpty().split('\n').mapNotNullTo(HashSet()) { line ->
    val at = line.indexOf(Between)
    if (at <= 0) null else Facet(line.substring(0, at), line.substring(at + 1))
}
