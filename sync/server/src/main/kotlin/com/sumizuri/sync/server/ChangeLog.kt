package com.sumizuri.sync.server

import com.sumizuri.sync.server.db.AuditLog
import org.jetbrains.exposed.v1.jdbc.insert

// Keeps a line in the change log, inside the transaction of the change it tells about, so a change and its line come together or not at all.
object ChangeLog {
    fun record(actor: String, action: String, target: String = "", detail: String = "") {
        AuditLog.insert {
            it[at] = System.currentTimeMillis()
            it[AuditLog.actor] = actor.take(32)
            it[AuditLog.action] = action.take(32)
            it[AuditLog.target] = target.take(64)
            it[AuditLog.detail] = detail
        }
    }
}
