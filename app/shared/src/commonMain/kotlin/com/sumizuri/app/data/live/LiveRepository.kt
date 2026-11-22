package com.sumizuri.app.data.live

import com.sumizuri.app.data.settings.SettingsRepository
import com.sumizuri.app.data.text.nowMillis
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.random.Random

// All keys live under memo.live. so playlists (which may carry credentials in URLs) never export or sync.
private const val KeyPlaylists = "memo.live.playlists"
private const val KeyFavorites = "memo.live.favorites"
private const val KeyRecents = "memo.live.recents"
private const val MaxRecents = 30
private const val FetchChunkSize = 32 * 1024

// Manages IPTV playlists, channels, favorites and recents per profile without adding Room tables.
class LiveRepository(
    private val settings: SettingsRepository,
    private val databasePath: String,
    @Suppress("unused") private val scope: CoroutineScope,
    private val files: FileSystem = FileSystem.SYSTEM,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheLock = Mutex()
    private val channelCache = HashMap<String, List<LiveChannel>>()
    private val http = lazy {
        HttpClient {
            install(HttpTimeout) {
                connectTimeoutMillis = 12_000
                requestTimeoutMillis = 30_000
                socketTimeoutMillis = 20_000
            }
        }
    }

    val playlists: Flow<List<LivePlaylist>> = settings.text(KeyPlaylists).map { raw ->
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                json.decodeFromString(ListSerializer(LivePlaylist.serializer()), raw)
            }.getOrDefault(emptyList())
        }
    }

    val favorites: Flow<Set<String>> = settings.text(KeyFavorites).map { raw ->
        if (raw.isNullOrBlank()) {
            emptySet()
        } else {
            runCatching {
                json.decodeFromString(SetSerializer(String.serializer()), raw)
            }.getOrDefault(emptySet())
        }
    }

    val recents: Flow<List<String>> = settings.text(KeyRecents).map { raw ->
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                json.decodeFromString(ListSerializer(String.serializer()), raw)
            }.getOrDefault(emptyList())
        }
    }

    suspend fun addFromUrl(name: String, url: String): LiveLoadResult {
        val cleanUrl = url.trim()
        if (!isHttpUrl(cleanUrl)) return LiveLoadResult.InvalidUrl
        val id = newId()
        val fetched = downloadPlaylist(cleanUrl) ?: return LiveLoadResult.ReadFailed
        if (fetched === TooLargeSentinel) return LiveLoadResult.TooLarge
        return saveParsed(id, name.trim().ifBlank { redactLiveUrl(cleanUrl) }, LiveSourceKind.Url, cleanUrl, fetched)
    }

    suspend fun addFromFile(
        name: String,
        path: Path,
        displayName: String? = null,
        temporary: Boolean = false,
    ): LiveLoadResult {
        val isTemp = temporary || isTemporaryLiveFile(path.name)
        val text = try {
            readLocalPlaylist(path)
        } finally {
            if (temporary) withContext(Dispatchers.IO) { runCatching { files.delete(path) } }
        } ?: return LiveLoadResult.ReadFailed
        if (text === TooLargeSentinel) return LiveLoadResult.TooLarge
        val id = newId()
        val label = name.trim().ifBlank {
            cleanFileLabel(displayName) ?: cleanFileLabel(path.name) ?: "IPTV"
        }
        // Never persist a temporary picker cache path, so refreshes ask the user for the file again.
        val storedSource = if (isTemp) "" else path.toString()
        return saveParsed(id, label, LiveSourceKind.File, storedSource, text)
    }

    suspend fun refreshFromFile(playlistId: String, path: Path, temporary: Boolean = false): LiveLoadResult {
        val list = currentPlaylists().firstOrNull { it.id == playlistId } ?: return LiveLoadResult.ReadFailed
        val isTemp = temporary || isTemporaryLiveFile(path.name)
        val text = try {
            readLocalPlaylist(path)
        } finally {
            if (temporary) withContext(Dispatchers.IO) { runCatching { files.delete(path) } }
        } ?: return LiveLoadResult.ReadFailed
        if (text === TooLargeSentinel) return LiveLoadResult.TooLarge
        val storedSource = if (isTemp) "" else path.toString()
        return saveParsed(list.id, list.name, LiveSourceKind.File, storedSource, text)
    }

    suspend fun refresh(playlistId: String): LiveLoadResult {
        val list = currentPlaylists().firstOrNull { it.id == playlistId } ?: return LiveLoadResult.ReadFailed
        val text = when (list.kind) {
            LiveSourceKind.Url -> {
                if (!isHttpUrl(list.source)) return LiveLoadResult.InvalidUrl
                downloadPlaylist(list.source)
            }

            LiveSourceKind.File -> {
                if (isTemporaryLiveFile(list.source)) return LiveLoadResult.PickFileRequired
                readLocalPlaylist(list.source.toPath())
            }
        } ?: return LiveLoadResult.ReadFailed
        if (text === TooLargeSentinel) return LiveLoadResult.TooLarge
        return saveParsed(list.id, list.name, list.kind, list.source, text)
    }

    suspend fun rename(playlistId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val updated = currentPlaylists().map { if (it.id == playlistId) it.copy(name = trimmed) else it }
        savePlaylists(updated)
    }

    suspend fun remove(playlistId: String) {
        val updated = currentPlaylists().filterNot { it.id == playlistId }
        savePlaylists(updated)
        cacheLock.withLock { channelCache.remove(playlistId) }
        val file = playlistFile(playlistId)
        withContext(Dispatchers.IO) { runCatching { files.delete(file) } }
    }

    suspend fun channelsOf(playlistId: String): List<LiveChannel> {
        cacheLock.withLock { channelCache[playlistId]?.let { return it } }
        val file = playlistFile(playlistId)
        val loaded = withContext(Dispatchers.IO) {
            if (!files.exists(file)) return@withContext emptyList()
            runCatching {
                val raw = files.read(file) { readUtf8() }
                json.decodeFromString(ListSerializer(LiveChannel.serializer()), raw)
            }.getOrDefault(emptyList())
        }
        cacheLock.withLock { channelCache[playlistId] = loaded }
        return loaded
    }

    suspend fun allChannels(lists: List<LivePlaylist>): List<LiveChannel> {
        if (lists.isEmpty()) return emptyList()
        val combined = ArrayList<LiveChannel>()
        for (list in lists) combined.addAll(channelsOf(list.id))
        return combined
    }

    suspend fun toggleFavorite(channelId: String) {
        val current = currentFavorites().toMutableSet()
        if (!current.add(channelId)) current.remove(channelId)
        settings.setText(KeyFavorites, json.encodeToString(SetSerializer(String.serializer()), current))
    }

    suspend fun recordRecent(channelId: String) {
        val updated = (listOf(channelId) + currentRecents().filterNot { it == channelId }).take(MaxRecents)
        settings.setText(KeyRecents, json.encodeToString(ListSerializer(String.serializer()), updated))
    }

    private suspend fun saveParsed(
        id: String,
        name: String,
        kind: LiveSourceKind,
        source: String,
        rawText: String,
    ): LiveLoadResult {
        val parsed = withContext(Dispatchers.Default) { M3uParser.parse(id, rawText) }
        if (parsed.channels.isEmpty()) return LiveLoadResult.Empty
        val file = playlistFile(id)
        val serialized = withContext(Dispatchers.Default) {
            json.encodeToString(ListSerializer(LiveChannel.serializer()), parsed.channels)
        }
        try {
            withContext(Dispatchers.IO) {
                file.parent?.let { files.createDirectories(it) }
                val temp = file.parent!! / "${file.name}.tmp"
                try {
                    files.write(temp) { writeUtf8(serialized) }
                    files.atomicMove(temp, file)
                } catch (error: Throwable) {
                    runCatching { files.delete(temp) }
                    throw error
                }
            }
            val entry = LivePlaylist(id, name, kind, source, parsed.channels.size, nowMillis())
            val current = currentPlaylists()
            val next =
                if (current.any { it.id == id }) current.map { if (it.id == id) entry else it } else current + entry
            savePlaylists(next)
            cacheLock.withLock { channelCache[id] = parsed.channels }
            return LiveLoadResult.Ok(entry, parsed.truncated)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            return LiveLoadResult.SaveFailed
        }
    }

    private fun cleanFileLabel(rawName: String?): String? {
        val trimmed = rawName?.trim().orEmpty()
        if (trimmed.isEmpty() || isTemporaryLiveFile(trimmed)) return null
        return trimmed.substringBeforeLast('.').ifBlank { trimmed }
    }

    // Reads at most MaxLivePlaylistBytes from an HTTP(S) response without logging the URL.
    private suspend fun downloadPlaylist(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            http.value.prepareGet(url).execute { response ->
                if (!response.status.isSuccess()) return@execute null
                val declared = response.headers["Content-Length"]?.toLongOrNull()
                if (declared != null && declared > MaxLivePlaylistBytes) return@execute TooLargeSentinel
                val channel = response.bodyAsChannel()
                val buffer = ByteArray(FetchChunkSize)
                val out = ByteArrayBuilder()
                while (true) {
                    val read = channel.readAvailable(buffer, 0, buffer.size)
                    if (read <= 0) break
                    if (out.size + read > MaxLivePlaylistBytes) return@execute TooLargeSentinel
                    out.append(buffer, read)
                }
                out.toUtf8String()
            }
        }.getOrNull()
    }

    private suspend fun readLocalPlaylist(path: Path): String? = withContext(Dispatchers.IO) {
        runCatching {
            val meta = files.metadataOrNull(path) ?: return@runCatching null
            val size = meta.size ?: 0L
            if (size > MaxLivePlaylistBytes) return@runCatching TooLargeSentinel
            files.read(path) { readUtf8() }
        }.getOrNull()
    }

    // Named after the database, whose name never changes, so deleting a profile for good finds its lists without opening it.
    private fun playlistFile(playlistId: String): Path {
        val database = databasePath.takeIf { it.isNotBlank() }?.toPath()
        val base = database?.parent ?: FileSystem.SYSTEM_TEMPORARY_DIRECTORY
        val profile = database?.name?.substringBeforeLast('.') ?: "sumizuri"
        return base / "live" / profile / "$playlistId.json"
    }

    private suspend fun currentPlaylists(): List<LivePlaylist> {
        val raw = settings.textNow(KeyPlaylists)
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(
                ListSerializer(LivePlaylist.serializer()),
                raw,
            )
        }.getOrDefault(emptyList())
    }

    private suspend fun currentFavorites(): Set<String> {
        val raw = settings.textNow(KeyFavorites)
        if (raw.isNullOrBlank()) return emptySet()
        return runCatching { json.decodeFromString(SetSerializer(String.serializer()), raw) }.getOrDefault(emptySet())
    }

    private suspend fun currentRecents(): List<String> {
        val raw = settings.textNow(KeyRecents)
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(String.serializer()), raw) }.getOrDefault(emptyList())
    }

    private suspend fun savePlaylists(lists: List<LivePlaylist>) {
        settings.setText(KeyPlaylists, json.encodeToString(ListSerializer(LivePlaylist.serializer()), lists))
    }

    private fun newId(): String = "${nowMillis().toString(16)}_${Random.nextInt(0x100000).toString(16)}"

    private companion object {
        val TooLargeSentinel = String(charArrayOf('\u0000'))
    }
}

// Growable byte buffer for bounded HTTP streaming in commonMain without JVM-only classes.
private class ByteArrayBuilder {
    private var data = ByteArray(16 * 1024)
    var size: Int = 0
        private set

    fun append(chunk: ByteArray, count: Int) {
        if (size + count > data.size) {
            var nextSize = data.size * 2
            while (nextSize < size + count) nextSize *= 2
            data = data.copyOf(nextSize)
        }
        chunk.copyInto(data, destinationOffset = size, startIndex = 0, endIndex = count)
        size += count
    }

    fun toUtf8String(): String = data.decodeToString(0, size)
}
