package com.sumizuri.app.data.migration

// How close the second best score may be to the best for the best to count as a clear match.
private const val Margin = 0.08

// Picks the candidates worth a look for a title, in order, and says whether one of them is a clear match.
object MigrationRanker {
    // The candidates that were kept by the order they were given in, with their scores, best first, and how many the rules ruled out before any was scored.
    class Ranking(val verdict: MigrationVerdict, val ranked: List<Pair<Int, MigrationScore>>, val excluded: Int)

    // Whether a candidate with these chapter numbers is worth moving to under a rule. A candidate whose chapters were not read passes, since there is nothing to judge it by.
    fun meetsChapterRule(subject: MigrationSubject, candidate: Set<Double>?, rule: ChapterRule): Boolean {
        if (rule == ChapterRule.Any || candidate == null) return true
        val mine = subject.chapterNumbers
        if (mine.isEmpty()) return true
        if (candidate.isEmpty()) return false
        return when (rule) {
            ChapterRule.Any -> true

            ChapterRule.AtLeastAsMany -> candidate.size >= mine.size

            ChapterRule.AtLeastAsNew -> candidate.max() >= mine.max() - 1e-6

            ChapterRule.CoversProgress -> {
                val read = subject.lastRead
                read == null || read <= 0 || candidate.max() >= read - 1e-6
            }
        }
    }

    private fun allowed(subject: MigrationSubject, candidate: MigrationCandidate, rules: MigrationRules): Boolean {
        if (!meetsChapterRule(subject, candidate.chapterNumbers, rules.chapterRule)) return false
        if (rules.minChapters > 0 && candidate.chapterNumbers != null &&
            candidate.chapterNumbers.size < rules.minChapters
        ) {
            return false
        }
        if (rules.requireAuthor) {
            val a = subject.author?.trim()?.lowercase()
            val b = candidate.author?.trim()?.lowercase()
            if (!a.isNullOrEmpty() && !b.isNullOrEmpty() && a != b) return false
        }
        return true
    }

    fun rank(
        subject: MigrationSubject,
        candidates: List<MigrationCandidate>,
        rules: MigrationRules,
        keep: Int = 3,
    ): Ranking {
        val usable = candidates.indices.filter { allowed(subject, candidates[it], rules) }
        val excluded = candidates.size - usable.size
        val scored = usable
            .map {
                it to MatchScoring.score(
                    subject,
                    candidates[it],
                )
            }.sortedByDescending { it.second.total }
        val worth = scored.filter { it.second.total >= rules.reviewAt }.take(keep)
        if (worth.isEmpty()) return Ranking(MigrationVerdict.NotFound, emptyList(), excluded)
        val best = worth.first().second.total
        val ordered: List<Pair<Int, MigrationScore>>
        val clear: Boolean
        if (rules.preferMoreChapters) {
            // Results that match about as well as the best are told apart by length, longest first, and their small gaps no longer count against being sure.
            val near = worth
                .withIndex()
                .filter { it.value.second.total >= best - Margin }
                .sortedWith(
                    compareByDescending<IndexedValue<Pair<Int, MigrationScore>>> {
                        candidates[it.value.first].chapterNumbers?.size
                            ?: 0
                    }.thenBy { it.index },
                ).map { it.value }
            ordered = near + worth.filter { it !in near }
            clear = ordered.first().second.total >= rules.foundAt
        } else {
            ordered = worth
            val runnerUp = if (worth.size > 1) worth[1].second.total else 0.0
            clear = best >= rules.foundAt && best - runnerUp >= Margin
        }
        return Ranking(
            if (clear &&
                rules.autoAccept
            ) {
                MigrationVerdict.Found
            } else {
                MigrationVerdict.Review
            },
            ordered,
            excluded,
        )
    }

    // The verdict for options that came from several sources, from their scores best first. A clear match is one that is good enough and well ahead of the next.
    fun verdictOf(scores: List<Double>, rules: MigrationRules): MigrationVerdict {
        if (scores.isEmpty()) return MigrationVerdict.NotFound
        val best = scores.first()
        val runnerUp = scores.getOrElse(1) { 0.0 }
        val clear = best >= rules.foundAt && best - runnerUp >= Margin
        return if (clear && rules.autoAccept) MigrationVerdict.Found else MigrationVerdict.Review
    }
}
