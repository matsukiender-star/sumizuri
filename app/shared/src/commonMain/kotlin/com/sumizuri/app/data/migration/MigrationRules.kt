package com.sumizuri.app.data.migration

import kotlinx.serialization.Serializable

// What a match must look like in its chapters to be worth moving to.
@Serializable
enum class ChapterRule { Any, AtLeastAsMany, AtLeastAsNew, CoversProgress }

// How close a name must be to count as the same title.
@Serializable
enum class Strictness { Strict, Balanced, Loose }

// How a search picks and accepts matches, and how a move is done. Every choice is kept between runs.
@Serializable
data class MigrationRules(
    val chapterRule: ChapterRule = ChapterRule.Any,
    val strictness: Strictness = Strictness.Balanced,
    // A clear match is chosen without asking. Off sends every match to review.
    val autoAccept: Boolean = true,
    // Of results that match about as well, the one with most chapters comes first.
    val preferMoreChapters: Boolean = false,
    // The chapters of the closest results are read and compared, which tells apart titles with the same name but costs a request for each. Off, the search goes by name and author alone, and the rules about chapters do not apply.
    val readChapters: Boolean = false,
    // How many results of one source have their chapters read to be compared.
    val looksAt: Int = 3,
    // A result by a different named author is left out.
    val requireAuthor: Boolean = false,
    // When the whole name finds nothing, the part before a colon or a dash and the first words are searched too.
    val tryShorterName: Boolean = true,
    // The address the title has now is tried on a source of the same site before anything is searched.
    val tryAddress: Boolean = true,
    // Every source is searched even for a title that has a match, and the best match of all is kept.
    val compareAll: Boolean = false,
    // How many titles one source is asked about at once, and the pause after each answer in milliseconds.
    val parallel: Int = 2,
    val delayMs: Int = 300,
    // A source that fails this many times in a row is left out of the rest of the run.
    val giveUpAfter: Int = 5,
    // A result with fewer chapters than this is left out.
    val minChapters: Int = 0,
    // Moving: keep the old title in the library, keep its shelves and its star, and move into a title that is in the library already.
    val keepOld: Boolean = false,
    val keepShelves: Boolean = true,
    val keepFavorite: Boolean = true,
    val allowMerge: Boolean = false,
) {
    // The score a match needs to be chosen without asking, and the score under which a result is not shown at all.
    val foundAt: Double
        get() = when (strictness) {
            Strictness.Strict -> 0.93
            Strictness.Balanced -> 0.85
            Strictness.Loose -> 0.75
        }
    val reviewAt: Double
        get() = when (strictness) {
            Strictness.Strict -> 0.65
            Strictness.Balanced -> 0.5
            Strictness.Loose -> 0.4
        }
}
