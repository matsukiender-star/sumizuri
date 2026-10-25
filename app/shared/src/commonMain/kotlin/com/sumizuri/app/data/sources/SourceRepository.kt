package com.sumizuri.app.data.sources

import com.sumizuri.app.data.db.AppDatabase
import com.sumizuri.app.data.db.source.SourceEntity
import com.sumizuri.app.data.local.LocalSources
import com.sumizuri.app.data.model.Kind
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.source.repo.EngineKind
import com.sumizuri.app.source.repo.InstalledSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// The installed sources, and the settings kept for them.
class SourceRepository(private val db: AppDatabase) {
    // The installed sources, and after them the sources of files on the device, which are built in.
    val installed: Flow<List<InstalledSource>> = db.sources().observeAll().map { rows ->
        rows.map(::toInstalled) +
            LocalSources.all
    }

    // Saving a source again, as an update does, keeps whether it is switched on and when it was last used.
    suspend fun save(source: InstalledSource) {
        val before = db.sources().get(source.id)
        db.sources().put(
            SourceEntity(
                id = source.id,
                repoUrl = source.repoUrl,
                name = source.name,
                lang = source.lang,
                kind = source.kind.code,
                engineKind = source.engineKind.key,
                version = source.version,
                iconUrl = source.iconUrl,
                baseUrl = source.baseUrl,
                code = source.code,
                extra = source.extra,
                enabled = before?.enabled ?: true,
                lastUsedAt = before?.lastUsedAt ?: 0,
            ),
        )
    }

    // The installed source with this id, or null when there is none.
    suspend fun find(id: String?): InstalledSource? = installed.first().firstOrNull { it.id == id }

    suspend fun setEnabled(id: String, enabled: Boolean) = db.sources().setEnabled(id, enabled)

    suspend fun markUsed(id: String) = db.sources().touch(id, nowMillis())

    // Removing a source also removes what it saved, its settings and its cookies.
    suspend fun remove(id: String) {
        db.sources().delete(id)
        db.values().clearScope(id)
        db.cookies().clearScope(id)
    }

    private fun toInstalled(row: SourceEntity) = InstalledSource(
        id = row.id,
        repoUrl = row.repoUrl,
        name = row.name,
        lang = row.lang,
        kind = Kind.fromCode(row.kind),
        engineKind = EngineKind.fromKey(row.engineKind),
        version = row.version,
        iconUrl = row.iconUrl,
        baseUrl = row.baseUrl,
        code = row.code,
        extra = row.extra,
        enabled = row.enabled,
        lastUsedAt = row.lastUsedAt,
    )
}
