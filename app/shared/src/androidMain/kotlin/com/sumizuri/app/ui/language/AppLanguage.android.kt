package com.sumizuri.app.ui.language

import java.util.Locale

// The language the system had when the app started, which is what the choice System goes back to.
private val systemLocale: Locale = Locale.getDefault()

actual fun applyAppLanguage(code: String?) {
    Locale.setDefault(if (code == null) systemLocale else Locale.forLanguageTag(code))
}
