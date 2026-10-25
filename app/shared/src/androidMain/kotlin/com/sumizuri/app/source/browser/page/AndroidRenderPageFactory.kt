package com.sumizuri.app.source.browser.page

import android.content.Context

// Makes the hidden pages on Android.
class AndroidRenderPageFactory(private val context: Context) : RenderPageFactory {
    override suspend fun open(userAgent: String?): RenderPage = AndroidRenderPage.open(context, userAgent)
}
