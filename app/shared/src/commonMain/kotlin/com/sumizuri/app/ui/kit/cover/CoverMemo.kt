package com.sumizuri.app.ui.kit.cover

import com.sumizuri.app.ui.kit.cover.focus.keepFocus

// Answers about covers that took work to find, such as where the face is and the main color, kept between runs where the app gives a place for them. Without one they last until the app closes.
object CoverMemo {
    var read: suspend (String) -> String? = { null }
    var write: suspend (String, String) -> Unit = { _, _ -> }

    // What is written for a cover that was looked at and gave no answer, so it is not looked at again.
    const val Nothing = "-"

    // Takes in everything kept from earlier runs at once, when the app opens, so the pages that use it never wait for it.
    fun preload(all: Map<String, String>) {
        for ((key, saved) in all) {
            when {
                key.startsWith("palette:") -> keepPalette(key.removePrefix("palette:"), saved)
                key.startsWith("color:") -> keepColor(key.removePrefix("color:"), saved)
                key.startsWith("focus:") -> keepFocus(key.removePrefix("focus:"), saved)
            }
        }
    }

    suspend fun get(key: String): String? = runCatching { read(key) }.getOrNull()

    suspend fun put(key: String, value: String) {
        runCatching { write(key, value) }
    }
}
