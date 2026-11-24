package com.sumizuri.app.data.net

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

actual val plainHttpBlocked: Boolean = true

// What a page of the app's browsers gets for a plain http address: an empty refusal in place of the page or the picture.
fun refusedPlainPage(request: WebResourceRequest): WebResourceResponse? =
    if (request.url.scheme.equals("http", ignoreCase = true)) {
        WebResourceResponse(
            "text/plain",
            "utf-8",
            403,
            "Plain http is only allowed in Live",
            emptyMap(),
            ByteArrayInputStream(ByteArray(0)),
        )
    } else {
        null
    }
