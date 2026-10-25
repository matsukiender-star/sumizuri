package com.sumizuri.app.feature.reader.lookup

// Finds the word under a press in a block of text.
object WordPick {
    // The longest run of characters taken from a press in a language written without spaces.
    const val MaxRun = 6

    fun isCjk(char: Char): Boolean = char in '぀'..'ヿ' || char in '㐀'..'鿿' || char in '豈'..'﫿'

    private fun inWord(char: Char) = char.isLetter() || char == '\'' || char == '’' || char == '-'

    // The word at the offset, or null when the press is on a space or a mark. For Japanese and Chinese it is the run of characters from the press on, as there are no spaces to find the end by.
    fun at(text: String, offset: Int): String? {
        if (text.isEmpty()) return null
        var at = offset.coerceIn(0, text.length - 1)
        if (!inWord(text[at]) && at > 0 && inWord(text[at - 1])) at--
        if (!inWord(text[at])) return null
        if (isCjk(text[at])) {
            var end = at
            while (end < text.length && isCjk(text[end]) && end - at < MaxRun) end++
            return text.substring(at, end)
        }
        var start = at
        while (start > 0 && inWord(text[start - 1]) && !isCjk(text[start - 1])) start--
        var end = at + 1
        while (end < text.length && inWord(text[end]) && !isCjk(text[end])) end++
        return text.substring(start, end).trim { it == '\'' || it == '’' || it == '-' }.ifEmpty { null }
    }
}
