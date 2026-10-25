package com.sumizuri.app.ui.kit.input

import androidx.compose.ui.focus.FocusRequester

// The search fields that are on screen, so a shortcut can put the cursor in the one that was shown last, which is the one of the page on top.
object SearchFocus {
    private val fields = mutableListOf<FocusRequester>()

    internal fun add(field: FocusRequester) {
        fields += field
    }

    internal fun remove(field: FocusRequester) {
        fields -= field
    }

    // Says whether there was a field to take the focus.
    fun request(): Boolean {
        val field = fields.lastOrNull() ?: return false
        return runCatching { field.requestFocus() }.isSuccess
    }
}
