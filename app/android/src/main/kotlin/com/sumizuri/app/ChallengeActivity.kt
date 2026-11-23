package com.sumizuri.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.sumizuri.app.data.net.refusedPlainPage
import kotlinx.coroutines.CompletableDeferred

// A screen with the site in a WebView, so a person can pass a bot check the hidden browser could not. Its cookies are the ones the hidden browser uses, so the clearance counts for it. Back goes back in the page first and leaves when there is nowhere to go.
class ChallengeActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val web = WebView(this)
        web.layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(web, true)
        }
        web.webViewClient = object : WebViewClient() {
            // Only Live may use plain http, so a site asking for it here gets nothing.
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                refusedPlainPage(request)
        }
        setContentView(web)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (web.canGoBack()) web.goBack() else finish()
                }
            },
        )
        intent.getStringExtra(EXTRA_URL)?.let(web::loadUrl)
    }

    override fun onDestroy() {
        super.onDestroy()
        waiting?.complete(Unit)
    }

    companion object {
        const val EXTRA_URL = "url"

        // Set by the solver that opened the screen, and finished when the screen closes.
        var waiting: CompletableDeferred<Unit>? = null
    }
}
