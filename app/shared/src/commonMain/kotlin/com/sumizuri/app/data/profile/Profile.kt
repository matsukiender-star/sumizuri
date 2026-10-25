package com.sumizuri.app.data.profile

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.sumizuri.app.data.text.safeFileName

// One reader of this device. The file is where the library of the profile is kept, and null for the first profile, which keeps the file the app had before profiles.
data class Profile(val id: Long, val name: String, val avatar: String?, val createdAt: Long, val file: String?)

// The profiles as they are now and the one that is open.
data class RegistryState(val profiles: List<Profile>, val activeId: Long) {
    val active: Profile get() = profiles.first { it.id == activeId }
}

private const val Extension = ".db"
private const val LongestName = 60

// The file for a profile, called after it. Marks a file name cannot hold become underscores, and a name that is taken gets a number.
fun profileFileName(name: String, taken: Collection<String>): String =
    safeFileName(name, Extension, taken, fallback = "profile", longest = LongestName)

// The place the view models of one profile are kept, so they end together when the profile is left.
class ProfileStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

// The profiles of this device, for the screens that show or change them.
val LocalProfiles = staticCompositionLocalOf<ProfileHost> { error("No ProfileHost was provided") }
