package com.sumizuri.app.data

// The icon of the app in the launcher, which a person can pick on the systems that let an app change it: the usual ink one, or the same book on another color.
enum class AppIcon(val color: Long) {
    Ink(0xFF0F1114),
    Paper(0xFFEDE6D6),
    Sakura(0xFFE7A3B6),
    Indigo(0xFF2E3170),
    Matcha(0xFF6F8F55),
}

expect object AppIcons {
    // Whether this system lets the app change its icon.
    val supported: Boolean

    fun current(): AppIcon

    fun choose(icon: AppIcon)
}
