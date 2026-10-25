package com.sumizuri.sync.core

// A new account as the app makes it: what goes to the server, the open account key that stays on the device, and the recovery key to show the person once.
class MadeAccount(val request: NewAccount, val accountKey: ByteArray, val recoveryKey: String)

// Making and unlocking the keys of an account. The password and the open account key never leave the device.
object AccountKeys {
    // Makes the account key, locks it with the password and with a new recovery key, and the proofs of both.
    fun make(username: String, password: String, kdf: Kdf = Kdf()): MadeAccount {
        val accountKey = SyncCrypto.randomBytes(32)
        val recovery = SyncCrypto.randomBytes(RecoveryCode.Bytes)
        val recoveryKeys = SyncCrypto.recoveryKeys(recovery)
        val request = NewAccount(
            username = username.trim(),
            password = newPassword(password, accountKey, kdf),
            recoveryProof = SyncCrypto.base64(recoveryKeys.proof),
            keyByRecovery = SyncCrypto.base64(
                SyncCrypto.lock(recoveryKeys.unlock, accountKey, SyncCrypto.AccountKeyPurpose),
            ),
        )
        return MadeAccount(request, accountKey, RecoveryCode.format(recovery))
    }

    // A password with a new salt, locking the account key it is given.
    fun newPassword(password: String, accountKey: ByteArray, kdf: Kdf = Kdf()): NewPassword {
        val salt = SyncCrypto.randomBytes(Limits.SaltBytes)
        val keys = SyncCrypto.passwordKeys(password, salt, kdf)
        return NewPassword(
            salt = SyncCrypto.base64(salt),
            kdf = kdf,
            proof = SyncCrypto.base64(keys.proof),
            keyByPassword = SyncCrypto.base64(SyncCrypto.lock(keys.unlock, accountKey, SyncCrypto.AccountKeyPurpose)),
        )
    }

    // A new recovery key for an account key: what goes to the server, and the key to show the person once. The current password proof is added by the caller.
    fun newRecovery(accountKey: ByteArray): Pair<RecoveryChange, String> {
        val recovery = SyncCrypto.randomBytes(RecoveryCode.Bytes)
        val keys = SyncCrypto.recoveryKeys(recovery)
        val change = RecoveryChange(
            currentProof = "",
            recoveryProof = SyncCrypto.base64(keys.proof),
            keyByRecovery = SyncCrypto.base64(SyncCrypto.lock(keys.unlock, accountKey, SyncCrypto.AccountKeyPurpose)),
        )
        return change to RecoveryCode.format(recovery)
    }

    // The proof of a password for signing in, from the salt the server gave, and the key to open the account key with afterwards.
    fun signInKeys(password: String, salt: SaltAnswer): SecretKeys =
        SyncCrypto.passwordKeys(password, SyncCrypto.fromBase64(salt.salt), salt.kdf)

    // Opens the account key with the unlock key of the password, or gives nothing when it does not fit.
    fun openAccountKey(unlock: ByteArray, keyByPassword: String): ByteArray? =
        SyncCrypto.unlock(unlock, SyncCrypto.fromBase64(keyByPassword), SyncCrypto.AccountKeyPurpose)
}

// The recovery key as a person writes it down: 32 letters and digits in groups of four, such as K7QM-2XPA-... Letters that look alike are left out, and the case does not matter.
object RecoveryCode {
    const val Bytes = 20
    private const val Letters = "ABCDEFGHJKMNPQRSTVWXYZ0123456789"

    fun format(bytes: ByteArray): String {
        val text = StringBuilder()
        var buffer = 0
        var bits = 0
        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bits += 8
            while (bits >= 5) {
                text.append(Letters[(buffer shr (bits - 5)) and 31])
                bits -= 5
            }
        }
        return text.chunked(4).joinToString("-")
    }

    // Reads a recovery key back, or nothing when it is not one. O is read as 0 and I or L as 1, the way people often mistype them.
    fun parse(text: String): ByteArray? {
        val clean = text.uppercase().filter { it.isLetterOrDigit() }.map {
            when (it) {
                'O' -> '0'
                'I', 'L' -> '1'
                'U' -> 'V'
                else -> it
            }
        }
        if (clean.size != Bytes * 8 / 5) return null
        val out = ArrayList<Byte>()
        var buffer = 0
        var bits = 0
        for (char in clean) {
            val value = Letters.indexOf(char)
            if (value < 0) return null
            buffer = (buffer shl 5) or value
            bits += 5
            if (bits >= 8) {
                out.add(((buffer shr (bits - 8)) and 0xFF).toByte())
                bits -= 8
            }
        }
        return out.toByteArray()
    }
}

// A library as the app makes it: what goes to the server, and its open key, which stays on the device.
class MadeLibrary(val request: NewLibrary, val libraryKey: ByteArray)

// A library opened on the device: its name and key, unlocked with the account key.
class OpenLibrary(val id: Long, val name: String, val libraryKey: ByteArray)

// Each library has its own key, locked with the account key, and its name is locked with that key. A library can then get a new key without touching the others.
object LibraryKeys {
    private const val KeyPurpose = "sumizuri library key v1"
    private const val NamePurpose = "sumizuri library name v1"

    fun make(name: String, accountKey: ByteArray): MadeLibrary {
        val libraryKey = SyncCrypto.randomBytes(32)
        val request = NewLibrary(
            name = SyncCrypto.base64(SyncCrypto.lock(libraryKey, name.trim().encodeToByteArray(), NamePurpose)),
            keyByAccount = SyncCrypto.base64(SyncCrypto.lock(accountKey, libraryKey, KeyPurpose)),
        )
        return MadeLibrary(request, libraryKey)
    }

    // Opens a library from the server, or gives nothing when this account key does not fit it.
    fun open(entry: LibraryEntry, accountKey: ByteArray): OpenLibrary? {
        val libraryKey =
            runCatching {
                SyncCrypto.unlock(
                    accountKey,
                    SyncCrypto.fromBase64(entry.keyByAccount),
                    KeyPurpose,
                )
            }.getOrNull()
                ?: return null
        val name =
            runCatching { SyncCrypto.unlock(libraryKey, SyncCrypto.fromBase64(entry.name), NamePurpose) }.getOrNull()
                ?: return null
        return OpenLibrary(entry.id, name.decodeToString(), libraryKey)
    }
}
