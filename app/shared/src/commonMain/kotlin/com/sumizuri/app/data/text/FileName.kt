package com.sumizuri.app.data.text

private const val Forbidden = "\\/:*?\"<>|"

// Names a system keeps for itself, which a file cannot have.
private val Reserved = setOf("con", "prn", "aux", "nul") + (1..9).map { "com$it" } + (1..9).map { "lpt$it" }

// A file name made from a name a person gave. Marks a file name cannot hold become underscores, an empty name becomes the fallback, and a name that is taken, whatever its case, gets a number. The extension starts with its dot, or is empty.
fun safeFileName(
    name: String,
    extension: String,
    taken: Collection<String>,
    fallback: String,
    longest: Int = Int.MAX_VALUE,
): String {
    val clean = name
        .map { if (it in Forbidden || it.code < 32) '_' else it }
        .joinToString("")
        .trim()
        .trim('.')
        .take(longest)
        .trim()
    val base = clean.ifEmpty { fallback }.let { if (it.lowercase() in Reserved) "${it}_" else it }
    val used = taken.map { it.lowercase() }.toSet()
    var candidate = base + extension
    var count = 2
    while (candidate.lowercase() in used) {
        candidate = "$base ($count)$extension"
        count++
    }
    return candidate
}
