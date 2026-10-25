package com.sumizuri.app.data.account

import com.sumizuri.app.BuildInfo
import com.sumizuri.sync.core.DeviceInfo
import com.sumizuri.sync.core.DeviceType
import java.net.InetAddress

// The computer's name as the system knows it.
actual fun thisDevice(): DeviceInfo {
    val name =
        System.getenv("COMPUTERNAME") ?: System.getenv("HOSTNAME")
            ?: runCatching { InetAddress.getLocalHost().hostName }.getOrNull()
    return DeviceInfo(
        (
            name?.trim()?.ifEmpty {
                null
            } ?: "Computer"
            ).take(64),
        DeviceType.Desktop,
        BuildInfo.Version.take(32),
    )
}
