package com.sumizuri.app.data.account

import com.sumizuri.app.BuildInfo
import com.sumizuri.sync.core.DeviceInfo
import com.sumizuri.sync.core.DeviceType
import platform.UIKit.*

// The name the person gave the phone or the tablet.
actual fun thisDevice(): DeviceInfo {
    val device = UIDevice.currentDevice
    val type = if (device.userInterfaceIdiom == UIUserInterfaceIdiomPad) DeviceType.Tablet else DeviceType.Phone
    return DeviceInfo(
        device.name
            .trim()
            .ifEmpty { "iPhone" }
            .take(64),
        type,
        BuildInfo.Version.take(32),
    )
}
