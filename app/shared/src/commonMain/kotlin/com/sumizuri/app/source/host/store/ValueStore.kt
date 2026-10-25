package com.sumizuri.app.source.host.store

// Where the values of a source are kept between runs. A scope is the id of the source, or a name for the app's own use.
interface ValueStore {
    suspend fun get(scope: String, area: ValueArea, key: String): String?
    suspend fun set(scope: String, area: ValueArea, key: String, value: String)
    suspend fun delete(scope: String, area: ValueArea, key: String)
    suspend fun clear(scope: String, area: ValueArea)
    suspend fun all(scope: String, area: ValueArea): Map<String, String>
}

// The kinds of saved value, kept apart in one table. The code is what the database stores.
enum class ValueArea(val code: Int) {
    // What an extension saves for itself.
    Storage(0),

    // The settings the user chose for a source.
    Preference(1),

    // Facts the app learned about a site, such as that it needs the hidden browser.
    Flag(2),
}

// A store that forgets everything when the app closes, for a host that has no database behind it.
class InMemoryValueStore : ValueStore {
    private val values = LinkedHashMap<Triple<String, ValueArea, String>, String>()

    override suspend fun get(scope: String, area: ValueArea, key: String): String? = values[Triple(scope, area, key)]

    override suspend fun set(scope: String, area: ValueArea, key: String, value: String) {
        values[Triple(scope, area, key)] = value
    }

    override suspend fun delete(scope: String, area: ValueArea, key: String) {
        values.remove(Triple(scope, area, key))
    }

    override suspend fun clear(scope: String, area: ValueArea) {
        values.keys.removeAll { it.first == scope && it.second == area }
    }

    override suspend fun all(scope: String, area: ValueArea): Map<String, String> = values
        .filterKeys {
            it.first ==
                scope &&
                it.second == area
        }.mapKeys { it.key.third }
}
