package com.sumizuri.app.source.browser.page

import com.sumizuri.app.core.log.AppLog
import com.sumizuri.app.source.browser.ChallengeSolver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cef.browser.CefRendering
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities

// A window with the site in it, in the same browser the hidden pages use, so what the person passes there counts for them.
class DesktopChallengeSolver(private val runtime: DesktopBrowserRuntime) : ChallengeSolver {
    override suspend fun solve(url: String) {
        val client = runtime.client()
        val closed = CompletableDeferred<Unit>()
        val browser = client.createBrowser(url, CefRendering.DEFAULT)
        try {
            withContext(Dispatchers.IO) {
                SwingUtilities.invokeAndWait {
                    JFrame(url.substringAfter("://").substringBefore('/')).apply {
                        layout = BorderLayout()
                        add(browser.uiComponent, BorderLayout.CENTER)
                        setSize(WIDTH, HEIGHT)
                        setLocationRelativeTo(null)
                        defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                        addWindowListener(
                            object : WindowAdapter() {
                                override fun windowClosed(event: WindowEvent) {
                                    closed.complete(Unit)
                                }
                            },
                        )
                        isVisible = true
                    }
                }
            }
            AppLog.info(
                "desktop-browser",
                "a window is open for a person to pass the bot check at ${url.substringAfter(
                    "://",
                ).substringBefore('/')}",
            )
            closed.await()
        } finally {
            withContext(Dispatchers.IO) {
                browser.dispose()
                client.dispose()
            }
        }
    }

    private companion object {
        const val WIDTH = 1000
        const val HEIGHT = 760
    }
}
