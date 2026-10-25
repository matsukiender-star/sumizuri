package com.sumizuri.app.data.account

import android.os.Build
import com.sumizuri.app.AndroidApp
import com.sumizuri.app.BuildInfo
import com.sumizuri.sync.core.DeviceInfo
import com.sumizuri.sync.core.DeviceType

// The phone's maker and model, and a tablet when its narrow side is at least 600 dp.
actual fun thisDevice(): DeviceInfo {
    val model = Build.MODEL.orEmpty()
    val maker = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
    val name = (if (model.startsWith(maker, ignoreCase = true)) model else "$maker $model").trim().ifEmpty { "Android" }
    val tablet = runCatching { AndroidApp.context.resources.configuration.smallestScreenWidthDp >= 600 }.getOrDefault(
        false,
    )
    return DeviceInfo(name.take(64), if (tablet) DeviceType.Tablet else DeviceType.Phone, BuildInfo.Version.take(32))
}
