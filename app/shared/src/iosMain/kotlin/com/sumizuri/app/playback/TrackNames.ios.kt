package com.sumizuri.app.playback

import platform.Foundation.*

actual fun languageDisplayName(code: String): String? {
    val name = NSLocale.currentLocale.localizedStringForLanguageCode(code)
    return name?.takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) }
}
