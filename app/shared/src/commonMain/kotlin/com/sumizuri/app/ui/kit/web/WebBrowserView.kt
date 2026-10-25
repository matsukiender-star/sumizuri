package com.sumizuri.app.ui.kit.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

// What a page with a site in it can do with the browser.
interface WebControls {
    fun back()
    fun forward()
    fun reload()
    fun stop()
}

// The site in a page of the app as the browser reports it, and the buttons to steer it. The browser is the one the sources use, with the same cookies, so a site a person signs in to or a bot check they pass here counts for the sources too.
class WebBrowserState(val startUrl: String) {
    var url by mutableStateOf(startUrl)
    var title by mutableStateOf("")
    var loading by mutableStateOf(true)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)

    // Why the browser could not start, or null while it is fine.
    var problem by mutableStateOf<String?>(null)

    internal var controls: WebControls? = null

    fun back() = controls?.back()
    fun forward() = controls?.forward()
    fun reload() = controls?.reload()
    fun stop() = controls?.stop()
}

// The site itself, drawn inside the app.
@Composable
expect fun WebBrowserView(state: WebBrowserState, modifier: Modifier = Modifier)

// Opens a site the way the person chose: in a page of the app, or in their own browser. The app gives it, so every button that opens a site does the same.
val LocalOpenSite = staticCompositionLocalOf<(String) -> Unit> { {} }
