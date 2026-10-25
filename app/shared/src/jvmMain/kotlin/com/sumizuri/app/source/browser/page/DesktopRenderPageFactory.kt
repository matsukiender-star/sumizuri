package com.sumizuri.app.source.browser.page

// Makes the hidden pages on desktop.
class DesktopRenderPageFactory(private val runtime: DesktopBrowserRuntime) : RenderPageFactory {
    override suspend fun open(userAgent: String?): RenderPage = DesktopPageBuilder.create(runtime)
}
