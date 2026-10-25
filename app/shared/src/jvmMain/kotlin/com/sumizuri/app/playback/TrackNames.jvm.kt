package com.sumizuri.app.playback

import java.util.Locale

actual fun languageDisplayName(code: String): String? {
    val name = Locale.forLanguageTag(code).getDisplayLanguage(Locale.getDefault())
    return name.takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) }
}
