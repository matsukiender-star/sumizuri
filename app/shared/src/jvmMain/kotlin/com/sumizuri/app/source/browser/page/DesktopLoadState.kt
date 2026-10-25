package com.sumizuri.app.source.browser.page

import kotlinx.coroutines.CompletableDeferred

// What the load handlers tell the page about the load that is running now.
internal class DesktopLoadState {
    @Volatile var loading: CompletableDeferred<Unit>? = null

    @Volatile var error: String? = null

    @Volatile var sawChallenge = false

    @Volatile var early: String? = null
}
