package com.sumizuri.app.data.sources

import com.sumizuri.app.data.db.AppDatabase
import com.sumizuri.app.data.db.cookie.CookieEntity
import com.sumizuri.app.data.db.value.SourceValueEntity
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.source.host.store.CookieStore
import com.sumizuri.app.source.host.store.StoredCookie
import com.sumizuri.app.source.host.store.ValueArea
import com.sumizuri.app.source.host.store.ValueStore

// What the engine keeps between runs, in the database of the profile.
class DatabaseHostStore(private val db: AppDatabase) :
    ValueStore,
    CookieStore {
    override suspend fun get(scope: String, area: ValueArea, key: String): String? =
        db.values().get(scope, area.code, key)

    override suspend fun set(scope: String, area: ValueArea, key: String, value: String) =
        db.values().put(SourceValueEntity(scope, area.code, key, value))

    override suspend fun delete(scope: String, area: ValueArea, key: String) = db.values().delete(scope, area.code, key)

    override suspend fun clear(scope: String, area: ValueArea) = db.values().clear(scope, area.code)

    override suspend fun all(scope: String, area: ValueArea): Map<String, String> =
        db.values().all(scope, area.code).associate {
            it.key to
                it.value
        }

    override suspend fun load(scope: String): List<StoredCookie> {
        db.cookies().deleteExpired(nowMillis())
        return db.cookies().all(scope).map {
            StoredCookie(it.domain, it.path, it.name, it.value, it.expiresAt, it.secure, it.httpOnly)
        }
    }

    override suspend fun save(scope: String, cookie: StoredCookie) = db.cookies().put(
        CookieEntity(
            scope,
            cookie.domain,
            cookie.path,
            cookie.name,
            cookie.value,
            cookie.expiresAt,
            cookie.secure,
            cookie.httpOnly,
        ),
    )

    override suspend fun delete(scope: String, domain: String, path: String, name: String) =
        db.cookies().delete(scope, domain, path, name)

    override suspend fun clear(scope: String) = db.cookies().clearScope(scope)
}
