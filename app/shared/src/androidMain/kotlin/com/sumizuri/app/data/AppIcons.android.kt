package com.sumizuri.app.data

import android.content.ComponentName
import android.content.pm.PackageManager
import com.sumizuri.app.AndroidApp

// Each icon is an entry of the launcher that opens the app, and the one picked is the only one on. The launcher may take a moment to show the change.
actual object AppIcons {
    actual val supported: Boolean = true

    private fun entry(icon: AppIcon) = ComponentName(AndroidApp.context, "com.sumizuri.app.Icon${icon.name}")

    actual fun current(): AppIcon {
        val manager = AndroidApp.context.packageManager
        return AppIcon.entries.firstOrNull { icon ->
            when (manager.getComponentEnabledSetting(entry(icon))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == AppIcon.Ink
                else -> false
            }
        } ?: AppIcon.Ink
    }

    // The new entry is turned on before the old one goes, so the app is never without one.
    actual fun choose(icon: AppIcon) {
        val manager = AndroidApp.context.packageManager
        manager.setComponentEnabledSetting(
            entry(icon),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        for (other in AppIcon.entries - icon) {
            manager.setComponentEnabledSetting(
                entry(other),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
