package com.sumizuri.app.data.account

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import java.io.File

// The data protection of Windows locks a secret to the Windows user, so another user of the computer, or the file copied elsewhere, cannot open it. On Linux and a Mac a key in the key store of the system does the same.
actual object DeviceSecret {
    private val windows = System
        .getProperty("os.name")
        .orEmpty()
        .lowercase()
        .contains("win")

    actual val canSeal: Boolean get() = windows || SystemKeyring.ready

    actual fun seal(bytes: ByteArray): ByteArray? = if (!windows) {
        SystemKeyring.seal(bytes)
    } else {
        runCatching {
            call(bytes, protect = true)
        }.getOrNull()
    }

    actual fun open(bytes: ByteArray): ByteArray? = if (!windows) {
        SystemKeyring.open(bytes)
    } else {
        runCatching {
            call(bytes, protect = false)
        }.getOrNull()
    }

    actual fun keepPrivate(file: String) {
        val target = File(file)
        runCatching {
            target.setReadable(false, false)
            target.setReadable(true, true)
            target.setWritable(false, false)
            target.setWritable(true, true)
        }
    }

    private fun call(bytes: ByteArray, protect: Boolean): ByteArray? {
        val input = Blob().apply {
            cbData = bytes.size
            pbData = Memory(bytes.size.toLong().coerceAtLeast(1)).also { it.write(0, bytes, 0, bytes.size) }
        }
        val output = Blob()
        val done = if (protect) {
            Crypt32.lib.CryptProtectData(input, null, null, null, null, NoPrompt, output)
        } else {
            Crypt32.lib.CryptUnprotectData(input, null, null, null, null, NoPrompt, output)
        }
        if (!done) return null
        val data = output.pbData ?: return null
        return try {
            data.getByteArray(0, output.cbData)
        } finally {
            Kernel32.lib.LocalFree(data)
        }
    }

    private const val NoPrompt = 1

    @Structure.FieldOrder("cbData", "pbData")
    class Blob : Structure() {
        @JvmField var cbData: Int = 0

        @JvmField var pbData: Pointer? = null
    }

    private interface Crypt32 : Library {
        fun CryptProtectData(
            dataIn: Blob,
            description: Pointer?,
            entropy: Blob?,
            reserved: Pointer?,
            prompt: Pointer?,
            flags: Int,
            dataOut: Blob,
        ): Boolean
        fun CryptUnprotectData(
            dataIn: Blob,
            description: Pointer?,
            entropy: Blob?,
            reserved: Pointer?,
            prompt: Pointer?,
            flags: Int,
            dataOut: Blob,
        ): Boolean

        companion object {
            val lib: Crypt32 by lazy { Native.load("Crypt32", Crypt32::class.java) }
        }
    }

    private interface Kernel32 : Library {
        fun LocalFree(memory: Pointer): Pointer?

        companion object {
            val lib: Kernel32 by lazy { Native.load("Kernel32", Kernel32::class.java) }
        }
    }
}
