package com.sumizuri.app.source.browser.page

// One hidden browser page. Each platform provides its own, and the rest works through eval.
interface RenderPage {
    // Opens the address and returns when the page finished loading, or throws a load exception.
    suspend fun load(url: String)

    // Runs a script in the page and returns what it gave back, as text.
    suspend fun eval(script: String): String

    // Sets a script that runs in every new document of the page before the page's own scripts. A platform that cannot do that early runs it as soon as it can.
    suspend fun runFirst(script: String) {}

    suspend fun close()
}

fun interface RenderPageFactory {
    // Opens a new page. A null user agent keeps the browser's own.
    suspend fun open(userAgent: String?): RenderPage
}
