package com.sumizuri.app.playback

// The name of a language in the language of the app, such as English for en or eng, or null when it is not a language the system knows.
expect fun languageDisplayName(code: String): String?

// Makes the name of a subtitle or an audio track from what the video says about it: its title, and the language it is in.
object TrackNames {
    // The three letter codes some files use for the languages whose bibliographic code differs from the usual one.
    private val Bibliographic = mapOf(
        "fre" to "fr", "ger" to "de", "chi" to "zh", "dut" to "nl", "gre" to "el", "ice" to "is", "cze" to "cs",
        "slo" to "sk", "rum" to "ro", "per" to "fa",
        "may" to "ms", "alb" to "sq", "arm" to "hy", "baq" to "eu", "bur" to "my", "geo" to "ka", "mac" to "mk",
        "mao" to "mi", "tib" to "bo", "wel" to "cy",
    )
    private val Generic = Regex("^(track|subtitle|subtitles|audio|spu|stream|text)\\s*#?\\d+$", RegexOption.IGNORE_CASE)

    // A title that only counts the tracks says nothing, so the language is used. A title that does not name the language gets it added. Nothing known gives a blank name.
    fun label(title: String?, language: String?): String {
        val clean = title?.trim()?.takeIf { it.isNotEmpty() && !Generic.matches(it) }
        val code = language?.trim()?.lowercase()?.takeIf { it.isNotEmpty() && it != "und" && it != "unk" }
        val name = code?.let { languageDisplayName(Bibliographic[it] ?: it) }
        return when {
            clean != null && name != null && !clean.contains(name, ignoreCase = true) -> "$clean ($name)"
            clean != null -> clean
            name != null -> name
            else -> ""
        }
    }
}
