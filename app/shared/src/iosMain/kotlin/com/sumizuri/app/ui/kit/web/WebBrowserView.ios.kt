package com.sumizuri.app.ui.kit.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import com.sumizuri.app.source.browser.page.newWebView
import kotlinx.cinterop.ObjCSignatureOverride
import platform.Foundation.*
import platform.WebKit.*
import platform.darwin.*

// Keeps the state of the page in step with the web view: where it is, what it is called, and whether it is loading.
private class PageWatcher(private val state: WebBrowserState) :
    NSObject(),
    WKNavigationDelegateProtocol {
    private fun sync(web: WKWebView) {
        web.URL?.absoluteString?.let { state.url = it }
        state.title = web.title.orEmpty()
        state.canGoBack = web.canGoBack
        state.canGoForward = web.canGoForward
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        state.loading = true
        sync(webView)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        state.loading = false
        sync(webView)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: platform.Foundation.NSError) {
        state.loading = false
        sync(webView)
    }
}

// The site itself, in the web view of the system. It shares its cookies with the sources.
@Composable
actual fun WebBrowserView(state: WebBrowserState, modifier: Modifier) {
    val web = remember { newWebView(null) }
    val watcher = remember(state) { PageWatcher(state) }
    DisposableEffect(web, watcher) {
        web.navigationDelegate = watcher
        state.controls = object : WebControls {
            override fun back() {
                web.goBack()
            }
            override fun forward() {
                web.goForward()
            }
            override fun reload() {
                web.reload()
            }
            override fun stop() {
                web.stopLoading()
            }
        }
        NSURL.URLWithString(state.startUrl)?.let { web.loadRequest(NSURLRequest.requestWithURL(it)) }
        onDispose {
            state.controls = null
            web.navigationDelegate = null
            web.stopLoading()
        }
    }
    UIKitView(factory = { web }, modifier = modifier)
}
