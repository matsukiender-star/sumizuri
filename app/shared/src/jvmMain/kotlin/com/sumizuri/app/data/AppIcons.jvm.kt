package com.sumizuri.app.data

// The icon of a computer or an iPhone app is fixed by its installer, so it cannot be changed from inside.
actual object AppIcons {
    actual val supported: Boolean = false

    actual fun current(): AppIcon = AppIcon.Ink

    actual fun choose(icon: AppIcon) = Unit
}
