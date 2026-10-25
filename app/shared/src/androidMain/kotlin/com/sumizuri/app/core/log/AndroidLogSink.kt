package com.sumizuri.app.core.log

import android.util.Log

// Sends each line to Logcat under the tag Sumizuri.
class AndroidLogSink : LogSink {
    override fun write(entry: LogEntry, line: String) {
        val priority = when (entry.level) {
            LogLevel.Debug -> Log.DEBUG
            LogLevel.Info -> Log.INFO
            LogLevel.Warn -> Log.WARN
            LogLevel.Error -> Log.ERROR
        }
        Log.println(priority, "Sumizuri", "${entry.tag} ${entry.message}")
    }
}
