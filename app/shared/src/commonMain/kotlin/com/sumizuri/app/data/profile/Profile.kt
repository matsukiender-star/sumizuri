package com.sumizuri.app.data.profile

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.sumizuri.app.data.text.safeFileName

// How long a deleted profile can be brought back.
const val TrashDays = 15
private const val DayMs = 24 * 60 * 60 * 1000L

// One reader of this device. The file is where the library of the profile is kept, and null for the first profile, which keeps the file the app had before profiles. One in the trash keeps every file where it was, as covers are saved as whole paths.
data class Profile(
    val id: Long,
    val name: String,
    val avatar: String?,
    val createdAt: Long,
    val file: String?,
    val deletedAt: Long? = null,
) {
    // A clock that went back never ends the wait.
    fun trashEnded(now: Long): Boolean = deletedAt != null && now - deletedAt >= TrashDays * DayMs

    // The days left in the trash, counting a day begun as whole, and 0 for the last one.
    fun trashDaysLeft(now: Long): Int {
        val left = (deletedAt ?: return 0) + TrashDays * DayMs - now
        if (left <= DayMs) return 0
        return ((left + DayMs - 1) / DayMs).coerceAtMost(TrashDays.toLong()).toInt()
    }
}

// The profiles as they are now, the one that is open, and apart from them those in the trash, so nothing that lists profiles sees those.
data class RegistryState(val profiles: List<Profile>, val activeId: Long, val trash: List<Profile> = emptyList()) {
    val active: Profile get() = profiles.first { it.id == activeId }
}

internal const val ProfileExtension = ".db"
private const val LongestName = 60

// The file for a profile, called after it. Marks a file name cannot hold become underscores, and a name that is taken gets a number.
fun profileFileName(name: String, taken: Collection<String>): String =
    safeFileName(name, ProfileExtension, taken, fallback = "profile", longest = LongestName)

// The place the view models of one profile are kept, so they end together when the profile is left.
class ProfileStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

// The profiles of this device, for the screens that show or change them.
val LocalProfiles = staticCompositionLocalOf<ProfileHost> { error("No ProfileHost was provided") }
