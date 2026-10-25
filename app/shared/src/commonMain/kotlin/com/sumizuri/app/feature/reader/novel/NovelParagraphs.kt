package com.sumizuri.app.feature.reader.novel

import com.sumizuri.app.feature.reader.ReaderPage

// Text a source gives as one block is cut at blank lines. A block with hardly any blank lines but many line breaks is cut at the line breaks instead.
fun splitParagraphs(text: String): List<String> {
    val byBlankLine = text.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
    if (byBlankLine.size > 2 || text.count { it == '\n' } < 6) return byBlankLine
    return text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
}

// The pages of a text chapter as one page for each paragraph, so a long block becomes something that can be scrolled, paged and counted.
fun paragraphsOf(pages: List<ReaderPage>): List<ReaderPage> =
    pages.flatMap { splitParagraphs(it.text.orEmpty()) }.map { ReaderPage(null, it) }

// How many words a text holds. Languages written without spaces count about two characters to a word.
fun wordCount(pages: List<ReaderPage>): Int {
    var words = 0
    var characters = 0
    for (page in pages) {
        val text = page.text.orEmpty()
        words += text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
        characters += text.length
    }
    return if (words * 8 < characters) characters / 2 else words
}
