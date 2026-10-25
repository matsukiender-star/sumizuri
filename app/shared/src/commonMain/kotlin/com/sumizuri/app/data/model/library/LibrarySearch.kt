package com.sumizuri.app.data.model.library

// A search typed into the library. Words must all be in the name or the author, a word with a minus in front must not be, and a phrase in quotes is one word. A word can also begin with the name of what to look in, which is status, kind, source, shelf, genre, author or unread, and a colon. For unread the rest is a number, or a number with a greater or a smaller sign in front.
class LibrarySearch private constructor(private val terms: List<Term>) {
    val isEmpty: Boolean get() = terms.isEmpty()

    fun matches(target: SearchTarget): Boolean = terms.all { it.matches(target) }

    private class Term(val field: String?, val text: String, val negated: Boolean) {
        fun matches(target: SearchTarget): Boolean = holds(target) != negated

        private fun holds(target: SearchTarget): Boolean = when (field) {
            null -> text in target.name || text in target.author
            "status" -> target.status.startsWith(text)
            "kind", "type" -> target.kind.startsWith(text)
            "source" -> text in target.source
            "shelf", "category" -> target.shelves.any { text in it }
            "genre" -> target.genres.any { text in it }
            "author" -> text in target.author
            "unread" -> unread(target.unread)
            else -> text in target.name || text in target.author
        }

        private fun unread(count: Int): Boolean = when {
            text.startsWith(">") -> text.drop(1).toIntOrNull()?.let { count > it } ?: true
            text.startsWith("<") -> text.drop(1).toIntOrNull()?.let { count < it } ?: true
            text == "yes" -> count > 0
            text == "no" -> count == 0
            else -> text.toIntOrNull()?.let { count == it } ?: true
        }
    }

    companion object {
        private val Fields = setOf("status", "kind", "type", "source", "shelf", "category", "genre", "author", "unread")

        fun parse(input: String): LibrarySearch {
            val terms = mutableListOf<Term>()
            var at = 0
            while (at < input.length) {
                if (input[at] == ' ') {
                    at++
                    continue
                }
                val negated = input[at] == '-' && at + 1 < input.length && input[at + 1] != ' '
                if (negated) at++
                val start = at
                var inQuotes = false
                while (at < input.length && (inQuotes || input[at] != ' ')) {
                    if (input[at] == '"') inQuotes = !inQuotes
                    at++
                }
                val word = input.substring(start, at).replace("\"", "").lowercase()
                if (word.isEmpty()) continue
                val colon = word.indexOf(':')
                val field = if (colon > 0) word.substring(0, colon).takeIf { it in Fields } else null
                terms +=
                    if (field != null) Term(field, word.substring(colon + 1), negated) else Term(null, word, negated)
            }
            return LibrarySearch(terms)
        }
    }
}

// What a search looks at in one title of the library. The words are lower case.
class SearchTarget(
    val name: String,
    val author: String,
    val genres: List<String>,
    val status: String,
    val kind: String,
    val source: String,
    val shelves: List<String>,
    val unread: Int,
)
