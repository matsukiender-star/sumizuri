package com.sumizuri.app.ui.kit.image

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.network.HttpException
import coil3.request.Options
import com.sumizuri.app.data.text.nowMillis
import com.sumizuri.app.source.host.net.ImageException
import com.sumizuri.app.source.host.net.ImageHost
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Buffer

private val BotCheckCodes = setOf(403, 503)

// A picture that failed is not asked for again this long, so a broken cover is not fetched again each time it is drawn.
private const val FailedForMs = 60 * 1000L

// The pictures that failed lately and when, shared by every fetcher.
private object FailedPictures {
    private val lock = Mutex()
    private val failed = HashMap<String, Long>()

    suspend fun recently(url: String): Boolean = lock.withLock {
        val at = failed[url] ?: return@withLock false
        if (nowMillis() - at < FailedForMs) return@withLock true
        failed.remove(url)
        false
    }

    suspend fun forget(url: String) = lock.withLock { failed.remove(url) }

    suspend fun mark(url: String) = lock.withLock {
        if (failed.size > 500) failed.clear()
        failed[url] = nowMillis()
    }
}

// Lets a picture be asked for again at once, for a page the person taps to load again.
suspend fun forgetFailedPicture(url: String) = FailedPictures.forget(url)

// Loads a picture the usual way, and through the hidden browser when its site answers with a bot check. What the browser brings is kept in the disk cache like any other picture.
class GuardedImageFetcher(
    private val url: String,
    private val options: Options,
    private val images: ImageHost,
    private val plain: Fetcher?,
    private val loader: ImageLoader,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        if (FailedPictures.recently(url)) throw ImageException("failed a moment ago, ${url.substringBefore('?')}")
        try {
            if (plain != null && !images.isGuarded(url)) {
                try {
                    return plain.fetch()
                } catch (error: HttpException) {
                    val challenge = error.response.headers["cf-mitigated"]
                        .orEmpty()
                        .contains("challenge", ignoreCase = true)
                    if (error.response.code !in BotCheckCodes || !challenge) throw error
                    images.markGuarded(url)
                }
            }
            return fromDisk() ?: throughBrowser()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            FailedPictures.mark(url)
            throw error
        }
    }

    private fun fromDisk(): FetchResult? {
        if (!options.diskCachePolicy.readEnabled) return null
        val snapshot = loader.diskCache?.openSnapshot(diskKey()) ?: return null
        return SourceFetchResult(
            ImageSource(snapshot.data, options.fileSystem, diskKey(), snapshot),
            null,
            DataSource.DISK,
        )
    }

    private suspend fun throughBrowser(): FetchResult {
        val bytes = images.load(url)
        val cache = loader.diskCache
        if (cache != null && options.diskCachePolicy.writeEnabled) {
            val editor = cache.openEditor(diskKey())
            if (editor != null) {
                try {
                    options.fileSystem.write(editor.data) { write(bytes) }
                    val snapshot = editor.commitAndOpenSnapshot()
                    if (snapshot !=
                        null
                    ) {
                        return SourceFetchResult(
                            ImageSource(snapshot.data, options.fileSystem, diskKey(), snapshot),
                            null,
                            DataSource.NETWORK,
                        )
                    }
                } catch (error: Exception) {
                    editor.abort()
                }
            }
        }
        return SourceFetchResult(
            ImageSource(Buffer().apply { write(bytes) }, options.fileSystem),
            null,
            DataSource.NETWORK,
        )
    }

    private fun diskKey(): String = options.diskCacheKey ?: url

    class Factory(private val images: ImageHost, private val plain: Fetcher.Factory<Uri>) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme != "http" && data.scheme != "https") return null
            return GuardedImageFetcher(
                data.toString(),
                options,
                images,
                plain.create(data, options, imageLoader),
                imageLoader,
            )
        }
    }
}
