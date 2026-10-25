package com.sumizuri.app.data.text

import kotlin.time.Clock

data class Ago(val unit: AgoUnit, val count: Int)

enum class AgoUnit { Now, Minutes, Hours, Yesterday, Days }

private const val Minute = 60_000L

private const val Hour = 60 * Minute

private const val Day = 24 * Hour

// How long ago something was, as a unit and a count, so the screen can put it into words.
fun agoSince(now: Long, at: Long): Ago {
    val gap = (now - at).coerceAtLeast(0)
    return when {
        gap < Minute -> Ago(AgoUnit.Now, 0)
        gap < Hour -> Ago(AgoUnit.Minutes, (gap / Minute).toInt())
        gap < Day -> Ago(AgoUnit.Hours, (gap / Hour).toInt())
        gap < 2 * Day -> Ago(AgoUnit.Yesterday, 1)
        else -> Ago(AgoUnit.Days, (gap / Day).toInt())
    }
}

fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()
