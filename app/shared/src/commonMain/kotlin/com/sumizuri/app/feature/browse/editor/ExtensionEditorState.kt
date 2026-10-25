package com.sumizuri.app.feature.browse.editor

import com.sumizuri.app.data.model.Kind

// The extension being written: its code and what the app shows about it, whether it changed since it was opened or saved, and where the tests are.
data class ExtensionEditorState(
    val loaded: Boolean = false,
    // The source that was opened, or null for a new one.
    val sourceId: String? = null,
    // The opened source came from a repo, so saving keeps a copy of its own and leaves the original as the repo gives it.
    val fromRepo: Boolean = false,
    val code: String = "",
    val name: String = "",
    val lang: String = "",
    val kind: Kind = Kind.Manga,
    val baseUrl: String = "",
    val iconUrl: String = "",
    val dirty: Boolean = false,
    val saving: Boolean = false,
    // The id it was saved under the last time, and what went wrong when it could not be saved.
    val savedAs: String? = null,
    val problem: String? = null,
    val test: ExtensionTest = ExtensionTest(),
) {
    // A new extension can be saved as the template is, and one that was opened once something changed.
    val canSave: Boolean get() = !saving && name.isNotBlank() && (dirty || (sourceId == null && savedAs == null))
}
