package com.sumizuri.sync.server

import com.sumizuri.sync.core.ServerSettings
import com.sumizuri.sync.server.db.AuditLog
import com.sumizuri.sync.server.db.Devices
import com.sumizuri.sync.server.db.Users
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private const val HourMs = 60 * 60 * 1000L
private const val DayMs = 24 * HourMs

// A deleted person can be brought back for this long.
private const val KeepDeletedMs = 7 * DayMs

// The change log keeps this many lines, the newest.
private const val LogKept = 5_000

// Runs the hourly upkeep while the server runs, the first time right after it starts.
fun CoroutineScope.startHousekeeping(db: Database, live: LiveSettings) = launch {
    while (isActive) {
        runCatching { withContext(Dispatchers.IO) { housekeeping(db, live) } }
        delay(HourMs)
    }
}

// Removes the people deleted more than a week ago with their profiles and copies, signs out devices idle longer than the setting allows, and trims the change log.
private fun housekeeping(db: Database, live: LiveSettings) = transaction(db) {
    val now = System.currentTimeMillis()
    val expired = (Users.deletedAt.isNotNull()) and (Users.deletedAt less now - KeepDeletedMs)
    val gone = Users.selectAll().where { expired }.map { it[Users.username] }
    if (gone.isNotEmpty()) {
        Users.deleteWhere { expired }
        gone.forEach { ChangeLog.record("server", "removed for good", it) }
    }
    val days = live.number(ServerSettings.IdleDeviceDays)
    if (days > 0) {
        val signedOut = Devices.deleteWhere { Devices.lastSeen less now - days * DayMs }
        if (signedOut > 0) ChangeLog.record("server", "signed out idle devices", detail = "$signedOut")
    }
    val oldest = AuditLog
        .selectAll()
        .orderBy(AuditLog.id, SortOrder.DESC)
        .limit(1)
        .offset(LogKept.toLong())
        .singleOrNull()
        ?.get(AuditLog.id)
    if (oldest != null) AuditLog.deleteWhere { AuditLog.id less oldest + 1 }
}
