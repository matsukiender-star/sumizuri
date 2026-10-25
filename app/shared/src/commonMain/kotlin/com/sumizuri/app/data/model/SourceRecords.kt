package com.sumizuri.app.data.model

// A repo the user added, with the name its index gave it.
data class Repo(val url: String, val name: String)

// A search with its filters that the reader kept as a tab of one source. A filter holds a text, a list of texts or a flag.
data class SavedCatalog(val name: String, val query: String = "", val filters: Map<String, Any?> = emptyMap())
