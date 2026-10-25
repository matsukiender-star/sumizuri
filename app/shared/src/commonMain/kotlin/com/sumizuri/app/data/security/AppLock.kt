package com.sumizuri.app.data.security

import com.sumizuri.app.data.settings.SettingKey
import com.sumizuri.app.data.settings.SettingsRepository
import com.sumizuri.app.data.text.nowMillis
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// When the app hides itself from screenshots and the list of recent apps: never, only while incognito, or always.
enum class SecureScreen { Off, Incognito, Always }

// The choices of the lock: whether it is on, how long the app may be away before it locks again, whether a PIN is set, and when the screen is hidden.
data class LockPrefs(
    val on: Boolean = false,
    // Seconds away before the lock comes back, 0 for every time, or -1 for only when the app starts.
    val afterSeconds: Int = 0,
    val hasPin: Boolean = false,
    val secureScreen: SecureScreen = SecureScreen.Off,
)

val LockAfterChoices = listOf(0, 60, 300, 900, 1800, 3600, -1)

// How hard a PIN is stretched before it is kept, and how long the salt and the kept hash are.
private const val PinIterations = 120_000
private const val SaltBytes = 16
private const val HashBits = 256

// The lock of the app. It starts locked when it is on, locks again when the app was away for longer than the person allows, and opens with the lock of the system or, where there is none, with a PIN. A PIN is kept only as a salted hash.
class AppLock(private val settings: SettingsRepository, private val scope: CoroutineScope) {
    val prefs: Flow<LockPrefs> = settings.prefixed(SettingKey.SecurityPrefix).map { saved ->
        LockPrefs(
            on = saved[SettingKey.LockOn] == "true",
            afterSeconds = saved[SettingKey.LockAfter]?.toIntOrNull() ?: 0,
            hasPin = !saved[SettingKey.LockPin].isNullOrBlank(),
            secureScreen =
            SecureScreen.entries.firstOrNull { it.name == saved[SettingKey.SecureScreen] } ?: SecureScreen.Off,
        )
    }

    // Null until the choices are read, so nothing of the library shows for a moment before the lock.
    private val mutableLocked = MutableStateFlow<Boolean?>(null)
    val locked: StateFlow<Boolean?> = mutableLocked

    // When the app went away, or null while it is in front.
    private var awayAt: Long? = null

    // True while the lock of the system is on the screen, which takes the app away for a moment without that counting.
    private var prompting = false

    init {
        scope.launch { mutableLocked.value = canLock(prefs.first()) }
    }

    // A lock that cannot be opened is not shown: the system lock was taken off the device, or the PIN is missing.
    private fun canLock(now: LockPrefs): Boolean =
        now.on && (if (DeviceUnlock.usesSystem) DeviceUnlock.available() else now.hasPin)

    // The app went to the background, or its window lost the focus. A locked app is already locked, so the screen of the system lock, which also takes the app away, does not count.
    fun onAway() {
        if (mutableLocked.value == false && awayAt == null && !prompting) awayAt = nowMillis()
    }

    fun onBack() {
        prompting = false
        val at = awayAt ?: return
        awayAt = null
        scope.launch {
            val now = prefs.first()
            if (canLock(now) && now.afterSeconds >= 0 &&
                nowMillis() - at >= now.afterSeconds * 1000L
            ) {
                mutableLocked.value = true
            }
        }
    }

    fun lockNow() {
        scope.launch { if (canLock(prefs.first())) mutableLocked.value = true }
    }

    fun unlock() {
        awayAt = null
        prompting = false
        mutableLocked.value = false
    }

    // Called just before the lock of the system is shown.
    fun onPrompt() {
        prompting = true
    }

    suspend fun setOn(on: Boolean) = settings.setFlag(SettingKey.LockOn, on)

    suspend fun setAfter(seconds: Int) = settings.setNumber(SettingKey.LockAfter, seconds)

    suspend fun setSecureScreen(mode: SecureScreen) = settings.setText(SettingKey.SecureScreen, mode.name)

    // Keeps a new PIN as its salt and hash, both in hex.
    suspend fun setPin(pin: String) {
        val salt = CryptographyRandom.nextBytes(SaltBytes)
        settings.setText(SettingKey.LockPin, salt.toHexString() + ":" + hash(pin, salt).toHexString())
    }

    suspend fun checkPin(pin: String): Boolean {
        val (salt, kept) = settings.textNow(SettingKey.LockPin)?.split(':')?.takeIf { it.size == 2 } ?: return false
        return hash(pin, salt.hexToByteArray()).toHexString() == kept
    }

    @OptIn(DelicateCryptographyApi::class)
    private suspend fun hash(pin: String, salt: ByteArray): ByteArray = CryptographyProvider.Default
        .get(PBKDF2)
        .secretDerivation(SHA256, PinIterations, HashBits.bits, salt)
        .deriveSecretToByteArray(pin.encodeToByteArray())
}

// How the system opens the lock. Where it has a lock of its own, as on a phone, the app uses it, and elsewhere the app asks for a PIN of its own.
expect object DeviceUnlock {
    // Whether the system lock opens the app, as opposed to a PIN.
    val usesSystem: Boolean

    // Whether the device has a lock the app can ask for now.
    fun available(): Boolean

    // Whether the app can hide itself from screenshots and the list of recent apps.
    val canHideScreen: Boolean

    // Whether the app counts as away when its window loses the focus. A phone tells the app itself when it goes to the background.
    val awayOnBlur: Boolean
}
