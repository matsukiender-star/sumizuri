package com.sumizuri.app.source.browser.page

import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

internal object PageLoader {
    // Opens the address, and tries once more if the page failed to load.
    suspend fun loadWithRetry(page: RenderPage, url: String) {
        try {
            page.load(url)
        } catch (error: PageLoadException) {
            delay(700.milliseconds)
            page.load(url)
        }
    }
}
