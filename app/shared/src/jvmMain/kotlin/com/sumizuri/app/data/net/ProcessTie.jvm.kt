package com.sumizuri.app.data.net

import com.sumizuri.app.core.log.AppLog
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString

// The calls of Windows that put a process in a job, a group the system ends together.
@Suppress("FunctionName")
private interface Jobs : Library {
    fun CreateJobObjectW(attributes: Pointer?, name: WString?): Pointer?

    fun SetInformationJobObject(job: Pointer, infoClass: Int, info: Pointer, length: Int): Boolean

    fun OpenProcess(access: Int, inherit: Boolean, pid: Int): Pointer?

    fun AssignProcessToJobObject(job: Pointer, process: Pointer): Boolean

    fun CloseHandle(handle: Pointer): Boolean
}

// The class of job information that holds the limits, the size of that information on a 64 bit Windows, where its flags sit, and the flag that ends every process of the job when the job is closed.
private const val ExtendedLimits = 9
private const val ExtendedLimitsBytes = 144
private const val LimitFlagsAt = 16
private const val KillOnClose = 0x2000

// The rights needed to put a process in a job.
private const val SetQuotaAndTerminate = 0x0100 or 0x0001

private val windows = System.getProperty("os.name").orEmpty().startsWith("Windows")

// One job for the life of the app. Its handle is never closed by hand, so Windows closes it when the app ends however it ends, and that ends the processes in it.
private val job: Pair<Jobs, Pointer>? by lazy {
    if (!windows) return@lazy null
    runCatching {
        val kernel = Native.load("kernel32", Jobs::class.java)
        val made = kernel.CreateJobObjectW(null, null) ?: return@runCatching null
        val limits = Memory(ExtendedLimitsBytes.toLong()).apply {
            clear()
            setInt(LimitFlagsAt.toLong(), KillOnClose)
        }
        if (!kernel.SetInformationJobObject(made, ExtendedLimits, limits, ExtendedLimitsBytes)) return@runCatching null
        kernel to made
    }.getOrNull()
}

// Ties a helper the app started to the life of the app. Without this, a helper of an app that was stopped from outside, such as by the build or a crash, keeps running and keeps its program file locked.
internal actual fun tieToApp(process: Process) {
    val (kernel, made) = job ?: return
    val handle = kernel.OpenProcess(SetQuotaAndTerminate, false, process.pid().toInt()) ?: return
    if (!kernel.AssignProcessToJobObject(made, handle)) AppLog.warn("network", "a helper could not be tied to the app")
    kernel.CloseHandle(handle)
}
