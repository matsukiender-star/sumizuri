package com.sumizuri.sync.core

import kotlinx.serialization.Serializable

// The signed in account as the server knows it.
@Serializable
data class AccountAnswer(
    val userId: Long,
    val username: String,
    val role: Role,
    val createdAt: Long,
    val deviceId: Long,
    val twoStep: Boolean = false,
)

// One device signed in to an account. This one is the device that asked.
@Serializable
data class DeviceEntry(
    val id: Long,
    val name: String,
    val type: DeviceType,
    val appVersion: String,
    val createdAt: Long,
    val lastSeen: Long,
    val thisOne: Boolean,
)

// A library of an account. Its name and key are locked on the device, so the server only keeps them as they came.
@Serializable
data class LibraryEntry(
    val id: Long,
    val name: String,
    val keyByAccount: String,
    val createdAt: Long,
    // The newest full copy of the library on the server, when there is one: when it came, how big it is and which device sent it.
    val snapshotAt: Long? = null,
    val snapshotSize: Long? = null,
    val snapshotDevice: String? = null,
    // The number of the newest row change of the library, when and how much its rows hold, so a device can tell in a few bytes whether anything changed.
    val rowHead: Long = 0,
    val rowsAt: Long? = null,
    val rowsSize: Long? = null,
)

@Serializable
data class NewLibrary(val name: String, val keyByAccount: String)

// A person on the server, for the owner and admins. Nothing of their library is in it.
@Serializable
data class UserEntry(
    val id: Long,
    val username: String,
    val role: Role,
    val createdAt: Long,
    val disabled: Boolean,
    val devices: Int,
    val lastSeen: Long? = null,
    // When the person was deleted, while an admin can still bring them back.
    val deletedAt: Long? = null,
    val twoStep: Boolean = false,
)

// One line of the change log of the server: who did what to whom, and when.
@Serializable
data class LogEntry(
    val at: Long,
    val actor: String,
    val action: String,
    val target: String = "",
    val detail: String = "",
)

// An invite code as the list shows it. The code itself is only shown once, when it is made, so the list knows it by a short id.
@Serializable
data class InviteEntry(
    val id: String,
    val createdBy: String,
    val createdAt: Long,
    val expiresAt: Long? = null,
    val usedBy: String? = null,
)

@Serializable
data class NewInvite(val days: Int? = 7)

@Serializable
data class InviteMade(val code: String, val invite: InviteEntry)

// The server at a glance, for the owner and admins: its facts, how many people and devices it has, and what it stores.
@Serializable
data class AdminOverview(
    val serverName: String,
    val version: String,
    val database: String,
    val signupMode: SignupMode,
    val startedAt: Long,
    val users: Int,
    val admins: Int,
    val disabled: Int,
    val devices: Int,
    val activeToday: Int,
    val profiles: Int,
    val storedBytes: Long,
    val openInvites: Int,
)

// One person in full, for the owner and admins: their entry, their devices, and how much of the server they use.
@Serializable
data class UserDetail(val user: UserEntry, val devices: List<DeviceEntry>, val profiles: Int, val storedBytes: Long)

// A change to a person. Only what is given changes.
@Serializable
data class UserChange(val disabled: Boolean? = null, val role: Role? = null)

// A new password for the signed in account: the proof of the current one, the new one, and whether the other devices are signed out.
@Serializable
data class PasswordChange(
    val currentProof: String,
    val password: NewPassword,
    val signOutOthers: Boolean = false,
    val code: String? = null,
)

// Deletes the signed in account with all of its devices, profiles and copies, given the proof of the password.
@Serializable
data class AccountDelete(val currentProof: String, val code: String? = null)

// A new username for the signed in account, given the proof of the password.
@Serializable
data class UsernameChange(val currentProof: String, val username: String, val code: String? = null)

// A new recovery key for the signed in account, given the proof of the password: the proof of the new key and the account key locked with it.
@Serializable
data class RecoveryChange(
    val currentProof: String,
    val recoveryProof: String,
    val keyByRecovery: String,
    val code: String? = null,
)

// The changes below need the code from the authenticator app as well when the account has two-step sign in.

// Starts turning on two-step sign in, given the proof of the password. The server answers with a new secret, which works once a code from it is confirmed.
@Serializable
data class TwoStepStart(val currentProof: String)

// The secret for the authenticator app, as text to type and as a link a QR code carries.
@Serializable
data class TwoStepSecret(val secret: String, val link: String)

// The first code from the authenticator app, which turns two-step sign in on.
@Serializable
data class TwoStepConfirm(val code: String)

// Turns two-step sign in off, given the proof of the password and a code.
@Serializable
data class TwoStepOff(val currentProof: String, val code: String? = null)
