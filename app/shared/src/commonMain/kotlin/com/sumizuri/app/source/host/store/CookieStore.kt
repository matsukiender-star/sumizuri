package com.sumizuri.app.source.host.store

// Where the cookies of a source are kept between runs.
interface CookieStore {
    // The cookies that have not expired, and the ones that have are dropped.
    suspend fun load(scope: String): List<StoredCookie>
    suspend fun save(scope: String, cookie: StoredCookie)
    suspend fun delete(scope: String, domain: String, path: String, name: String)
    suspend fun clear(scope: String)
}

// A cookie as it is kept between runs. The expiry is epoch milliseconds, or null for a cookie that lasts until it is cleared.
data class StoredCookie(
    val domain: String,
    val path: String,
    val name: String,
    val value: String,
    val expiresAt: Long?,
    val secure: Boolean,
    val httpOnly: Boolean,
)
