package com.sumizuri.app.ui.kit.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.sumizuri.app.shared.resources.Res
import com.sumizuri.app.shared.resources.markdown_caution
import com.sumizuri.app.shared.resources.markdown_important
import com.sumizuri.app.shared.resources.markdown_note
import com.sumizuri.app.shared.resources.markdown_tip
import com.sumizuri.app.shared.resources.markdown_warning
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// One block of a Markdown text.
private sealed interface Block {
    data class Heading(val level: Int, val text: String) : Block
    data class Paragraph(val text: String) : Block

    // A list item, with how far it is indented and its mark, a dot or a number.
    data class Item(val depth: Int, val mark: String, val text: String) : Block
    data class Code(val text: String) : Block

    // A quote holds blocks of its own. A GitHub note such as [!TIP] on its first line gives it a kind.
    data class Quote(val kind: Alert?, val blocks: List<Block>) : Block
    data object Rule : Block
}

// The kinds of notes GitHub draws as colored quotes.
private enum class Alert(val label: StringResource) {
    Note(Res.string.markdown_note),
    Tip(Res.string.markdown_tip),
    Important(Res.string.markdown_important),
    Warning(Res.string.markdown_warning),
    Caution(Res.string.markdown_caution),
}

// Release notes and the like as they are written on GitHub and GitLab: headings, lists, quotes and notes, code, rules, and bold, italic, struck, code and links inside the lines. Comments and the common HTML tags are left out, and anything else shows as plain text.
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { blocksOf(text.replace(Comment, "")) }
    val colors = MaterialTheme.colorScheme
    val styles = InlineStyles(
        TextLinkStyles(SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline)),
        SpanStyle(fontFamily = FontFamily.Monospace, background = colors.surfaceContainerHighest),
    )
    Blocks(blocks, styles, modifier)
}

private class InlineStyles(val links: TextLinkStyles, val code: SpanStyle)

@Composable
private fun Blocks(blocks: List<Block>, styles: InlineStyles, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (block in blocks) {
            when (block) {
                is Block.Heading -> Text(
                    inline(block.text, styles),
                    Modifier.padding(top = if (block.level <= 2) 6.dp else 2.dp),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.Bold,
                )

                is Block.Paragraph -> Text(inline(block.text, styles), style = MaterialTheme.typography.bodyMedium)

                is Block.Item -> Row(
                    Modifier.padding(start = (block.depth * 16).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        block.mark,
                        Modifier.widthIn(min = 14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.primary,
                    )
                    Text(inline(block.text, styles), style = MaterialTheme.typography.bodyMedium)
                }

                is Block.Code -> Text(
                    block.text,
                    Modifier
                        .fillMaxWidth()
                        .background(
                            colors.surfaceContainerHighest,
                            MaterialTheme.shapes.small,
                        ).padding(10.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )

                is Block.Quote -> {
                    val tint = when (block.kind) {
                        null -> colors.outlineVariant
                        Alert.Note -> colors.primary
                        Alert.Tip -> colors.tertiary
                        Alert.Important -> colors.secondary
                        Alert.Warning, Alert.Caution -> colors.error
                    }
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        Box(
                            Modifier
                                .padding(end = 10.dp)
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(tint),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            block.kind?.let {
                                Text(
                                    stringResource(it.label),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = tint,
                                )
                            }
                            if (block.blocks.isNotEmpty()) Blocks(block.blocks, styles)
                        }
                    }
                }

                Block.Rule -> HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant)
            }
        }
    }
}

private val Comment = Regex("""<!--[\s\S]*?-->""")
private val Html =
    Regex(
        """</?(details|summary|br|p|div|span|img|picture|source|sup|sub|b|i|em|strong|kbd|a|h[1-6]|ul|ol|li|code|pre|hr|center)\b[^>]*>""",
        RegexOption.IGNORE_CASE,
    )
private val Bullet = Regex("""^(\s*)[-*+]\s+(.*)$""")
private val Numbered = Regex("""^(\s*)(\d+)[.)]\s+(.*)$""")
private val HeadingLine = Regex("""^(#{1,6})\s+(.*?)\s*#*$""")
private val RuleLine = Regex("""^([-*_])(\s*\1){2,}$""")
private val AlertLine = Regex("""^\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION)]\s*$""", RegexOption.IGNORE_CASE)

private fun blocksOf(text: String): List<Block> {
    val blocks = mutableListOf<Block>()
    val paragraph = StringBuilder()
    fun flush() {
        if (paragraph.isNotBlank()) blocks += Block.Paragraph(paragraph.toString().trim())
        paragraph.clear()
    }
    val lines = text.replace("\r\n", "\n").split('\n')
    var i = 0
    while (i < lines.size) {
        val raw = lines[i]
        val fence = raw.trim().startsWith("```")
        // Tags only go away outside code, where they are part of the text.
        val line = if (fence) raw else raw.replace(Html, "")
        val trimmed = line.trim()
        when {
            fence -> {
                flush()
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) code.appendLine(lines[i++])
                blocks += Block.Code(code.toString().trimEnd())
            }

            // The lines of a quote that follow each other make one quote, read as Markdown of its own.
            trimmed.startsWith(">") -> {
                flush()
                val inner = mutableListOf<String>()
                while (i < lines.size &&
                    lines[i].trim().startsWith(">")
                ) {
                    inner += lines[i++].trim().removePrefix(">").removePrefix(" ")
                }
                i--
                val kind = inner
                    .firstOrNull()
                    ?.let {
                        AlertLine.find(it.trim())
                    }?.let { match -> Alert.entries.first { it.name.equals(match.groupValues[1], ignoreCase = true) } }
                blocks += Block.Quote(kind, blocksOf((if (kind != null) inner.drop(1) else inner).joinToString("\n")))
            }

            trimmed.isEmpty() -> flush()

            RuleLine.matches(trimmed) -> {
                flush()
                blocks += Block.Rule
            }

            HeadingLine.matches(trimmed) -> {
                flush()
                val match = HeadingLine.find(trimmed)!!
                blocks += Block.Heading(match.groupValues[1].length, match.groupValues[2])
            }

            Bullet.matches(line) -> {
                flush()
                val match = Bullet.find(line)!!
                blocks += Block.Item(match.groupValues[1].length / 2, "•", match.groupValues[2])
            }

            Numbered.matches(line) -> {
                flush()
                val match = Numbered.find(line)!!
                blocks += Block.Item(match.groupValues[1].length / 2, match.groupValues[2] + ".", match.groupValues[3])
            }

            else -> paragraph.append(if (paragraph.isEmpty()) trimmed else " $trimmed")
        }
        i++
    }
    flush()
    return blocks
}

// The marks inside a line: **bold**, *italic* or _italic_, `code`, [words](address), plain addresses, ![a picture](address) shown as a link, and ~~struck~~ words.
private val Inline =
    Regex(
        """\*\*(.+?)\*\*|__(.+?)__|`([^`]+)`|\[([^\]]+)]\(([^)\s]+)\)|(?<![\w*])\*(?!\s)(.+?)\*(?!\w)|(?<!\w)_(?!\s)(.+?)_(?!\w)|(https?://[^\s)]+)|!\[([^\]]*)]\(([^)\s]+)\)|~~(.+?)~~""",
    )

private fun inline(text: String, styles: InlineStyles): AnnotatedString = buildAnnotatedString {
    var at = 0
    for (match in Inline.findAll(text)) {
        append(text.substring(at, match.range.first))
        val groups = match.groupValues
        when {
            groups[1].isNotEmpty() || groups[2].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(groups[1].ifEmpty { groups[2] })
            }

            groups[3].isNotEmpty() -> withStyle(styles.code) { append(groups[3]) }

            groups[4].isNotEmpty() -> withLink(LinkAnnotation.Url(groups[5], styles.links)) { append(groups[4]) }

            groups[6].isNotEmpty() || groups[7].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(groups[6].ifEmpty { groups[7] })
            }

            groups[8].isNotEmpty() -> withLink(LinkAnnotation.Url(groups[8], styles.links)) { append(groups[8]) }

            groups[10].isNotEmpty() -> withLink(LinkAnnotation.Url(groups[10], styles.links)) {
                append(groups[9].ifEmpty { groups[10] })
            }

            groups[11].isNotEmpty() -> withStyle(
                SpanStyle(textDecoration = TextDecoration.LineThrough),
            ) { append(groups[11]) }

            else -> append(match.value)
        }
        at = match.range.last + 1
    }
    append(text.substring(at))
}
