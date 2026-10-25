package com.sumizuri.app.source.host

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.core.log.LogLevel
import com.sumizuri.app.source.browser.BrowserBroker
import com.sumizuri.app.source.browser.ChallengeStillShowing
import com.sumizuri.app.source.browser.page.BrowserUnavailableException
import com.sumizuri.app.source.browser.page.PageLoadException
import com.sumizuri.app.source.browser.request.RenderRequest
import com.sumizuri.app.source.host.BridgeException
import com.sumizuri.app.source.host.BridgeHandler
import com.sumizuri.app.source.host.ChallengeException
import com.sumizuri.app.source.host.HostBridge
import com.sumizuri.app.source.host.int
import com.sumizuri.app.source.host.net.HttpHost
import com.sumizuri.app.source.host.store.InMemoryValueStore
import com.sumizuri.app.source.host.store.StorageHost
import com.sumizuri.app.source.host.string
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// The host an extension runs against: its own HTTP client, its stored state and settings, and the hidden browser. App is made from these calls.
class StandardHost(
    val http: HttpHost = HttpHost(),
    val storage: StorageHost = StorageHost("source", InMemoryValueStore()),
    // The hidden browser, shared with the HTTP host. Without one, executeInWebView is refused.
    val browser: BrowserBroker? = null,
    // The name the source goes by in the log.
    val label: String = "source",
) {
    val bridge: HostBridge = HostBridge(
        mapOf<String, BridgeHandler>(
            "fetchUrl" to { http.fetch(it) },
            "storageGet" to { storage.get(it) },
            "storageSet" to { storage.set(it) },
            "render" to { render(it) },
            "log" to { scriptLog(it) },
            "sleep" to {
                // Bounded, so a source cannot park a call for ever.
                delay((it.int("ms") ?: 0).coerceIn(0, 10_000).toLong())
                ok()
            },
        ),
    )

    // A line the extension wrote with console.log and its relatives.
    private fun scriptLog(arguments: JsonObject): JsonElement {
        val level = when (arguments.string("level")) {
            "error" -> LogLevel.Error
            "warn" -> LogLevel.Warn
            "debug" -> LogLevel.Debug
            else -> LogLevel.Info
        }
        val text = arguments.string("text").orEmpty().take(2000)
        when (level) {
            LogLevel.Error -> AppLog.error(label, text)
            LogLevel.Warn -> AppLog.warn(label, text)
            LogLevel.Debug -> AppLog.debug(label, text)
            LogLevel.Info -> AppLog.info(label, text)
        }
        return ok()
    }

    private suspend fun render(arguments: JsonObject): JsonElement {
        val broker = browser ?: throw BridgeException("Rendering pages is not available on this device.")
        val request = RenderRequest.fromJson(arguments)
        return try {
            broker.render(request).toJson()
        } catch (error: ChallengeStillShowing) {
            throw ChallengeException(error.url, "the hidden browser could not get past it")
        } catch (error: PageLoadException) {
            throw BridgeException(error.message ?: "The page could not be loaded")
        } catch (error: BrowserUnavailableException) {
            throw BridgeException(error.message ?: "The browser could not be started")
        }
    }

    fun close() = http.close()

    private fun ok(): JsonElement = buildJsonObject { put("ok", true) }
}
