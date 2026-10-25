package com.sumizuri.app.setup

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.W32APITypeMapper

// The calls of Windows for its registry, in their Unicode forms, so names and folders in any language come through whole.
@Suppress("FunctionName")
private interface Advapi32 : Library {
    fun RegOpenKeyExW(key: Pointer, subKey: String, options: Int, access: Int, result: PointerByReference): Int
    fun RegCreateKeyExW(
        key: Pointer,
        subKey: String,
        reserved: Int,
        kind: String?,
        options: Int,
        access: Int,
        security: Pointer?,
        result: PointerByReference,
        disposition: IntByReference?,
    ): Int
    fun RegQueryValueExW(
        key: Pointer,
        name: String?,
        reserved: Pointer?,
        type: IntByReference,
        data: Pointer?,
        size: IntByReference,
    ): Int
    fun RegSetValueExW(key: Pointer, name: String?, reserved: Int, type: Int, data: Pointer, size: Int): Int
    fun RegEnumKeyExW(
        key: Pointer,
        index: Int,
        name: CharArray,
        nameSize: IntByReference,
        reserved: Pointer?,
        kind: Pointer?,
        kindSize: Pointer?,
        time: Pointer?,
    ): Int
    fun RegCloseKey(key: Pointer): Int
}

// The part of the registry of the person who runs the app, where an install for one person keeps its records.
internal object WinRegistry {
    private const val KeyRead = 0x20019
    private const val KeyWrite = 0x20006
    private const val TypeString = 1
    private const val TypeExpandString = 2
    private const val TypeNumber = 4
    private const val NoMoreItems = 259

    // HKEY_CURRENT_USER, which Windows defines as 0x80000001 widened with its sign.
    private val CurrentUser = Pointer(-0x7FFFFFFFL)

    private val api: Advapi32 by lazy {
        Native.load("advapi32", Advapi32::class.java, mapOf(Library.OPTION_TYPE_MAPPER to W32APITypeMapper.UNICODE))
    }

    private inline fun <T> open(path: String, access: Int, use: (Pointer) -> T): T? {
        val handle = PointerByReference()
        if (api.RegOpenKeyExW(CurrentUser, path, 0, access, handle) != 0) return null
        val key = handle.value
        return try {
            use(key)
        } finally {
            api.RegCloseKey(key)
        }
    }

    fun text(path: String, name: String): String? = open(path, KeyRead) { key ->
        val type = IntByReference()
        val size = IntByReference()
        if (api.RegQueryValueExW(key, name, null, type, null, size) != 0) return@open null
        if (type.value != TypeString && type.value != TypeExpandString) return@open null
        val data = Memory(size.value.toLong() + 2).apply { clear() }
        if (api.RegQueryValueExW(key, name, null, type, data, size) != 0) null else data.getWideString(0)
    }

    fun number(path: String, name: String): Int? = open(path, KeyRead) { key ->
        val type = IntByReference()
        val size = IntByReference(4)
        val data = Memory(4)
        if (api.RegQueryValueExW(key, name, null, type, data, size) != 0 ||
            type.value != TypeNumber
        ) {
            null
        } else {
            data.getInt(0)
        }
    }

    // The names of the keys right under a key.
    fun children(path: String): List<String> = open(path, KeyRead) { key ->
        val names = mutableListOf<String>()
        val buffer = CharArray(512)
        var index = 0
        while (true) {
            val size = IntByReference(buffer.size)
            val result = api.RegEnumKeyExW(key, index++, buffer, size, null, null, null, null)
            if (result == NoMoreItems || result != 0) break
            names += String(buffer, 0, size.value)
        }
        names
    }.orEmpty()

    // Writes names with their text or number into a key, making the key when it is not there.
    fun write(path: String, texts: Map<String, String>, numbers: Map<String, Int>) {
        val handle = PointerByReference()
        check(api.RegCreateKeyExW(CurrentUser, path, 0, null, 0, KeyRead or KeyWrite, null, handle, null) == 0) {
            "the registry key $path could not be made"
        }
        val key = handle.value
        try {
            for ((name, value) in texts) {
                val data = Memory((value.length + 1) * 2L).apply { setWideString(0, value) }
                api.RegSetValueExW(key, name, 0, TypeString, data, (value.length + 1) * 2)
            }
            for ((name, value) in numbers) {
                val data = Memory(4).apply { setInt(0, value) }
                api.RegSetValueExW(key, name, 0, TypeNumber, data, 4)
            }
        } finally {
            api.RegCloseKey(key)
        }
    }
}
