package com.sumizuri.app.source.browser.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

// A hidden WebView. Every call to it runs on the main thread, which is where a WebView lives.
class AndroidRenderPage private constructor(private val webView: WebView) : RenderPage {
    private var loading: CompletableDeferred<Unit>? = null
    private var loadError: String? = null
    private var sawChallenge = false

    // Kept for a WebView too old to add a script at the start of each document, which then gets it as the page starts.
    private var lateEarly: String? = null

    private val client = object : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            if (url.contains("__cf_chl_")) sawChallenge = true
            lateEarly?.let { view.evaluateJavascript(it, null) }
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (url.contains("__cf_chl_")) sawChallenge = true
            loading?.complete(Unit)
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            // Only the page itself matters, and a blocked picture or tracker is not a failed load.
            if (request.isForMainFrame) loadError = "${error.description} (${error.errorCode})"
        }
    }

    init {
        webView.webViewClient = client
    }

    override suspend fun load(url: String) {
        val done = CompletableDeferred<Unit>()
        withContext(Dispatchers.Main) {
            loading = done
            loadError = null
            sawChallenge = false
            webView.loadUrl(url)
        }
        try {
            done.await()
        } finally {
            withContext(Dispatchers.Main) { if (loading === done) loading = null }
        }
        // Cloudflare answers the first load with a redirect that may look like a failure, and the caller waits it out.
        val error = loadError
        if (error != null && !sawChallenge) {
            throw PageLoadException(
                "The page could not be loaded ($error) at ${url.substringAfter("://").substringBefore('/')}",
            )
        }
    }

    override suspend fun runFirst(script: String) {
        withContext(Dispatchers.Main) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                WebViewCompat.addDocumentStartJavaScript(webView, script, setOf("*"))
            } else {
                lateEarly = script
            }
        }
    }

    override suspend fun eval(script: String): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            webView.evaluateJavascript(script) { result -> continuation.resume(result ?: "null") }
        }
    }

    override suspend fun close() {
        withContext(Dispatchers.Main) {
            webView.stopLoading()
            webView.destroy()
        }
    }

    companion object {
        @SuppressLint("SetJavaScriptEnabled")
        suspend fun open(context: Context, userAgent: String?): AndroidRenderPage = withContext(Dispatchers.Main) {
            val webView = try {
                WebView(context.applicationContext)
            } catch (error: Exception) {
                throw BrowserUnavailableException(
                    "Could not start the embedded browser: ${error.message}. It needs the Android System WebView.",
                )
            }
            webView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                // The page is never shown, so a popup could never be dismissed.
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                if (userAgent != null) userAgentString = userAgent
            }
            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(webView, true)
            }
            AndroidRenderPage(webView)
        }
    }
}
