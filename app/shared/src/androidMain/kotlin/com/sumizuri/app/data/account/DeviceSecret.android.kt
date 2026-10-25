package com.sumizuri.app.data.account

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val Alias = "sumizuri-sync"
private const val NonceBytes = 12

// A key in the Android Keystore that never leaves it, made the first time it is needed. A sealed secret is the nonce followed by the locked bytes.
actual object DeviceSecret {
    actual val canSeal: Boolean get() = runCatching { key() }.isSuccess

    actual fun seal(bytes: ByteArray): ByteArray? = runCatching {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        cipher.iv + cipher.doFinal(bytes)
    }.getOrNull()

    actual fun open(bytes: ByteArray): ByteArray? = runCatching {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes, 0, NonceBytes))
        cipher.doFinal(bytes, NonceBytes, bytes.size - NonceBytes)
    }.getOrNull()

    // The folder of the app is private to it already.
    actual fun keepPrivate(file: String) = Unit

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(Alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val spec = KeyGenParameterSpec
            .Builder(Alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        return KeyGenerator
            .getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore",
            ).apply { init(spec) }
            .generateKey()
    }
}
