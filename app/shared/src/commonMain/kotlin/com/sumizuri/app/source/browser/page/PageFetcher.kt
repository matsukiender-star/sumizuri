package com.sumizuri.app.source.browser.page

import com.sumizuri.app.source.browser.request.BrowserFetchRequest
import com.sumizuri.app.source.browser.request.BrowserFetchResult
import com.sumizuri.app.source.host.BridgeException
import com.sumizuri.app.source.host.string
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

internal object PageFetcher {
    // Makes the request from inside a page that is already on the site, so the browser's own cookies go with it.
    suspend fun fetch(page: RenderPage, request: BrowserFetchRequest): BrowserFetchResult {
        page.eval(startScript(request))
        val clock = TimeSource.Monotonic.markNow()
        while (true) {
            val json = decodePageAnswer(page.eval(POLL_SCRIPT))
            if (json.flag("done")) {
                json.string("error")?.let { throw BridgeException(it) }
                return BrowserFetchResult(
                    statusCode = (json["statusCode"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0,
                    url = json.string("url").orEmpty(),
                    body = json.string("body").orEmpty(),
                    headers = json.stringMap("headers"),
                    binary = request.binary,
                )
            }
            if (clock.elapsedNow() >= request.timeout) throw BridgeException("The page did not answer the request")
            // Most answers take a few dozen milliseconds, and a page of covers needs dozens of them in a row.
            delay(50.milliseconds)
        }
    }

    private fun startScript(request: BrowserFetchRequest): String {
        val options = buildMap<String, JsonElement> {
            put("method", JsonPrimitive(request.method))
            put("credentials", JsonPrimitive("include"))
            if (request.headers.isNotEmpty()) {
                put(
                    "headers",
                    JsonObject(request.headers.mapValues { JsonPrimitive(it.value) }),
                )
            }
            request.body?.let { put("body", JsonPrimitive(it)) }
        }
        // Bytes are turned into base64 inside the page, because only text can come back out of it.
        val reader = if (request.binary) BYTES_READER else "res.text()"
        return """
            (function () {
              window.__sumizuriFetch = { done: false };
              fetch(${quoteForScript(request.url)}, ${JsonObject(options)})
                .then(function (res) {
                  return $reader.then(function (text) {
                    var headers = {};
                    res.headers.forEach(function (v, k) { headers[k] = v; });
                    window.__sumizuriFetch = { done: true, statusCode: res.status, url: res.url, body: text, headers: headers };
                  });
                })
                .catch(function (e) {
                  window.__sumizuriFetch = { done: true, error: String((e && e.message) || e) };
                });
              return 'started';
            })()
        """.trimIndent()
    }

    private const val BYTES_READER = """res.arrayBuffer().then(function (buf) {
      var bytes = new Uint8Array(buf);
      var out = '';
      for (var i = 0; i < bytes.length; i += 32768) out += String.fromCharCode.apply(null, bytes.subarray(i, i + 32768));
      return btoa(out);
    })"""

    private const val POLL_SCRIPT = """
        (function () {
          var s = window.__sumizuriFetch;
          var payload = JSON.stringify(s && s.done ? s : { done: false });
          return btoa(unescape(encodeURIComponent(payload)));
        })()
    """
}
