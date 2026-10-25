package com.sumizuri.app.data.local

import com.fleeksoft.ksoup.nodes.Entities
import io.ktor.http.decodeURLPart

private val Spanning = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
private val FullPath = Regex("full-path\\s*=\\s*\"([^\"]+)\"")
private val ItemTag = Regex("<item\\b[^>]*>", RegexOption.IGNORE_CASE)
private val ItemRefTag = Regex("<itemref\\b[^>]*>", RegexOption.IGNORE_CASE)
private val CoverMeta = Regex("<meta\\b[^>]*name\\s*=\\s*\"cover\"[^>]*>", RegexOption.IGNORE_CASE)
private val NavLink = Regex("<a\\b[^>]*href\\s*=\\s*\"([^\"]+)\"[^>]*>(.*?)</a>", Spanning)
private val NcxPoint =
    Regex("<navLabel>\\s*<text>(.*?)</text>\\s*</navLabel>\\s*<content\\b[^>]*src\\s*=\\s*\"([^\"]+)\"", Spanning)
private val PictureTag =
    Regex("<(?:img|image)\\b[^>]*?(?:src|xlink:href|href)\\s*=\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
private val Attribute = Regex("([\\w:-]+)\\s*=\\s*\"([^\"]*)\"")
private val Body = Regex("<body[^>]*>(.*)</body>", Spanning)
private val Hidden = Regex("<(script|style|head)[^>]*>.*?</\\1>", Spanning)
private val LineBreak = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
private val BlockEnd = Regex("</(p|div|h[1-6]|li|blockquote|section|tr)>", RegexOption.IGNORE_CASE)
private val Tag = Regex("<[^>]+>")
private val Spaces = Regex("[ \\t\\u00A0]+")
private val BlankLines = Regex("\\n{3,}")

// An EPUB file read just enough to show it: the documents of its reading order with the names its table of contents gives them, its cover, and the pictures and the words of each document.
class Epub(private val path: String) {
    private val entries: Set<String> by lazy { archiveEntries(path).toSet() }

    private fun text(entry: String): String = archiveEntry(path, entry).decodeToString()

    // Where the package file is, which lists everything else.
    private val packagePath: String by lazy {
        val container = runCatching { text("META-INF/container.xml") }.getOrDefault("")
        FullPath.find(container)?.groupValues?.get(1) ?: entries.firstOrNull { it.endsWith(".opf") }
            ?: error("Not an EPUB: no package file")
    }

    private val packageText: String by lazy { text(packagePath) }

    private val base: String get() = packagePath.substringBeforeLast('/', "")

    // The files the package lists, by their id: where each is and what it is.
    private val manifest: Map<String, ManifestItem> by lazy {
        ItemTag
            .findAll(packageText)
            .mapNotNull { tag ->
                val attributes = attributesOf(tag.value)
                val id = attributes["id"] ?: return@mapNotNull null
                val href = attributes["href"] ?: return@mapNotNull null
                id to
                    ManifestItem(
                        resolve(base, href),
                        attributes["media-type"].orEmpty(),
                        attributes["properties"].orEmpty(),
                    )
            }.toMap()
    }

    private class ManifestItem(val path: String, val type: String, val properties: String)

    // The documents in the order they are read.
    val documents: List<String> by lazy {
        ItemRefTag
            .findAll(packageText)
            .mapNotNull { attributesOf(it.value)["idref"]?.let(manifest::get)?.path }
            .filter { it in entries }
            .toList()
    }

    // One fact the package gives about the book, such as its name, as plain text.
    private fun fact(name: String): String? = Regex("<dc:$name[^>]*>(.*?)</dc:$name>", Spanning)
        .find(packageText)
        ?.groupValues
        ?.get(1)
        ?.let(::plainText)
        ?.trim()
        ?.ifEmpty { null }

    val title: String? by lazy { fact("title") }

    val author: String? by lazy { fact("creator") }

    // Books often keep their description as escaped HTML, so the tags that come out of the entities are taken off too.
    val description: String? by lazy { fact("description")?.let(::htmlToText)?.ifEmpty { null } }

    // The cover picture: the one the package marks as the cover, or else the first picture of the book.
    val cover: String? by lazy {
        val marked = manifest.values.firstOrNull { "cover-image" in it.properties }?.path
            ?: CoverMeta.find(packageText)?.let { attributesOf(it.value)["content"] }?.let { manifest[it]?.path }
        marked?.takeIf { it in entries } ?: documents.firstNotNullOfOrNull { pictures(it).firstOrNull() }
    }

    // The names the table of contents gives the documents, by document.
    val names: Map<String, String> by lazy {
        val found = LinkedHashMap<String, String>()
        val nav = manifest.values.firstOrNull { "nav" in it.properties.split(' ') }?.path
        if (nav != null && nav in entries) {
            val folder = nav.substringBeforeLast('/', "")
            NavLink.findAll(text(nav)).forEach { link ->
                val label = plainText(link.groupValues[2]).trim()
                if (label.isNotEmpty()) {
                    found.getOrPut(
                        resolve(folder, link.groupValues[1].substringBefore('#')),
                    ) { label }
                }
            }
        }
        val ncx = manifest.values.firstOrNull { it.type == "application/x-dtbncx+xml" }?.path
        if (found.isEmpty() && ncx != null && ncx in entries) {
            val folder = ncx.substringBeforeLast('/', "")
            NcxPoint.findAll(text(ncx)).forEach { point ->
                val label = plainText(point.groupValues[1]).trim()
                if (label.isNotEmpty()) {
                    found.getOrPut(
                        resolve(folder, point.groupValues[2].substringBefore('#')),
                    ) { label }
                }
            }
        }
        found
    }

    // The pictures a document shows, in order.
    fun pictures(document: String): List<String> {
        val folder = document.substringBeforeLast('/', "")
        val markup = runCatching { text(document) }.getOrDefault("")
        return PictureTag
            .findAll(markup)
            .map { resolve(folder, it.groupValues[1]) }
            .filter { it in entries }
            .toList()
    }

    // The words of a document as paragraphs with blank lines between them.
    fun words(document: String): String = htmlToText(runCatching { text(document) }.getOrDefault(""))
}

// The attributes of one tag, by name.
private fun attributesOf(tag: String): Map<String, String> = Attribute.findAll(tag).associate {
    it.groupValues[1].lowercase() to
        it.groupValues[2]
}

// A path inside the archive from the folder of the file that names it, with ../ and %20 worked out.
internal fun resolve(folder: String, href: String): String {
    val decoded = runCatching { href.decodeURLPart() }.getOrDefault(href)
    val parts = (
        if (decoded.startsWith(
                "/",
            )
        ) {
            emptyList()
        } else {
            folder.split('/').filter { it.isNotEmpty() }
        }
        ).toMutableList()
    for (part in decoded.split('/')) {
        when (part) {
            "", "." -> Unit
            ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
            else -> parts += part
        }
    }
    return parts.joinToString("/")
}

// The words of a page of HTML, with a blank line after each paragraph, heading and list item, and the tags and the styling left out.
internal fun htmlToText(html: String): String {
    val body = Body.find(html)?.groupValues?.get(1) ?: html
    val marked = body.replace(Hidden, "").replace(LineBreak, "\n").replace(BlockEnd, "\n\n")
    return plainText(marked)
        .lines()
        .joinToString("\n") { it.replace(Spaces, " ").trim() }
        .replace(BlankLines, "\n\n")
        .trim()
}

// Text with its tags taken out and its entities written as letters.
internal fun plainText(markup: String): String = Entities.unescape(markup.replace(Tag, ""))
