package com.sumizuri.app.data.sources

import com.sumizuri.app.data.db.AppDatabase
import com.sumizuri.app.data.model.Repo
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.source.SourceResult
import com.sumizuri.app.source.repo.InstalledSource
import com.sumizuri.app.source.repo.RepoClient
import com.sumizuri.app.source.repo.RepoSource
import kotlinx.coroutines.flow.first

// How a source of the old repo was found in the new one: by the same id, by the same site under another id, by the same name, or not at all.
enum class RepoMatch { SameId, SameSite, SameName, None }

// One installed source of the old repo, what it was matched to in the new repo, and how many titles it holds.
data class RepoMoveItem(val installed: InstalledSource, val match: RepoSource?, val how: RepoMatch, val titles: Int)

// What a move of the sources of one repo to another would do, worked out before anything changes.
data class RepoMovePlan(val from: Repo, val to: Repo, val items: List<RepoMoveItem>)

// What a move did: the sources installed from the new repo, the titles that followed a source to its new id, those that stayed because the new source had them already, and the sources that failed or had no match.
data class RepoMoveResult(
    val moved: Int,
    val titlesMoved: Int,
    val titlesKept: Int,
    val failed: List<String>,
    val unmatched: List<String>,
)

// Moves the sources installed from one repo to another, such as a repo that moved to a new address or a copy of it with more sources. Each source is matched in the new repo by its id, and else by its site or its name. A source with the same id is installed again from the new repo and the library does not change. A source found under another id is installed too, and the titles of the old one move to it unless the new one has the same title already.
class RepoMigration(
    private val client: RepoClient,
    private val sources: SourceRepository,
    private val db: AppDatabase,
) {
    suspend fun plan(from: Repo, to: Repo): SourceResult<RepoMovePlan> {
        val index = when (val read = client.fetchIndex(to.url)) {
            is SourceResult.Err -> return read
            is SourceResult.Ok -> read.value
        }
        val mine = sources.installed.first().filter { it.repoUrl == from.url }
        val items = mine.map { source ->
            val byId = index.sources.firstOrNull { it.id == source.id }
            val bySite = index.sources.firstOrNull {
                it.kind == source.kind && siteOf(it.baseUrl) != null &&
                    siteOf(it.baseUrl) == siteOf(source.baseUrl)
            }
            val byName = index.sources.firstOrNull { it.kind == source.kind && nameOf(it.name) == nameOf(source.name) }
            val (match, how) = when {
                byId != null -> byId to RepoMatch.SameId
                bySite != null -> bySite to RepoMatch.SameSite
                byName != null -> byName to RepoMatch.SameName
                else -> null to RepoMatch.None
            }
            RepoMoveItem(source, match, how, db.titles().countOf(source.id))
        }
        return SourceResult.Ok(RepoMovePlan(from, to, items.sortedBy { it.how }))
    }

    // Carries out a plan, one source at a time, telling how far it has got.
    suspend fun run(plan: RepoMovePlan, onProgress: (done: Int, total: Int) -> Unit): RepoMoveResult {
        val matched = plan.items.filter { it.match != null }
        var moved = 0
        var titlesMoved = 0
        var titlesKept = 0
        val failed = mutableListOf<String>()
        matched.forEachIndexed { at, item ->
            onProgress(at, matched.size)
            val target = item.match ?: return@forEachIndexed
            when (val got = client.fetchSource(target, plan.to.url)) {
                is SourceResult.Err -> failed += item.installed.name

                is SourceResult.Ok -> {
                    sources.save(got.value)
                    moved++
                    if (target.id != item.installed.id) {
                        val went = db.titles().moveSource(item.installed.id, target.id, nowMillis())
                        titlesMoved += went
                        titlesKept += item.titles - went
                    }
                }
            }
        }
        onProgress(matched.size, matched.size)
        val unmatched = plan.items.filter { it.match == null }.map { it.installed.name }
        return RepoMoveResult(moved, titlesMoved, titlesKept, failed, unmatched)
    }
}

// The host of a site without its www, so two sources of the same site match however their addresses were written.
private fun siteOf(address: String): String? {
    val host = address
        .substringAfter(
            "://",
            "",
        ).substringBefore('/')
        .substringBefore(':')
        .lowercase()
        .removePrefix("www.")
    return host.takeIf { it.contains('.') }
}

// A name with only its letters and digits, so Asura Scans and AsuraScans match.
private fun nameOf(name: String): String = name.lowercase().filter { it.isLetterOrDigit() }
