package com.sumizuri.app.ui.kit.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.unit.dp
import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.source.browser.page.DesktopBrowserRuntime
import com.sumizuri.app.ui.kit.art.InkCircularProgressIndicator
import dev.datlag.kcef.KCEFBrowser
import dev.datlag.kcef.KCEFClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefRendering
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import javax.swing.SwingUtilities

// The browser the desktop app runs, set when the app starts, so a page can show a site in the same Chromium the sources use and with the same cookies.
object DesktopWeb {
    @Volatile
    var runtime: DesktopBrowserRuntime? = null
}

// Chromium inside the page. The browser starts on first use, which can take a moment, and longer the very first time when it is still being downloaded.
@Composable
actual fun WebBrowserView(state: WebBrowserState, modifier: Modifier) {
    var browser by remember { mutableStateOf<KCEFBrowser?>(null) }
    var client by remember { mutableStateOf<KCEFClient?>(null) }
    LaunchedEffect(state) {
        val runtime = DesktopWeb.runtime
        if (runtime == null) {
            state.problem = "The browser is not available."
            state.loading = false
            return@LaunchedEffect
        }
        try {
            val made = runtime.client()
            made.addDisplayHandler(
                object : CefDisplayHandlerAdapter() {
                    override fun onAddressChange(browser: CefBrowser, frame: CefFrame, url: String) {
                        if (frame.isMain) SwingUtilities.invokeLater { state.url = url }
                    }

                    override fun onTitleChange(browser: CefBrowser, title: String?) {
                        SwingUtilities.invokeLater { state.title = title.orEmpty() }
                    }
                },
            )
            made.addLoadHandler(
                object : CefLoadHandlerAdapter() {
                    override fun onLoadingStateChange(
                        browser: CefBrowser,
                        isLoading: Boolean,
                        canGoBack: Boolean,
                        canGoForward: Boolean,
                    ) {
                        SwingUtilities.invokeLater {
                            state.loading = isLoading
                            state.canGoBack = canGoBack
                            state.canGoForward = canGoForward
                        }
                    }
                },
            )
            val page = made.createBrowser(state.startUrl, CefRendering.DEFAULT, false)
            state.controls = object : WebControls {
                override fun back() = page.goBack()
                override fun forward() = page.goForward()
                override fun reload() = page.reload()
                override fun stop() = page.stopLoad()
            }
            client = made
            browser = page
        } catch (error: Throwable) {
            AppLog.warn("web", "the page browser could not start: ${error.message}")
            state.problem = error.message ?: "The browser could not start."
            state.loading = false
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            state.controls = null
            browser?.dispose()
            client?.dispose()
        }
    }
    val page = browser
    if (page != null) {
        SwingPanel(factory = { page.uiComponent }, modifier = modifier)
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            val problem = state.problem
            if (problem ==
                null
            ) {
                InkCircularProgressIndicator()
            } else {
                Text(problem, Modifier.fillMaxWidth().padding(24.dp), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
