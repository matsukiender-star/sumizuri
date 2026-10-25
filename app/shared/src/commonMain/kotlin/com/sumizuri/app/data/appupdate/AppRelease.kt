package com.sumizuri.app.data.appupdate

import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull
import kotlin.time.Instant

// One version of the app as its repo lists it: its tag, its name, the notes of what changed, when it came out, whether it is a test version, its page and its files.
data class AppRelease(
    val version: String,
    val name: String,
    val notes: String,
    val publishedAt: Long?,
    val prerelease: Boolean,
    val pageUrl: String,
    val assets: List<ReleaseAsset>,
)

// A file of a release, such as the APK for Android or the installer for a computer.
data class ReleaseAsset(val name: String, val url: String, val size: Long?)

// Where the releases are kept: GitHub, or GitLab on its own site or on another.
sealed interface UpdateSource {
    val pageUrl: String

    data class GitHub(val owner: String, val repo: String) : UpdateSource {
        override val pageUrl: String get() = "https://github.com/$owner/$repo/releases"
        val releasesApi: String get() = "https://api.github.com/repos/$owner/$repo/releases?per_page=30"
        val tagsApi: String get() = "https://api.github.com/repos/$owner/$repo/tags?per_page=30"
    }

    data class GitLab(val host: String, val path: String) : UpdateSource {
        override val pageUrl: String get() = "https://$host/$path/-/releases"
        private val project: String get() = path.encodeURLPathPart().replace("/", "%2F")
        val releasesApi: String get() = "https://$host/api/v4/projects/$project/releases?per_page=30"
        val tagsApi: String get() = "https://$host/api/v4/projects/$project/repository/tags?per_page=30"
    }

    companion object {
        // Reads an address such as https://github.com/owner/repo, gitlab.com/group/project, a GitLab on a site of its own, or just owner/repo for GitHub. Null when it cannot be read.
        fun of(address: String): UpdateSource? {
            val text = address
                .trim()
                .removeSuffix("/")
                .removeSuffix(".git")
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
            if (text.isEmpty()) return null
            val parts = text.split('/').filter { it.isNotBlank() }
            return when {
                parts.size == 2 && !parts[0].contains('.') -> UpdateSource.GitHub(parts[0], parts[1])

                parts.size >= 3 && parts[0] == "github.com" -> UpdateSource.GitHub(parts[1], parts[2])

                parts.size >= 3 -> UpdateSource.GitLab(
                    parts[0],
                    parts.drop(1).takeWhile { it != "-" }.joinToString("/"),
                )

                else -> null
            }
        }
    }
}

// The releases in a reply of GitHub, without drafts.
internal fun gitHubReleases(reply: JsonElement): List<AppRelease> =
    (reply as? JsonArray).orEmpty().mapNotNull { element ->
        val json = element as? JsonObject ?: return@mapNotNull null
        if (json.flag("draft")) return@mapNotNull null
        val tag = json.text("tag_name") ?: return@mapNotNull null
        AppRelease(
            version = versionOf(tag),
            name = json.text("name")?.takeIf { it.isNotBlank() } ?: tag,
            notes = json.text("body").orEmpty(),
            publishedAt = parseIsoMillis(json.text("published_at")),
            prerelease = json.flag("prerelease"),
            pageUrl = json.text("html_url").orEmpty(),
            assets = (json["assets"] as? JsonArray).orEmpty().mapNotNull { asset ->
                val file = asset as? JsonObject ?: return@mapNotNull null
                ReleaseAsset(
                    file.text("name") ?: return@mapNotNull null,
                    file.text("browser_download_url") ?: return@mapNotNull null,
                    file.number("size"),
                )
            },
        )
    }

// The releases in a reply of GitLab. GitLab marks no test versions, so a tag with a dash in it, such as 1.2.0-beta, counts as one.
internal fun gitLabReleases(reply: JsonElement, source: UpdateSource.GitLab): List<AppRelease> =
    (reply as? JsonArray).orEmpty().mapNotNull { element ->
        val json = element as? JsonObject ?: return@mapNotNull null
        if (json.flag("upcoming_release")) return@mapNotNull null
        val tag = json.text("tag_name") ?: return@mapNotNull null
        val links = ((json["assets"] as? JsonObject)?.get("links") as? JsonArray).orEmpty()
        AppRelease(
            version = versionOf(tag),
            name = json.text("name")?.takeIf { it.isNotBlank() } ?: tag,
            notes = json.text("description").orEmpty(),
            publishedAt = parseIsoMillis(json.text("released_at")),
            prerelease = versionOf(tag).contains('-'),
            pageUrl = ((json["_links"] as? JsonObject)?.text("self")) ?: source.pageUrl,
            assets = links.mapNotNull { link ->
                val file = link as? JsonObject ?: return@mapNotNull null
                ReleaseAsset(
                    file.text("name") ?: return@mapNotNull null,
                    file.text("direct_asset_url") ?: file.text("url") ?: return@mapNotNull null,
                    null,
                )
            },
        )
    }

// Tags with no releases, for a repo that only tags its versions. They have no files, and GitLab gives the message of a tag as its notes.
internal fun tagReleases(reply: JsonElement, source: UpdateSource): List<AppRelease> =
    (reply as? JsonArray).orEmpty().mapNotNull { element ->
        val json = element as? JsonObject ?: return@mapNotNull null
        val tag = json.text("name") ?: return@mapNotNull null
        val notes = json.text("message") ?: ((json["release"] as? JsonObject)?.text("description")).orEmpty()
        AppRelease(versionOf(tag), tag, notes, null, versionOf(tag).contains('-'), source.pageUrl, emptyList())
    }

// A tag as a version: v1.2.0 and release-1.2.0 are 1.2.0.
internal fun versionOf(tag: String): String = tag
    .trim()
    .substringAfterLast('/')
    .trimStart('v', 'V')
    .let { text -> text.dropWhile { !it.isDigit() }.ifEmpty { text } }

// Compares two versions part by part, so 0.10.0 is after 0.9.0. A test version such as 1.2.0-beta comes before 1.2.0 itself.
fun compareVersions(a: String, b: String): Int {
    val (mainA, testA) = split(a)
    val (mainB, testB) = split(b)
    for (i in 0 until maxOf(mainA.size, mainB.size)) {
        val diff = (mainA.getOrElse(i) { 0 }).compareTo(mainB.getOrElse(i) { 0 })
        if (diff != 0) return diff
    }
    return when {
        testA == null && testB == null -> 0
        testA == null -> 1
        testB == null -> -1
        else -> compareTest(testA, testB)
    }
}

private fun split(version: String): Pair<List<Int>, String?> {
    val clean = versionOf(version).substringBefore('+')
    val main = clean.substringBefore('-')
    val test = clean.substringAfter('-', "").ifEmpty { null }
    return main.split('.').map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 } to test
}

// Test parts such as beta.2 and beta.10, compared by their numbers where they have them.
private fun compareTest(a: String, b: String): Int {
    val partsA = a.split('.', '-')
    val partsB = b.split('.', '-')
    for (i in 0 until maxOf(partsA.size, partsB.size)) {
        val x = partsA.getOrNull(i) ?: return -1
        val y = partsB.getOrNull(i) ?: return 1
        val diff = if (x.toIntOrNull() != null &&
            y.toIntOrNull() != null
        ) {
            x.toInt().compareTo(y.toInt())
        } else {
            x.compareTo(y)
        }
        if (diff != 0) return diff
    }
    return 0
}

private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

private fun JsonObject.flag(key: String): Boolean = (this[key] as? JsonPrimitive)?.booleanOrNull ?: false

private fun JsonObject.number(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull

// An ISO date as epoch milliseconds. A date with no zone is read as UTC.
private fun parseIsoMillis(text: String?): Long? {
    if (text.isNullOrBlank()) return null
    val trimmed = text.trim()
    val hasZone =
        trimmed.endsWith("Z", ignoreCase = true) ||
            Regex("[+-]\\d\\d:?\\d\\d$").containsMatchIn(trimmed.substringAfter('T', ""))
    val withZone = if (trimmed.contains('T') && !hasZone) "${trimmed}Z" else trimmed
    return try {
        Instant.parse(withZone).toEpochMilliseconds()
    } catch (_: IllegalArgumentException) {
        null
    }
}
