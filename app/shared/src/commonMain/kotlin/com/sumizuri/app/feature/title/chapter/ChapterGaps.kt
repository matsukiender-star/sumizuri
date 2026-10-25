package com.sumizuri.app.feature.title.chapter

import com.sumizuri.app.feature.title.ChapterItem
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.roundToInt

// The whole chapter numbers from 1 up to the highest that no chapter has.
fun missingNumbers(chapters: List<ChapterItem>): List<Int> {
    val present = chapters
        .map { it.number }
        .filter { it == round(it) }
        .map { it.roundToInt() }
        .toSet()
    val highest = present.maxOrNull() ?: return emptyList()
    return (1 until highest).filter { it !in present }
}

// Runs of numbers written short, such as 5-7, 10.
fun missingRanges(missing: List<Int>): String {
    if (missing.isEmpty()) return ""
    val parts = mutableListOf<String>()
    var start = missing.first()
    var end = start
    for (n in missing.drop(1)) {
        if (n == end + 1) {
            end = n
            continue
        }
        parts += if (start == end) "$start" else "$start-$end"
        start = n
        end = n
    }
    parts += if (start == end) "$start" else "$start-$end"
    return parts.joinToString(", ")
}

// Where the breaks in a list are. After holds, for each chapter, the missing numbers between it and the next one in the list, and before holds the ones missing ahead of the first chapter.
data class ChapterGaps(val after: List<List<Int>>, val before: List<Int>)

// In the order of a site or of the dates the numbers can go back and forth, so a missing number is shown once, at the first place it fits.
fun chapterGaps(chapters: List<ChapterItem>): ChapterGaps {
    val missing = missingNumbers(chapters).toSet()
    val placed = HashSet<Int>()
    val after = MutableList(chapters.size) { index ->
        if (missing.isEmpty() ||
            index == chapters.lastIndex
        ) {
            emptyList()
        } else {
            between(chapters[index].number, chapters[index + 1].number, missing).filter { placed.add(it) }
        }
    }
    var before = emptyList<Int>()
    val first = chapters.firstOrNull()?.number
    val last = chapters.lastOrNull()?.number
    if (missing.isNotEmpty() && first != null && last != null) {
        val lowest = chapters.minOf { it.number }
        val tail = missing.filter { it < lowest && it !in placed }.sorted()
        if (tail.isNotEmpty()) {
            // Newest first, the low numbers fall off the bottom of the list.
            if (first > last) after[after.lastIndex] = tail else before = tail
        }
    }
    return ChapterGaps(after, before)
}

// The missing whole numbers strictly between two chapters, in either order.
private fun between(a: Double, b: Double, missing: Set<Int>): List<Int> {
    val low = minOf(a, b)
    val high = maxOf(a, b)
    return (floor(low).toInt() + 1 until high.toInt() + 1).filter { it > low && it < high && it in missing }
}
