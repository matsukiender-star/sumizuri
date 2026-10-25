package com.sumizuri.app.core.perf

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import java.io.File

// What Windows counts for the memory of a process, in bytes. The sizes are as wide as a pointer, which the app, built for 64 bits only, reads as longs.
@Structure.FieldOrder(
    "cb", "pageFaultCount", "peakWorkingSetSize", "workingSetSize", "quotaPeakPagedPoolUsage", "quotaPagedPoolUsage",
    "quotaPeakNonPagedPoolUsage", "quotaNonPagedPoolUsage", "pagefileUsage", "peakPagefileUsage", "privateUsage",
)
class ProcessMemoryCounters : Structure() {
    @JvmField var cb: Int = 0

    @JvmField var pageFaultCount: Int = 0

    @JvmField var peakWorkingSetSize: Long = 0

    @JvmField var workingSetSize: Long = 0

    @JvmField var quotaPeakPagedPoolUsage: Long = 0

    @JvmField var quotaPagedPoolUsage: Long = 0

    @JvmField var quotaPeakNonPagedPoolUsage: Long = 0

    @JvmField var quotaNonPagedPoolUsage: Long = 0

    @JvmField var pagefileUsage: Long = 0

    @JvmField var peakPagefileUsage: Long = 0

    @JvmField var privateUsage: Long = 0
}

private interface PsapiCalls : Library {
    fun GetProcessMemoryInfo(process: Pointer, counters: ProcessMemoryCounters, size: Int): Boolean
}

private val system = System.getProperty("os.name").orEmpty().lowercase()

// The library of Windows that tells the memory of a process, loaded once, or null elsewhere.
private val psapi: PsapiCalls? by lazy {
    if ("win" in system &&
        Native.POINTER_SIZE == 8
    ) {
        runCatching { Native.load("psapi", PsapiCalls::class.java) }.getOrNull()
    } else {
        null
    }
}

// Windows' handle for the process itself.
private val currentProcess = Pointer(-1)

// The working set on Windows, the resident size on Linux, and null on a Mac, which has no cheap way to ask.
actual fun processMemoryMb(): Int? {
    psapi?.let { calls ->
        val counters = ProcessMemoryCounters()
        counters.cb = counters.size()
        return if (calls.GetProcessMemoryInfo(currentProcess, counters, counters.cb)) {
            (
                counters.workingSetSize /
                    (1024 * 1024)
                ).toInt()
        } else {
            null
        }
    }
    if ("linux" in system) {
        val line =
            runCatching {
                File("/proc/self/status").useLines { lines -> lines.firstOrNull { it.startsWith("VmRSS:") } }
            }.getOrNull()
                ?: return null
        return line.filter(Char::isDigit).toLongOrNull()?.let { (it / 1024).toInt() }
    }
    return null
}
