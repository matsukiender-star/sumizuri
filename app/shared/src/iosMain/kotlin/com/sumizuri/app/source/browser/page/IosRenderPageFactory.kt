package com.sumizuri.app.source.browser.page

class IosRenderPageFactory : RenderPageFactory {
    override suspend fun open(userAgent: String?): RenderPage = IosRenderPage.open(userAgent)
}
