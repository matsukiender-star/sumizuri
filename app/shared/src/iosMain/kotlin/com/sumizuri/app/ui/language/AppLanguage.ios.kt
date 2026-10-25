package com.sumizuri.app.ui.language

import platform.Foundation.*

// The system reads the languages an app asks for when it starts, so the choice is kept for the next start. null goes back to the languages of the system.
actual fun applyAppLanguage(code: String?) {
    val defaults = NSUserDefaults.standardUserDefaults
    if (code ==
        null
    ) {
        defaults.removeObjectForKey("AppleLanguages")
    } else {
        defaults.setObject(listOf(code), forKey = "AppleLanguages")
    }
}
