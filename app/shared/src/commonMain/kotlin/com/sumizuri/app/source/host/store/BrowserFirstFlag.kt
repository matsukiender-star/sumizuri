package com.sumizuri.app.source.host.store

// Remembers that a source's site put a bot check in the way, so its requests use the browser first.
interface BrowserFirstFlag {
    suspend fun isSet(): Boolean
    suspend fun set()
}

class InMemoryBrowserFirstFlag : BrowserFirstFlag {
    private var value = false

    override suspend fun isSet(): Boolean = value

    override suspend fun set() {
        value = true
    }
}

private const val Key = "browserFirst"

// The browser first mark of one source, kept in the value store so it survives a restart.
class StoredBrowserFirstFlag(private val scope: String, private val values: ValueStore) : BrowserFirstFlag {
    override suspend fun isSet(): Boolean = values.get(scope, ValueArea.Flag, Key) != null

    override suspend fun set() = values.set(scope, ValueArea.Flag, Key, "true")
}
