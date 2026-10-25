package com.sumizuri.app.data.sync

// How many titles of the library have been checked so far, out of how many.
data class UpdateProgress(val done: Int, val total: Int)

// A title the check for new chapters could not ask, with its name, what went wrong and the name of its source, so the failures of one source can be shown together.
data class UpdateFailure(val titleId: Long, val name: String, val problem: String, val source: String = "")

// A title the last check found new chapters for, and how many.
data class ChapterNews(val titleId: Long, val name: String, val added: Int, val coverUrl: String? = null)
