package com.sumizuri.app.core.windows

import com.sun.jna.Function
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference

@Suppress("FunctionName")
private interface Ole32 : Library {
    fun CoInitializeEx(reserved: Pointer?, mode: Int): Int
    fun CoUninitialize()
    fun CoCreateInstance(kind: Pointer, outer: Pointer?, context: Int, iid: Pointer, result: PointerByReference): Int
    fun CoTaskMemFree(memory: Pointer?)
}

// The few pieces of COM the setup uses to reach parts of Windows that have no plain call: making objects, calling their methods by their place in the object's table, and the GUIDs that name them.
internal object Com {
    private const val ApartmentThreaded = 0x2
    private const val NoOle1Dde = 0x4
    private const val InProcess = 0x1

    // The places of the methods every COM object has first.
    const val QueryInterface = 0
    const val Release = 2

    private val ole: Ole32 by lazy { Native.load("ole32", Ole32::class.java) }

    // Runs work on the calling thread with COM set up for it, as a window of Windows such as a picker wants.
    fun <T> on(work: () -> T): T {
        ole.CoInitializeEx(null, ApartmentThreaded or NoOle1Dde)
        try {
            return work()
        } finally {
            ole.CoUninitialize()
        }
    }

    // Makes an object of a kind, asking for one of its interfaces, or null when Windows cannot.
    fun create(kind: Memory, iid: Memory): Pointer? {
        val made = PointerByReference()
        return if (ole.CoCreateInstance(kind, null, InProcess, iid, made) == 0) made.value else null
    }

    fun free(memory: Pointer?) = ole.CoTaskMemFree(memory)

    // Calls the method at a place in the table of a COM object, with the object first as COM wants.
    fun call(target: Pointer, index: Int, vararg args: Any?): Int {
        val table = target.getPointer(0)
        val method = Function.getFunction(table.getPointer(index.toLong() * Native.POINTER_SIZE))
        return method.invokeInt(arrayOf(target, *args))
    }

    // A GUID laid out as Windows keeps it: a number of four bytes, two of two, then eight single bytes.
    fun guid(text: String): Memory {
        val parts = text.split('-')
        val memory = Memory(16)
        memory.setInt(0, parts[0].toLong(16).toInt())
        memory.setShort(4, parts[1].toInt(16).toShort())
        memory.setShort(6, parts[2].toInt(16).toShort())
        val tail = parts[3] + parts[4]
        for (i in 0 until 8) memory.setByte(8L + i, tail.substring(i * 2, i * 2 + 2).toInt(16).toByte())
        return memory
    }
}
