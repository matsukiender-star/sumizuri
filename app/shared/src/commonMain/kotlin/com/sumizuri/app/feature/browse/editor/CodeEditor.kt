package com.sumizuri.app.feature.browse.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// How far in a step of indent goes.
private const val Indent = "  "

// A field for code: the numbers of the lines at the side, the words colored by what they are, and lines that do not wrap, so a long one scrolls to the side and the numbers stay beside their lines. Tab steps in, and a new line starts as far in as the one before it.
@Composable
fun CodeEditor(value: TextFieldValue, onValue: (TextFieldValue) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val colors =
        CodeColors(
            keyword = scheme.primary,
            text = scheme.tertiary,
            key = scheme.secondary,
            number = scheme.tertiary,
            comment = scheme.outline,
        )
    val highlight = remember(colors) { CodeHighlight(colors) }
    val style =
        TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp, color = scheme.onSurface)
    val lines = remember(value.text) { value.text.count { it == '\n' } + 1 }
    val numbers = remember(lines) { (1..lines).joinToString("\n") }
    Surface(modifier, color = scheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val height = maxHeight
            Row(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Text(
                    numbers,
                    Modifier.padding(start = 10.dp, end = 8.dp, top = 12.dp, bottom = 12.dp).widthIn(min = 20.dp),
                    style = style.copy(color = scheme.outline, textAlign = TextAlign.End),
                    softWrap = false,
                )
                BoxWithConstraints(Modifier.weight(1f)) {
                    val width = maxWidth
                    BasicTextField(
                        value = value,
                        onValueChange = { onValue(indentNewLine(value, it)) },
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .widthIn(min = width)
                            .heightIn(min = height)
                            .padding(top = 12.dp, bottom = 12.dp, end = 16.dp)
                            .onPreviewKeyEvent { event ->
                                if (event.type != KeyEventType.KeyDown ||
                                    event.key != Key.Tab
                                ) {
                                    return@onPreviewKeyEvent false
                                }
                                onValue(if (event.isShiftPressed) stepOut(value) else typed(value, Indent))
                                true
                            },
                        textStyle = style,
                        cursorBrush = SolidColor(scheme.primary),
                        visualTransformation = highlight,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Ascii,
                        ),
                    )
                }
            }
        }
    }
}

// The text with words typed in place of the selection.
private fun typed(value: TextFieldValue, words: String): TextFieldValue {
    val start = value.selection.min
    val text = value.text.substring(0, start) + words + value.text.substring(value.selection.max)
    return TextFieldValue(text, TextRange(start + words.length))
}

// Takes a step of indent away from the start of the line the cursor is on.
private fun stepOut(value: TextFieldValue): TextFieldValue {
    val text = value.text
    val lineStart = text.lastIndexOf('\n', (value.selection.min - 1).coerceAtLeast(0)).let {
        if (value.selection.min ==
            0
        ) {
            0
        } else {
            it + 1
        }
    }
    val spaces = text
        .drop(lineStart)
        .takeWhile { it == ' ' }
        .length
        .coerceAtMost(Indent.length)
    if (spaces == 0) return value
    val cursor = (value.selection.min - spaces).coerceAtLeast(lineStart)
    return TextFieldValue(text.removeRange(lineStart, lineStart + spaces), TextRange(cursor))
}

// When a line break was just typed, the new line starts as far in as the one before it, and a step further after an opening bracket.
private fun indentNewLine(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
    val at = new.selection.start
    val one =
        new.selection.collapsed && old.selection.collapsed && new.text.length == old.text.length + 1 &&
            old.selection.start == at - 1
    if (!one || new.text[at - 1] != '\n') return new
    val lineStart = new.text.lastIndexOf('\n', at - 2) + 1
    val indent = new.text.substring(lineStart, at - 1).takeWhile { it == ' ' || it == '\t' }
    val opened = at >= 2 && new.text[at - 2] in "{[("
    val added = indent + if (opened) Indent else ""
    if (added.isEmpty()) return new
    return TextFieldValue(new.text.substring(0, at) + added + new.text.substring(at), TextRange(at + added.length))
}
