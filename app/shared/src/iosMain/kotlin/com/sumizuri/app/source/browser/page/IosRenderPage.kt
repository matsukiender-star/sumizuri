@file:OptIn(ExperimentalForeignApi::class)

package com.sumizuri.app.source.browser.page

import com.sumizuri.app.ui.asText
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.WebKit.*
import platform.darwin.*
import kotlin.coroutines.resume

// Tells the page when a load ends, and with what.
private class LoadWatcher :
    NSObject(),
    WKNavigationDelegateProtocol {
    var loading: CompletableDeferred<Unit>? = null
    var loadError: String? = null
    var sawChallenge = false

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        if (webView.URL?.absoluteString?.contains("__cf_chl_") == true) sawChallenge = true
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        if (webView.URL?.absoluteString?.contains("__cf_chl_") == true) sawChallenge = true
        loading?.complete(Unit)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
        loadError = "${withError.localizedDescription} (${withError.code})"
        loading?.complete(Unit)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) {
        loadError = "${withError.localizedDescription} (${withError.code})"
        loading?.complete(Unit)
    }
}

// A web view that is never shown. Every call to it runs on the main thread, which is where a web view lives. A page that is not on the screen may be slowed by the system, which a site that waits long before it answers can notice.
class IosRenderPage private constructor(private val web: WKWebView) : RenderPage {
    private val watcher = LoadWatcher()

    init {
        web.navigationDelegate = watcher
    }

    override suspend fun load(url: String) {
        val done = CompletableDeferred<Unit>()
        withContext(Dispatchers.Main) {
            watcher.loading = done
            watcher.loadError = null
            watcher.sawChallenge = false
            val address = NSURL.URLWithString(url) ?: throw PageLoadException("The address $url is not valid")
            web.loadRequest(NSURLRequest.requestWithURL(address))
        }
        try {
            done.await()
        } finally {
            withContext(Dispatchers.Main) { if (watcher.loading === done) watcher.loading = null }
        }
        // Cloudflare answers the first load with a redirect that may look like a failure, and the caller waits it out.
        val error = watcher.loadError
        if (error != null && !watcher.sawChallenge) {
            throw PageLoadException(
                "The page could not be loaded ($error) at ${url.substringAfter("://").substringBefore('/')}",
            )
        }
    }

    override suspend fun runFirst(script: String) {
        withContext(Dispatchers.Main) {
            web.configuration.userContentController.addUserScript(
                WKUserScript(
                    source = script,
                    injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
                    forMainFrameOnly = false,
                ),
            )
        }
    }

    // What the script gave back, written as JSON, the way the other systems give it.
    override suspend fun eval(script: String): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            web.evaluateJavaScript(script) { result, _ -> continuation.resume(jsonOf(result)) }
        }
    }

    override suspend fun close() {
        withContext(Dispatchers.Main) {
            web.stopLoading()
            web.navigationDelegate = null
        }
    }

    companion object {
        suspend fun open(userAgent: String?): IosRenderPage =
            withContext(Dispatchers.Main) { IosRenderPage(newWebView(userAgent)) }
    }
}

private fun jsonOf(value: Any?): String {
    if (value == null || value is NSNull) return "null"
    val data =
        NSJSONSerialization.dataWithJSONObject(value, NSJSONWritingFragmentsAllowed, error = null) ?: return "null"
    return data.asText() ?: "null"
}
