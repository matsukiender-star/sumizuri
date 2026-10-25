@file:OptIn(ExperimentalForeignApi::class)

package com.sumizuri.app.source.browser.page

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.*
import platform.WebKit.*

// A web view of the system. Every one shares the default store of the system, so the cookies a site gave in a page the person opened are the ones the sources use, and the other way round.
internal fun newWebView(userAgent: String?): WKWebView {
    val view = WKWebView(frame = CGRectMake(0.0, 0.0, 390.0, 844.0), configuration = WKWebViewConfiguration())
    if (userAgent != null) view.customUserAgent = userAgent
    return view
}
