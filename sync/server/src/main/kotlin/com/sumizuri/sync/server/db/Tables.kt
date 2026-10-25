package com.sumizuri.sync.server.db

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table

// The step of the database layout this database is at, one row.
object SchemaVersion : Table("schema_version") {
    val version = integer("version")
}

// Small facts about the server itself: its owner once claimed, the hash of its setup code, and its own secret.
object ServerState : Table("server_state") {
    val key = varchar("key", 64)
    val value = text("value")
    override val primaryKey = PrimaryKey(key)
}

// One row for each account. The server keeps checks of the proofs and the account key locked twice, never a password or an open key.
object Users : Table("users") {
    val id = long("id").autoIncrement()
    val username = varchar("username", 32)

    // The name in lower case, so Akari and akari are the same account.
    val usernameKey = varchar("username_key", 32).uniqueIndex()
    val role = varchar("role", 8)
    val salt = varchar("salt", 64)
    val kdf = text("kdf")
    val passwordCheck = text("password_check")
    val keyByPassword = text("key_by_password")
    val recoveryCheck = text("recovery_check")
    val keyByRecovery = text("key_by_recovery")
    val createdAt = long("created_at")
    val disabled = bool("disabled").default(false)

    // When the person was deleted. They stay for a while so an admin can bring them back, then the server removes them.
    val deletedAt = long("deleted_at").nullable()

    // The secret of two-step sign in once it is on, the one waiting for its first code while it is being turned on, and the step of the code used last, so a code never works twice.
    val twoStepSecret = varchar("two_step_secret", 64).nullable()
    val twoStepPending = varchar("two_step_pending", 64).nullable()
    val twoStepUsed = long("two_step_used").default(0)
    override val primaryKey = PrimaryKey(id)
}

// One row for each signed in device. Only the hash of its token is kept.
object Devices : Table("devices") {
    val id = long("id").autoIncrement()
    val userId = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE).index()
    val name = varchar("name", 64)
    val type = varchar("type", 16)
    val appVersion = varchar("app_version", 32)
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val createdAt = long("created_at")
    val lastSeen = long("last_seen")
    override val primaryKey = PrimaryKey(id)
}

// Codes that let a person sign up while sign up is by invite. Only the hash of a code is kept.
object Invites : Table("invites") {
    val codeHash = varchar("code_hash", 64)
    val createdBy = long("created_by").references(Users.id, onDelete = ReferenceOption.CASCADE)
    val createdAt = long("created_at")
    val expiresAt = long("expires_at").nullable()
    val usedBy = long("used_by").references(Users.id, onDelete = ReferenceOption.SET_NULL).nullable()
    override val primaryKey = PrimaryKey(codeHash)
}

// The libraries of an account. The name and the library's key are locked on the device, so the server keeps them as it got them.
object Libraries : Table("libraries") {
    val id = long("id").autoIncrement()
    val userId = long("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE).index()
    val name = text("name")
    val keyByAccount = text("key_by_account")
    val createdAt = long("created_at")

    // The number of the newest row change, and when it came.
    val rowHead = long("row_head").default(0)
    val rowsAt = long("rows_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

// The newest snapshot of each library: its locked bytes, which the server cannot read, and when and from which device it came.
object Snapshots : Table("snapshots") {
    val libraryId = long("library_id").references(Libraries.id, onDelete = ReferenceOption.CASCADE)
    val data = blob("data")
    val size = long("size")
    val createdAt = long("created_at")
    val deviceName = varchar("device_name", 64)
    override val primaryKey = PrimaryKey(libraryId)
}

// What the owner, admins and people did on the server, newest last, for the change log admins read.
object AuditLog : Table("audit_log") {
    val id = long("id").autoIncrement()
    val at = long("at").index()
    val actor = varchar("actor", 32)
    val action = varchar("action", 32)
    val target = varchar("target", 64).default("")
    val detail = text("detail").default("")
    override val primaryKey = PrimaryKey(id)
}

// The rows of each library, locked on the device. Each change takes the next number of its library, so devices fetch what changed after the number they last saw.
object SyncRows : Table("sync_rows") {
    val libraryId = long("library_id").references(Libraries.id, onDelete = ReferenceOption.CASCADE)
    val rowId = varchar("row_id", 64)
    val seq = long("seq")
    val data = blob("data")
    val size = integer("size")
    override val primaryKey = PrimaryKey(libraryId, rowId)

    init {
        index(false, libraryId, seq)
    }
}
