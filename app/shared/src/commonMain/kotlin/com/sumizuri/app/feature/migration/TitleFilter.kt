package com.sumizuri.app.feature.migration

import com.sumizuri.app.data.migration.MigrationItem
import com.sumizuri.app.data.model.Kind

// Which of the titles the list shows: by a word in the name, by kind, by the source it comes from, by a shelf, and by whether it is in the work.
internal data class TitleFilter(
    val query: String = "",
    val kind: Kind? = null,
    val from: String? = null,
    val shelf: Long? = null,
    val show: Show = Show.All,
) {
    enum class Show { All, Included, Excluded }

    fun matches(item: MigrationItem): Boolean = (kind == null || item.title.kind == kind) &&
        (from == null || item.fromName == from) &&
        (shelf == null || shelf in item.shelves) &&
        (show == Show.All || (show == Show.Included) == item.included) &&
        (query.isBlank() || item.title.name.contains(query.trim(), ignoreCase = true))
}
