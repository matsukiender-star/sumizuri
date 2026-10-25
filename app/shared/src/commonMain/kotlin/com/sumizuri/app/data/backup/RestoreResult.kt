package com.sumizuri.app.data.backup

import com.sumizuri.app.data.model.Kind

// A title that was imported without a source of its own, to be tried against the installed sources. originId is the id a repo gave the source of the title, for a title whose source was installed from another repo.
class SourceCandidate(
    val titleId: Long,
    val kind: Kind,
    val url: String,
    val sourceName: String?,
    val originId: String? = null,
)

// What a restore or an import did.
class RestoreResult(
    // Titles that are new in the library.
    val added: Int,
    // Titles that were already there, which a restore of a Sumizuri backup fills in and an import leaves alone.
    val skipped: Int,
    // Titles that could not be put in.
    val failed: Int,
    val candidates: List<SourceCandidate>,
    // True when the person stopped it before every title was done.
    val cancelled: Boolean,
)

// The id a title gets when it comes from a backup and its source is not one that is installed. It holds the app the backup came from and the name of the source there, so the person can see where it was and move it to a source they have.
object ImportedSources {
    private const val Prefix = "imported:"

    fun idOf(app: BackupApp, name: String?): String = "$Prefix${app.name}:${name.orEmpty()}"

    fun isImported(sourceId: String): Boolean = sourceId.startsWith(Prefix)

    // The name the other app gave the source, or null for an id that is not one of these or has no name.
    fun nameOf(sourceId: String): String? =
        if (!isImported(sourceId)) null else sourceId.removePrefix(Prefix).substringAfter(':', "").ifBlank { null }

    // The name to show for the source of an imported title, such as MangaDex (Mihon), or null for an id that is not one of these.
    fun labelOf(sourceId: String): String? {
        if (!isImported(sourceId)) return null
        val app = sourceId.removePrefix(Prefix).substringBefore(':')
        val name = sourceId.removePrefix(Prefix).substringAfter(':', "")
        return if (name.isEmpty()) app else "$name ($app)"
    }
}
