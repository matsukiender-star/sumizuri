package com.sumizuri.app.data.migration

// What is known about a title that is to be moved: its name and author, the numbers of the chapters it has, and the chapter the reader is up to.
class MigrationSubject(val title: String, val author: String?, val chapterNumbers: Set<Double>, val lastRead: Double?)

// What a source has for a name that was searched: the name, the author and the numbers of its chapters when they were read.
class MigrationCandidate(val title: String, val author: String?, val chapterNumbers: Set<Double>?)

// How well a candidate fits, as a whole and by its name and its chapters.
data class MigrationScore(val total: Double, val title: Double, val chapters: Double?)

// What came of the search for one title.
enum class MigrationVerdict { Found, Review, NotFound }

// Where a title is in the work.
enum class MigrationStatus { Queued, Searching, Found, Review, NotFound, Moved, Failed }

// Why a move was refused.
enum class MoveProblem { AlreadyInLibrary, ChaptersUnavailable, SourceUnavailable }

// What the search of one source for one title came to: the verdict, the options in order, how many results the rules ruled out, and the message of the source when it could not be searched at all.
class MigrationFound(
    val verdict: MigrationVerdict,
    val options: List<MigrationOption>,
    val ruledOut: Int,
    val failure: String?,
)
