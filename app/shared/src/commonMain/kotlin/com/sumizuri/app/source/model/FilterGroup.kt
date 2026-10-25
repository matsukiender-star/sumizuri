package com.sumizuri.app.source.model

// One setting of a search that a source offers, such as a genre list or a sort order.
data class FilterGroup(
    val key: String,
    val name: String,
    val type: FilterType,
    val options: List<FilterOption>,
    val defaultIndex: Int? = null,
    val defaultSelected: List<String>? = null,
    val defaultValue: Boolean? = null,
) {
    // What the group is set to before the reader chooses, a value, a list of values or a flag.
    fun defaultSelection(): Any? = when (type) {
        FilterType.Select -> options.getOrNull(defaultIndex ?: 0)?.value
        FilterType.MultiSelect -> defaultSelected.orEmpty()
        FilterType.Checkbox -> defaultValue ?: false
    }
}

data class FilterOption(val label: String, val value: String)

enum class FilterType { Select, MultiSelect, Checkbox }
