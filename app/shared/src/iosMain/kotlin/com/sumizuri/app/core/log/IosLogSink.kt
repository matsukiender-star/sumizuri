package com.sumizuri.app.core.log

import platform.Foundation.*

// Writes each line to the log of the system, which Xcode and the Console app show.
class IosLogSink : LogSink {
    override fun write(entry: LogEntry, line: String) {
        NSLog("Sumizuri %s", line)
    }
}
