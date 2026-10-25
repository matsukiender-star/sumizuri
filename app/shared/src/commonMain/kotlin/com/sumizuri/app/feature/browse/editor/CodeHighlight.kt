package com.sumizuri.app.feature.browse.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// The colors of the kinds of words in the code.
data class CodeColors(val keyword: Color, val text: Color, val key: Color, val number: Color, val comment: Color)

// Code longer than this is shown without colors, so typing in a very large script stays quick.
private const val MostColored = 120_000

private val Keywords = setOf(
    "var", "let", "const", "function", "return", "if", "else", "for", "while", "do", "async", "await", "new", "throw", "try", "catch", "finally",
    "true", "false", "null", "undefined", "this", "typeof", "instanceof", "of", "in", "break", "continue", "switch", "case", "default", "class",
)

// Colors the code as it is shown, without changing the text, so the cursor and the selection stay where they are. The last text and its colors are kept, so moving the cursor does not color it all again.
class CodeHighlight(private val colors: CodeColors) : VisualTransformation {
    private var lastText: String? = null
    private var lastColored: AnnotatedString? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val colored = if (text.text == lastText) {
            lastColored!!
        } else {
            color(text.text).also {
                lastText = text.text
                lastColored = it
            }
        }
        return TransformedText(colored, OffsetMapping.Identity)
    }

    // Reads the code once from start to end. A pattern would be shorter, but the one the platform has goes deeper into itself for each letter of a long quoted text, and a large script of a site has quoted texts long enough to overflow it.
    private fun color(code: String): AnnotatedString = buildAnnotatedString {
        append(code)
        if (code.length > MostColored) return@buildAnnotatedString
        fun mark(start: Int, end: Int, color: Color) = addStyle(SpanStyle(color = color), start, end)
        val size = code.length
        var at = 0
        while (at < size) {
            val char = code[at]
            val next = if (at + 1 < size) code[at + 1] else ' '
            at = when {
                char == '/' && next == '/' -> code.indexOf('\n', at).let { if (it < 0) size else it }.also { mark(at, it, colors.comment) }

                char == '/' && next == '*' -> code.indexOf("*/", at + 2).let { if (it < 0) size else it + 2 }.also { mark(at, it, colors.comment) }

                // Quoted text that comes before a colon is the name of a field, as in JSON.
                char == '"' || char == '\'' || char == '`' -> quoteEnd(code, at).also { mark(at, it, if (isKey(code, it)) colors.key else colors.text) }

                char.isDigit() -> wordEnd(code, at).also { mark(at, it, colors.number) }

                char.isLetter() || char == '_' || char == '$' -> wordEnd(code, at).also { if (code.substring(at, it) in Keywords) mark(at, it, colors.keyword) }

                else -> at + 1
            }
        }
    }

    override fun equals(other: Any?) = other is CodeHighlight && other.colors == colors
    override fun hashCode() = colors.hashCode()
}

// Where a quoted text ends: after its closing quote, or at the end of the line when it is not closed. A backslash keeps the next letter in the text, and backticks may go over lines.
private fun quoteEnd(code: String, start: Int): Int {
    val quote = code[start]
    var at = start + 1
    while (at < code.length) {
        val char = code[at]
        when {
            char == '\\' -> at += 2
            char == quote -> return at + 1
            char == '\n' && quote != '`' -> return at
            else -> at++
        }
    }
    return code.length
}

// Where a name or a number ends.
private fun wordEnd(code: String, start: Int): Int {
    var at = start
    while (at < code.length && (code[at].isLetterOrDigit() || code[at] == '_' || code[at] == '$' || (code[at] == '.' && code[start].isDigit()))) at++
    return at
}

// True when the next thing after spaces is a colon.
private fun isKey(code: String, from: Int): Boolean {
    var at = from
    while (at < code.length && (code[at] == ' ' || code[at] == '\t')) at++
    return at < code.length && code[at] == ':'
}
