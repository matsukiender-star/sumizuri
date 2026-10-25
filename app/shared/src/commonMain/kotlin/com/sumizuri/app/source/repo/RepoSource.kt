package com.sumizuri.app.source.repo

import com.sumizuri.app.data.model.Kind

// One source a repo lists. Its code is fetched from the file address only when it is installed or updated.
data class RepoSource(
    val id: String,
    val name: String,
    val lang: String,
    val kind: Kind,
    val engineKind: EngineKind,
    val version: Int,
    val iconUrl: String,
    val baseUrl: String,
    val fileUrl: String,
    // The repo's own claim. It only feeds a filter and nothing is blocked because of it.
    val nsfw: Boolean,
    // What the engine keeps from the index entry as JSON text, such as the allowed hosts and the settings of an extension source.
    val extra: String = "",
    // The hash the index gives for the file, which the download must match.
    val sha256: String? = null,
)

data class RepoIndex(val name: String, val sources: List<RepoSource>)

// The repo's index is not in the format a repo should use.
class RepoFormatException(message: String) : Exception(message)

// The mark between the id a repo gave a source and the short code of its repo.
private const val RepoMark = "@"

// The id the repo gave the source, without the code of the repo that may follow it.
val InstalledSource.originId: String get() = id.substringBefore(RepoMark)

// The id to install a source under. A source that is already installed from this repo keeps its id. Otherwise it gets the id its repo gave it, unless something else already holds that id, such as the same name from another repo, and then the code of its repo follows the id so both can be installed.
fun installIdFor(repoUrl: String?, sourceId: String, installed: List<InstalledSource>): String {
    installed.firstOrNull { it.repoUrl == repoUrl && it.originId == sourceId }?.let { return it.id }
    if (installed.none { it.id == sourceId }) return sourceId
    return sourceId + RepoMark + (repoUrl ?: "").hashCode().toUInt().toString(16)
}

// The id a repo gave a source, taken from the id it is installed under.
fun originIdOf(installId: String): String = installId.substringBefore(RepoMark)

// The sources of the index that are installed from this repo in an older version, so each one has an update. A source of the same name installed from another repo does not count.
fun updatesAvailable(installed: List<InstalledSource>, index: RepoIndex, repoUrl: String?): List<RepoSource> {
    val versions = installed.filter { it.repoUrl == repoUrl }.associate { it.originId to it.version }
    return index.sources.filter { source -> versions[source.id]?.let { it < source.version } == true }
}

// How a version reads to a person. A whole number reads as it is, and a version of three numbers that was kept as one number reads as three again.
fun versionText(version: Int): String = if (version >=
    SEMVER_MAJOR
) {
    "${version / SEMVER_MAJOR}.${version / SEMVER_MINOR % SEMVER_MINOR}.${version % SEMVER_MINOR}"
} else {
    version.toString()
}

private const val SEMVER_MAJOR = 1_000_000
private const val SEMVER_MINOR = 1_000
