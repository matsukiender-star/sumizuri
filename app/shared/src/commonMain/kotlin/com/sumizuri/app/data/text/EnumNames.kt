package com.sumizuri.app.data.text

import kotlin.enums.enumEntries

// The choice of an enum with this name, as choices are kept in settings and files, or null when none has it.
inline fun <reified E : Enum<E>> enumNamed(name: String?): E? = enumEntries<E>().firstOrNull { it.name == name }
