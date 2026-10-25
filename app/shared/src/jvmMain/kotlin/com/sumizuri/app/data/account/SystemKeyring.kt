package com.sumizuri.app.data.account

import com.sumizuri.app.core.log.AppLog
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val Service = "sumizuri"
private const val Account = "device-key"
private const val IvBytes = 12
private const val TagBits = 128
private const val WaitSeconds = 20L

// A key of the app kept in the key store of the system, the Keychain on a Mac and the Secret Service of the desktop on Linux, which seals secrets with AES-GCM. A new key is made only when the store says it has none, never when the store is locked or missing, so a secret sealed before is never left without its key.
internal object SystemKeyring {
    private val mac = System
        .getProperty("os.name")
        .orEmpty()
        .lowercase()
        .contains("mac")
    private val key: SecretKeySpec? by lazy { runCatching { found() }.getOrNull() }

    val ready: Boolean get() = key != null

    fun seal(bytes: ByteArray): ByteArray? {
        val secret = key ?: return null
        val iv = ByteArray(IvBytes).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, secret, GCMParameterSpec(TagBits, iv))
        }
        return iv + cipher.doFinal(bytes)
    }

    fun open(bytes: ByteArray): ByteArray? {
        val secret = key ?: return null
        if (bytes.size <= IvBytes) return null
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secret, GCMParameterSpec(TagBits, bytes.copyOfRange(0, IvBytes)))
            cipher.doFinal(bytes, IvBytes, bytes.size - IvBytes)
        }.getOrNull()
    }

    private fun found(): SecretKeySpec? {
        val looked = if (mac) {
            run(listOf("security", "find-generic-password", "-a", Account, "-s", Service, "-w"))
        } else {
            run(listOf("secret-tool", "lookup", "service", Service, "account", Account))
        }
        val text = looked?.second?.trim().orEmpty()
        if (looked?.first == 0 && text.isNotEmpty()) return SecretKeySpec(Base64.getDecoder().decode(text), "AES")
        // The Keychain says 44 for an item it does not have. The Secret Service says 1 with nothing written for a missing item, and writes why when it cannot be reached.
        val missing = looked != null && (if (mac) looked.first == 44 else looked.first == 1 && looked.third.isBlank())
        if (!missing) {
            AppLog.warn("secret", "the key store of the system cannot be used, secrets are kept in a private file only")
            return null
        }
        val made = ByteArray(32).also(SecureRandom()::nextBytes)
        val encoded = Base64.getEncoder().encodeToString(made)
        val stored = if (mac) {
            run(listOf("security", "add-generic-password", "-a", Account, "-s", Service, "-w", encoded, "-U"))
        } else {
            run(
                listOf("secret-tool", "store", "--label=Sumizuri device key", "service", Service, "account", Account),
                input = encoded,
            )
        }
        if (stored?.first != 0) {
            AppLog.warn(
                "secret",
                "the key store of the system did not take the key, secrets are kept in a private file only",
            )
            return null
        }
        AppLog.info("secret", "a key for secrets was made in the key store of the system")
        return SecretKeySpec(made, "AES")
    }

    // Runs a program of the system and gives its exit code, what it wrote and what it said went wrong, or null when it is not there.
    private fun run(command: List<String>, input: String? = null): Triple<Int, String, String>? = runCatching {
        val process = ProcessBuilder(command).start()
        input?.let { text -> process.outputStream.use { it.write(text.encodeToByteArray()) } }
            ?: process.outputStream.close()
        val out = process.inputStream.bufferedReader().readText()
        val err = process.errorStream.bufferedReader().readText()
        if (!process.waitFor(WaitSeconds, TimeUnit.SECONDS)) {
            process.destroy()
            return@runCatching null
        }
        Triple(process.exitValue(), out, err)
    }.getOrNull()
}
