package com.sumizuri.app.ui.language

// A language the app can be set to, by its code such as en or pt-BR and its own name. The one with no code follows the system.
data class AppLanguage(val code: String?, val nativeName: String?) {
    companion object {
        val System = AppLanguage(null, null)
        private val English = AppLanguage("en", "English")

        // The system, English, and then every translation the build found among the folders of the words.
        val entries: List<AppLanguage> =
            listOf(System, English) + TranslatedLanguages.map { (code, name) -> AppLanguage(code, name) }

        // The choice that was saved: a code, or a name the older list saved such as English. A language that is no longer there falls back to the system.
        fun fromSaved(saved: String?): AppLanguage = when (saved) {
            null, "", "System" -> System
            "English" -> English
            else -> entries.firstOrNull { it.code.equals(saved, ignoreCase = true) } ?: System
        }
    }
}

// Makes the app read its words in the language, or in the one of the system for null. The words are looked up again by whatever draws them next.
expect fun applyAppLanguage(code: String?)
