package com.sumizuri.app.data.net

import java.io.File

internal actual fun applyRouter(port: Int?) = ProxyRoute.use(port)

actual val networkSupported: Boolean = true

// wireproxy sits beside libmpv: in the folder of the resources of an installed app, or in the folder the build puts them in.
internal actual fun wireproxyBinary(): File? {
    val name = if (System
            .getProperty("os.name")
            .orEmpty()
            .lowercase()
            .contains("win")
    ) {
        "wireproxy.exe"
    } else {
        "wireproxy"
    }
    val built = System.getProperty("sumizuri.mpv.dir")?.let(::File)
    val folders = listOfNotNull(
        System.getProperty("compose.application.resources.dir")?.let(::File),
        built,
        built?.resolveSibling("linux"),
        File(System.getProperty("user.dir"), "build/app-resources/windows"),
        File(System.getProperty("user.dir"), "build/app-resources/linux"),
    )
    return folders.map { File(it, name) }.firstOrNull { it.isFile }
}
