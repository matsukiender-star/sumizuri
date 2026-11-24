package com.sumizuri.app.data.appupdate

import com.sumizuri.app.BuildInfo
import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.data.settings.SettingsRepository
import com.sumizuri.app.data.text.nowMillis
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.buffer
import okio.use

private const val Tag = "appupdate"

// How long Install stays waiting after the setup opened, long enough for its window to show.
private const val OpenedHoldMs = 15_000L
// How long a download may go without a byte before it fails.
private const val StallMs = 30_000L
private const val Prefix = "appupdate."
private const val AutoCheckKey = "appupdate.autoCheck"
private const val PrereleaseKey = "appupdate.prerelease"
private const val SkipKey = "appupdate.skip"
private const val LastCheckKey = "appupdate.lastCheck"
private const val LastRunKey = "appupdate.lastRun"

// How long after a check the app waits before it checks again on its own.
private const val CheckEveryMs = 6 * 60 * 60 * 1000L

// The choices of the updater.
data class UpdatePrefs(
    // Look for a new version when the app opens.
    val autoCheck: Boolean = true,
    // Count test versions, such as 1.2.0-beta, as new versions too.
    val prerelease: Boolean = false,
    // A version the person chose not to be told about.
    val skipped: String? = null,
    val lastCheck: Long = 0,
)

// What the updater knows now.
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState

    // No repo is set, so there is nothing to check.
    data object NoSource : UpdateState
    data class UpToDate(val latest: AppRelease?) : UpdateState

    // The versions newer than this one, the newest first, and the files of the newest this system can use.
    data class Available(val newer: List<AppRelease>, val files: List<UpdateFile>) : UpdateState {
        val latest: AppRelease get() = newer.first()

        // The file this system likes best, or the first one it can use.
        val asset: ReleaseAsset? get() = (files.firstOrNull { it.recommended } ?: files.firstOrNull())?.asset
    }

    // Not found means the repo is wrong or private, and limited means the site asked to wait a while.
    data class Failed(val message: String, val notFound: Boolean = false, val limited: Boolean = false) : UpdateState
}

// Why a file was not kept.
enum class DownloadProblem { Failed, NoHash, BadHash }

// A file being saved: how much of it there is, and the file once it is all there, or why it was not kept.
data class UpdateDownload(
    val asset: ReleaseAsset,
    val done: Long = 0,
    val total: Long? = null,
    val file: String? = null,
    val problem: DownloadProblem? = null,
) {
    val finished: Boolean get() = file != null
}

// Looks for new versions of the app in the releases of its repo on GitHub or GitLab, saves the file of one, and hands it to the system to install. A repo with no releases is read from its tags.
class AppUpdater(private val settings: SettingsRepository, private val scope: CoroutineScope) {
    private val http = HttpClient { install(HttpTimeout) { requestTimeoutMillis = 30_000 } }
    private val json = Json { ignoreUnknownKeys = true }

    val prefs: Flow<UpdatePrefs> = settings.prefixed(Prefix).map { saved ->
        UpdatePrefs(
            autoCheck = saved[AutoCheckKey] != "false",
            prerelease = saved[PrereleaseKey] == "true",
            skipped = saved[SkipKey]?.takeIf { it.isNotBlank() },
            lastCheck = saved[LastCheckKey]?.toLongOrNull() ?: 0L,
        )
    }

    private val mutableState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = mutableState

    private val mutableDownload = MutableStateFlow<UpdateDownload?>(null)
    val download: StateFlow<UpdateDownload?> = mutableDownload
    private var downloading: Job? = null

    private val mutableOpening = MutableStateFlow(false)

    // Whether the setup is being opened, or opened a moment ago, while Install does nothing more, so one press opens one setup.
    val opening: StateFlow<Boolean> = mutableOpening

    // Every release the last check read, to show the notes of this version too.
    private var known: List<AppRelease> = emptyList()

    // The repo to check, which is the one the app was built with. What an older version saved as its own repo is ignored.
    fun sourceOf(): UpdateSource? = UpdateSource.of(BuildInfo.UpdateRepo)

    suspend fun check(): UpdateState {
        val now = prefs.first()
        val source = sourceOf() ?: return UpdateState.NoSource.also { mutableState.value = it }
        mutableState.value = UpdateState.Checking
        val result = try {
            val releases = releasesOf(source)
            known = releases
            settings.setText(LastCheckKey, nowMillis().toString())
            val newer = releases.filter { (now.prerelease || !it.prerelease) && compareVersions(it.version, BuildInfo.Version) > 0 }.sortedWith { a, b -> compareVersions(b.version, a.version) }
            if (newer.isEmpty()) {
                UpdateState.UpToDate(releases.filter { now.prerelease || !it.prerelease }.maxWithOrNull { a, b -> compareVersions(a.version, b.version) })
            } else {
                // Linux asks dpkg, so the files are looked at away from the screen.
                UpdateState.Available(newer, withContext(Dispatchers.IO) { UpdateInstaller.files(newer.first().assets) })
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppLog.warn(Tag, "could not check for updates, ${error.message}")
            UpdateState.Failed(error.message ?: error::class.simpleName.orEmpty(), notFound = error is UpdateProblem.NotFound, limited = error is UpdateProblem.Limited)
        }
        mutableState.value = result
        return result
    }

    // The check when the app opens: only when the person wants it, a repo is set and the last check was a while ago. Gives the newest version when it is one the person was not told to skip.
    suspend fun checkIfDue(): AppRelease? {
        val now = prefs.first()
        if (!now.autoCheck || sourceOf() == null || nowMillis() - now.lastCheck < CheckEveryMs) return null
        val found = check() as? UpdateState.Available ?: return null
        return found.latest.takeIf { it.version != now.skipped }
    }

    // The notes of this version, once, the first time the app opens after it was updated. Nothing on the first run of all.
    suspend fun whatsNew(): AppRelease? {
        val last = settings.textNow(LastRunKey)
        if (last == BuildInfo.Version) return null
        settings.setText(LastRunKey, BuildInfo.Version)
        if (last == null) return null
        val source = sourceOf() ?: return null
        val releases = known.ifEmpty { runCatching { releasesOf(source) }.getOrDefault(emptyList()) }
        return releases.firstOrNull { compareVersions(it.version, BuildInfo.Version) == 0 }?.takeIf { it.notes.isNotBlank() }
    }

    // The release of this version, for its notes, from the last check.
    fun current(): AppRelease? = known.firstOrNull { compareVersions(it.version, BuildInfo.Version) == 0 }

    // Takes the choices of the updater back. The version the app last opened as stays, so the notes of a new version still show once.
    suspend fun resetChoices() = settings.reset(Prefix, except = LastRunKey)

    suspend fun skip(version: String?) = settings.setText(SkipKey, version.orEmpty())

    suspend fun setAutoCheck(on: Boolean) = settings.setFlag(AutoCheckKey, on)

    suspend fun setPrerelease(on: Boolean) = settings.setFlag(PrereleaseKey, on)

    // Saves the file to a folder of its own, telling how far it got. A new download takes the place of one under way.
    fun startDownload(asset: ReleaseAsset) {
        downloading?.cancel()
        mutableDownload.value = UpdateDownload(asset, total = asset.size)
        downloading = scope.launch(Dispatchers.IO) {
            try {
                val folder = UpdateInstaller.folder()
                FileSystem.SYSTEM.deleteRecursively(folder)
                FileSystem.SYSTEM.createDirectories(folder)
                val file = folder / asset.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
                // The sha256 is taken as the file is written, so a file the release does not give, or gives another hash for, is never kept.
                val hash = CryptographyProvider.Default.get(SHA256).hasher().createHashFunction()
                val request = http.prepareGet(asset.url) {
                    // The 30 seconds are for a check. A file of 200 MB takes longer, so here only silence ends it.
                    timeout {
                        requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                        socketTimeoutMillis = StallMs
                    }
                }
                request.execute { response ->
                    if (!response.status.isSuccess()) error("The download answered ${response.status.value}")
                    val total = response.contentLength() ?: asset.size
                    val channel = response.bodyAsChannel()
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    var shown = 0L
                    FileSystem.SYSTEM.sink(file).buffer().use { sink ->
                        while (true) {
                            val read = channel.readAvailable(buffer, 0, buffer.size)
                            if (read == -1) break
                            if (read == 0) continue
                            sink.write(buffer, 0, read)
                            hash.update(buffer, 0, read)
                            done += read
                            // Progress reaches the screen every quarter of a megabyte, not for every piece.
                            if (done - shown >= 256 * 1024) {
                                shown = done
                                mutableDownload.value = UpdateDownload(asset, done, total)
                            }
                        }
                    }
                    val found = hash.hashToByteArray().hexText()
                    hash.close()
                    val expected = asset.sha256Hex
                    if (expected == null || found != expected) {
                        FileSystem.SYSTEM.delete(file)
                        AppLog.warn(Tag, "deleted ${asset.name}, its sha256 is $found and the release gives ${expected ?: "none"}")
                        mutableDownload.value = UpdateDownload(
                            asset,
                            done,
                            total,
                            problem = if (expected == null) DownloadProblem.NoHash else DownloadProblem.BadHash,
                        )
                        return@execute
                    }
                    mutableDownload.value = UpdateDownload(asset, done, total, file = file.toString())
                    AppLog.info(Tag, "downloaded ${asset.name}, ${done / 1024} KB")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                AppLog.warn(Tag, "could not download ${asset.name}, ${error.message}")
                mutableDownload.value = UpdateDownload(asset, problem = DownloadProblem.Failed)
            }
        }
    }

    fun cancelDownload() {
        downloading?.cancel()
        mutableDownload.value = null
    }

    // Hands the saved file to the system to install. A press while the setup is opening, or soon after it opened, does nothing, as opening takes a few seconds on Windows.
    suspend fun install(): InstallResult {
        val file = mutableDownload.value?.file ?: return InstallResult.Failed
        if (!mutableOpening.compareAndSet(expect = false, update = true)) return InstallResult.Started
        var started = false
        try {
            return withContext(Dispatchers.Main) { UpdateInstaller.install(file) }.also { started = it == InstallResult.Started }
        } finally {
            if (started) {
                scope.launch {
                    delay(OpenedHoldMs)
                    mutableOpening.value = false
                }
            } else {
                mutableOpening.value = false
            }
        }
    }

    // The releases of the repo, or its tags when it has no releases.
    private suspend fun releasesOf(source: UpdateSource): List<AppRelease> {
        val (releasesApi, tagsApi) = when (source) {
            is UpdateSource.GitHub -> source.releasesApi to source.tagsApi
            is UpdateSource.GitLab -> source.releasesApi to source.tagsApi
        }
        val releases = when (source) {
            is UpdateSource.GitHub -> gitHubReleases(json.parseToJsonElement(read(releasesApi)))
            is UpdateSource.GitLab -> gitLabReleases(json.parseToJsonElement(read(releasesApi)), source)
        }
        return releases.ifEmpty { tagReleases(json.parseToJsonElement(read(tagsApi)), source) }
    }

    private suspend fun read(address: String): String = withContext(Dispatchers.IO) {
        val response = http.get(address) {
            header("Accept", "application/json")
            header("User-Agent", "Sumizuri/${BuildInfo.Version}")
        }
        when {
            response.status == HttpStatusCode.NotFound -> throw UpdateProblem.NotFound
            response.status == HttpStatusCode.Forbidden || response.status == HttpStatusCode.TooManyRequests -> throw UpdateProblem.Limited
            !response.status.isSuccess() -> error("The repo answered ${response.status.value}")
        }
        response.bodyAsText()
    }
}

// Why a check failed in a way the page explains in words of its own.
sealed class UpdateProblem(message: String) : Exception(message) {
    data object NotFound : UpdateProblem("not found")
    data object Limited : UpdateProblem("limited")
}

// What happened when the file was handed to the system.
enum class InstallResult {
    Started,
    NeedsPermission,
    Failed,

    // The app itself was replaced, so the new version shows once it is opened again.
    Restart,
}

// The bytes of a hash as the letters of an address, which is how a release writes its sha256.
private fun ByteArray.hexText(): String = joinToString("") { byte ->
    val part = (byte.toInt() and 0xFF).toString(16)
    if (part.length == 1) "0$part" else part
}
