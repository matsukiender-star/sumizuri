package com.sumizuri.sync.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The newest version of the conversation between the app and the server.
const val ApiVersion = 1

// The addresses of version 1. Each version keeps its own fixed addresses, so a later version is added beside this one and older apps keep working.
object V1 {
    const val Info = "/v1/info"
    const val Setup = "/v1/setup"
    const val Salt = "/v1/auth/salt"
    const val Signup = "/v1/auth/signup"
    const val Signin = "/v1/auth/signin"
    const val RecoveryKey = "/v1/auth/recovery-key"
    const val Recover = "/v1/auth/recover"

    // These need the device's token in an Authorization: Bearer header.
    const val Signout = "/v1/auth/signout"
    const val Account = "/v1/account"
    const val Devices = "/v1/devices"
    const val Libraries = "/v1/libraries"
    const val AdminUsers = "/v1/admin/users"
    const val AdminOverview = "/v1/admin/overview"
    const val AdminSettings = "/v1/admin/settings"
    const val Password = "/v1/account/password"
    const val AccountDelete = "/v1/account/delete"
    const val Username = "/v1/account/username"
    const val Recovery = "/v1/account/recovery"
    const val SignoutOthers = "/v1/devices/signout-others"
    const val AdminLog = "/v1/admin/log"
    const val AdminInvites = "/v1/admin/invites"
    const val TwoStepStart = "/v1/account/two-step/start"
    const val TwoStepConfirm = "/v1/account/two-step/confirm"
    const val TwoStepOff = "/v1/account/two-step/off"

    // The full locked copy of one library, sent and fetched as raw bytes.
    fun snapshot(libraryId: Long) = "/v1/libraries/$libraryId/snapshot"

    // One person, for the owner and admins, and signing out all of their devices.
    fun adminUser(id: Long) = "$AdminUsers/$id"
    fun adminUserSignout(id: Long) = "$AdminUsers/$id/signout"
    fun adminUserOwner(id: Long) = "$AdminUsers/$id/owner"
    fun adminUserRestore(id: Long) = "$AdminUsers/$id/restore"
    fun adminUserTwoStepOff(id: Long) = "$AdminUsers/$id/two-step-off"

    // The rows of a library, fetched in pages after a number and sent in batches.
    fun rows(libraryId: Long) = "/v1/libraries/$libraryId/rows"
}

// Who may make an account on a server.
@Serializable
enum class SignupMode {
    @SerialName("open")
    Open,

    @SerialName("invite")
    Invite,

    @SerialName("closed")
    Closed,
}

// What a server says about itself before anyone signs in, so the app knows whether it can talk to it and whether it still needs its owner.
@Serializable
data class ServerInfo(
    val name: String,
    val version: String,
    val apiVersion: Int = ApiVersion,
    val needsSetup: Boolean,
    val signupMode: SignupMode,
    // The shortest password the server allows. The app checks it, since the server never sees the password itself.
    val minPassword: Int = 10,
)

// The answer to a request that went wrong: a fixed code the app can act on, and words for a person.
@Serializable
data class ApiError(val code: String, val message: String)
