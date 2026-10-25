package com.sumizuri.app.data.net

import android.os.Handler
import android.os.Looper
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.sumizuri.app.AndroidApp
import com.sumizuri.app.core.log.AppLog
import java.io.File

// Whether the hidden browser was pointed at the router, so turning it off takes it back. Without a router the browser is never woken for this, which would slow the start of the app.
@Volatile private var browserRouted = false

internal actual fun applyRouter(port: Int?) {
    ProxyRoute.use(port)
    if (port == null && !browserRouted) return
    Handler(Looper.getMainLooper()).post {
        runCatching {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
                AppLog.warn(
                    "network",
                    "this WebView cannot take a proxy, the hidden browser goes straight to the sites",
                )
                return@post
            }
            val controller = ProxyController.getInstance()
            if (port == null) {
                controller.clearProxyOverride({ it.run() }, {})
                browserRouted = false
            } else {
                controller.setProxyOverride(
                    ProxyConfig.Builder().addProxyRule("socks5://127.0.0.1:$port").build(),
                    { it.run() },
                    {},
                )
                browserRouted = true
            }
        }.onFailure { AppLog.warn("network", "the hidden browser could not take the router, ${it.message}") }
    }
}

actual val networkSupported: Boolean = true

// wireproxy comes as a native library, since Android only lets an app run programs from the folder of its native libraries.
internal actual fun wireproxyBinary(): File? =
    File(AndroidApp.context.applicationInfo.nativeLibraryDir, "libwireproxy.so").takeIf { it.isFile }

// Android ends the processes of an app with the app, so a helper needs no tie.
internal actual fun tieToApp(process: Process) = Unit
