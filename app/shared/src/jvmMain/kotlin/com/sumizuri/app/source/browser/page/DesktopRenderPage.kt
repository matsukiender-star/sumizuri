package com.sumizuri.app.source.browser.page

import dev.datlag.kcef.KCEFBrowser
import dev.datlag.kcef.KCEFClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JWindow
import javax.swing.SwingUtilities

// A hidden Chromium page, in a window that sits outside the screen.
class DesktopRenderPage internal constructor(
    private val client: KCEFClient,
    private val browser: KCEFBrowser,
    private val window: JWindow,
    private val state: DesktopLoadState,
) : RenderPage {

    override suspend fun load(url: String) {
        val done = CompletableDeferred<Unit>()
        state.loading = done
        state.error = null
        state.sawChallenge = false
        withContext(Dispatchers.IO) { browser.loadURL(url) }
        try {
            done.await()
        } finally {
            if (state.loading === done) state.loading = null
        }
        val error = state.error
        if (error != null && !state.sawChallenge) {
            throw PageLoadException(
                "The page could not be loaded ($error) at ${url.substringAfter("://").substringBefore('/')}",
            )
        }
    }

    override suspend fun runFirst(script: String) {
        state.early = script
    }

    // The library takes a function body, so the trimmed script is returned from one, since a new line after return ends it.
    override suspend fun eval(script: String): String = browser.evaluateJavaScript("return ${script.trim()}") ?: "null"

    override suspend fun close() {
        withContext(Dispatchers.IO) {
            try {
                SwingUtilities.invokeAndWait { window.dispose() }
                browser.dispose()
            } finally {
                client.dispose()
            }
        }
    }
}
