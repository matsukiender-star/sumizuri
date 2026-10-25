package com.sumizuri.app.data.account

import com.sumizuri.sync.core.AccountAnswer
import com.sumizuri.sync.core.AccountDelete
import com.sumizuri.sync.core.AdminOverview
import com.sumizuri.sync.core.ApiError
import com.sumizuri.sync.core.ApiVersion
import com.sumizuri.sync.core.DeviceEntry
import com.sumizuri.sync.core.ErrorCodes
import com.sumizuri.sync.core.InviteEntry
import com.sumizuri.sync.core.InviteMade
import com.sumizuri.sync.core.LibraryEntry
import com.sumizuri.sync.core.LogEntry
import com.sumizuri.sync.core.NewInvite
import com.sumizuri.sync.core.NewLibrary
import com.sumizuri.sync.core.PasswordChange
import com.sumizuri.sync.core.RecoverRequest
import com.sumizuri.sync.core.RecoveryChange
import com.sumizuri.sync.core.RecoveryKeyAnswer
import com.sumizuri.sync.core.RecoveryKeyRequest
import com.sumizuri.sync.core.RowLimits
import com.sumizuri.sync.core.RowPage
import com.sumizuri.sync.core.RowPush
import com.sumizuri.sync.core.RowPushed
import com.sumizuri.sync.core.Rows
import com.sumizuri.sync.core.SaltAnswer
import com.sumizuri.sync.core.ServerInfo
import com.sumizuri.sync.core.ServerSettingEntry
import com.sumizuri.sync.core.SettingChange
import com.sumizuri.sync.core.SetupRequest
import com.sumizuri.sync.core.SignedIn
import com.sumizuri.sync.core.SigninRequest
import com.sumizuri.sync.core.SignupRequest
import com.sumizuri.sync.core.TwoStepConfirm
import com.sumizuri.sync.core.TwoStepOff
import com.sumizuri.sync.core.TwoStepSecret
import com.sumizuri.sync.core.TwoStepStart
import com.sumizuri.sync.core.UserChange
import com.sumizuri.sync.core.UserDetail
import com.sumizuri.sync.core.UserEntry
import com.sumizuri.sync.core.UsernameChange
import com.sumizuri.sync.core.V1
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// Why a call to the sync server did not work: a code from the server, or one of the codes below for problems before the server answered.
class SyncProblem(val code: String, message: String) : Exception(message) {
    companion object {
        const val Unreachable = "unreachable"
        const val BadAddress = "bad_address"
        const val NeedsHttps = "needs_https"
        const val TooNew = "server_too_new"
        const val BadAnswer = "bad_answer"
    }
}

// The most a packed page of rows may unpack to: a full page of the biggest rows, as base64.
private const val MostUnpacked = 64 * 1024 * 1024

// Talks to a sync server. Every answer is checked, and a refusal comes back as a SyncProblem with the server's code.
class SyncClient {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val http = HttpClient {
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 30_000
        }
    }

    // What the server says about itself, turned down when it speaks a newer version than this app knows.
    suspend fun info(server: String): ServerInfo {
        val info = read(ServerInfo.serializer()) { http.get(server + V1.Info) }
        if (info.apiVersion > ApiVersion) throw SyncProblem(SyncProblem.TooNew, "This server is newer than the app. Update the app to use it.")
        return info
    }

    suspend fun setup(server: String, request: SetupRequest): SignedIn = send(server + V1.Setup, request, SetupRequest.serializer())

    suspend fun salt(server: String, username: String): SaltAnswer = read(SaltAnswer.serializer()) { http.get(server + V1.Salt) { parameter("username", username) } }

    suspend fun signin(server: String, request: SigninRequest): SignedIn = send(server + V1.Signin, request, SigninRequest.serializer())

    suspend fun signup(server: String, request: SignupRequest): SignedIn = send(server + V1.Signup, request, SignupRequest.serializer())

    suspend fun recoveryKey(server: String, request: RecoveryKeyRequest): RecoveryKeyAnswer = read(RecoveryKeyAnswer.serializer()) {
        http.post(server + V1.RecoveryKey) { setBody(TextContent(json.encodeToString(RecoveryKeyRequest.serializer(), request), ContentType.Application.Json)) }
    }

    suspend fun recover(server: String, request: RecoverRequest): SignedIn = send(server + V1.Recover, request, RecoverRequest.serializer())

    // The calls of a signed in device, each with its token.
    suspend fun account(account: SyncAccount): AccountAnswer = read(AccountAnswer.serializer()) { http.get(account.server + V1.Account) { signed(account) } }

    suspend fun devices(account: SyncAccount): List<DeviceEntry> = read(ListSerializer(DeviceEntry.serializer())) { http.get(account.server + V1.Devices) { signed(account) } }

    suspend fun removeDevice(account: SyncAccount, id: Long) = done { http.delete("${account.server}${V1.Devices}/$id") { signed(account) } }

    suspend fun signout(account: SyncAccount) = done { http.post(account.server + V1.Signout) { signed(account) } }

    suspend fun libraries(account: SyncAccount): List<LibraryEntry> = read(ListSerializer(LibraryEntry.serializer())) { http.get(account.server + V1.Libraries) { signed(account) } }

    suspend fun newLibrary(account: SyncAccount, request: NewLibrary): LibraryEntry = read(LibraryEntry.serializer()) {
        http.post(account.server + V1.Libraries) {
            signed(account)
            setBody(TextContent(json.encodeToString(NewLibrary.serializer(), request), ContentType.Application.Json))
        }
    }

    suspend fun users(account: SyncAccount): List<UserEntry> = read(ListSerializer(UserEntry.serializer())) { http.get(account.server + V1.AdminUsers) { signed(account) } }

    suspend fun invites(account: SyncAccount): List<InviteEntry> = read(ListSerializer(InviteEntry.serializer())) { http.get(account.server + V1.AdminInvites) { signed(account) } }

    suspend fun newInvite(account: SyncAccount, days: Int?): InviteMade = read(InviteMade.serializer()) {
        http.post(account.server + V1.AdminInvites) {
            signed(account)
            setBody(TextContent(json.encodeToString(NewInvite.serializer(), NewInvite(days)), ContentType.Application.Json))
        }
    }

    suspend fun removeInvite(account: SyncAccount, id: String) = done { http.delete("${account.server}${V1.AdminInvites}/$id") { signed(account) } }

    suspend fun overview(account: SyncAccount): AdminOverview = read(AdminOverview.serializer()) { http.get(account.server + V1.AdminOverview) { signed(account) } }

    suspend fun user(account: SyncAccount, id: Long): UserDetail = read(UserDetail.serializer()) { http.get(account.server + V1.adminUser(id)) { signed(account) } }

    suspend fun changeUser(account: SyncAccount, id: Long, change: UserChange): UserEntry = read(UserEntry.serializer()) {
        http.patch(account.server + V1.adminUser(id)) {
            signed(account)
            setBody(TextContent(json.encodeToString(UserChange.serializer(), change), ContentType.Application.Json))
        }
    }

    suspend fun signOutUser(account: SyncAccount, id: Long) = done { http.post(account.server + V1.adminUserSignout(id)) { signed(account) } }

    suspend fun handOver(account: SyncAccount, id: Long) = done { http.post(account.server + V1.adminUserOwner(id)) { signed(account) } }

    suspend fun restoreUser(account: SyncAccount, id: Long) = done { http.post(account.server + V1.adminUserRestore(id)) { signed(account) } }

    suspend fun signOutOthers(account: SyncAccount) = done { http.post(account.server + V1.SignoutOthers) { signed(account) } }

    suspend fun changeLog(account: SyncAccount): List<LogEntry> = read(ListSerializer(LogEntry.serializer())) { http.get(account.server + V1.AdminLog) { signed(account) } }

    suspend fun serverSettings(account: SyncAccount): List<ServerSettingEntry> = read(ListSerializer(ServerSettingEntry.serializer())) { http.get(account.server + V1.AdminSettings) { signed(account) } }

    suspend fun changeSetting(account: SyncAccount, key: String, value: String): List<ServerSettingEntry> = read(ListSerializer(ServerSettingEntry.serializer())) {
        http.patch(account.server + V1.AdminSettings) {
            signed(account)
            setBody(TextContent(json.encodeToString(SettingChange.serializer(), SettingChange(key, value)), ContentType.Application.Json))
        }
    }

    suspend fun changePassword(account: SyncAccount, change: PasswordChange) = done {
        http.post(account.server + V1.Password) {
            signed(account)
            setBody(TextContent(json.encodeToString(PasswordChange.serializer(), change), ContentType.Application.Json))
        }
    }

    suspend fun deleteAccount(account: SyncAccount, change: AccountDelete) = post(account, V1.AccountDelete, change, AccountDelete.serializer())

    suspend fun changeUsername(account: SyncAccount, change: UsernameChange) = post(account, V1.Username, change, UsernameChange.serializer())

    suspend fun changeRecovery(account: SyncAccount, change: RecoveryChange) = post(account, V1.Recovery, change, RecoveryChange.serializer())

    suspend fun startTwoStep(account: SyncAccount, change: TwoStepStart): TwoStepSecret = read(TwoStepSecret.serializer()) {
        http.post(account.server + V1.TwoStepStart) {
            signed(account)
            setBody(TextContent(json.encodeToString(TwoStepStart.serializer(), change), ContentType.Application.Json))
        }
    }

    suspend fun confirmTwoStep(account: SyncAccount, change: TwoStepConfirm) = post(account, V1.TwoStepConfirm, change, TwoStepConfirm.serializer())

    suspend fun turnOffTwoStep(account: SyncAccount, change: TwoStepOff) = post(account, V1.TwoStepOff, change, TwoStepOff.serializer())

    suspend fun turnOffTwoStepFor(account: SyncAccount, id: Long) = done { http.post(account.server + V1.adminUserTwoStepOff(id)) { signed(account) } }

    // A signed in call that sends a body and answers with nothing.
    private suspend fun <T> post(account: SyncAccount, path: String, body: T, serializer: KSerializer<T>) = done {
        http.post(account.server + path) {
            signed(account)
            setBody(TextContent(json.encodeToString(serializer, body), ContentType.Application.Json))
        }
    }

    suspend fun deleteUser(account: SyncAccount, id: Long) = done { http.delete(account.server + V1.adminUser(id)) { signed(account) } }

    // The bytes of rows sent and fetched over the wire, packed, for the details page and the log.
    var rowBytesSent = 0L
        private set
    var rowBytesFetched = 0L
        private set

    // A page of the row changes of a library after a number, oldest first. It comes packed with zlib, which is about a third smaller.
    suspend fun pullRows(account: SyncAccount, libraryId: Long, after: Long, limit: Int = RowLimits.PerBatch): RowPage {
        val response = reach {
            http.get(account.server + V1.rows(libraryId)) {
                signed(account)
                parameter("after", after)
                parameter("limit", limit)
                parameter("packed", 1)
            }
        }
        if (!response.status.isSuccess()) failure(response)
        val bytes = response.readRawBytes()
        rowBytesFetched += bytes.size
        val text = if (response.headers[Rows.PackedHeader] == "zlib") Rows.unpack(bytes, MostUnpacked)?.decodeToString() else bytes.decodeToString()
        return text?.let { runCatching { json.decodeFromString(RowPage.serializer(), it) }.getOrNull() } ?: throw SyncProblem(SyncProblem.BadAnswer, "The page of rows was not understood.")
    }

    // Sends a batch of rows built on the changes up to its base, packed with zlib. The server turns it down with Changed when another device changed one of them since.
    suspend fun pushRows(account: SyncAccount, libraryId: Long, push: RowPush): RowPushed = read(RowPushed.serializer()) {
        http.post(account.server + V1.rows(libraryId)) {
            signed(account)
            header(Rows.PackedHeader, "zlib")
            val packed = Rows.pack(json.encodeToString(RowPush.serializer(), push).encodeToByteArray())
            rowBytesSent += packed.size
            setBody(ByteArrayContent(packed, ContentType.Application.OctetStream))
        }
    }

    // The locked copy of a library with the time that names it, or nothing when no device has sent one yet.
    suspend fun fetchCopy(account: SyncAccount, libraryId: Long): LockedCopy? = try {
        val response = reach { http.get(account.server + V1.snapshot(libraryId)) { signed(account) } }
        if (!response.status.isSuccess()) failure(response)
        LockedCopy(response.readRawBytes(), response.headers["X-Snapshot-At"]?.toLongOrNull() ?: 0)
    } catch (problem: SyncProblem) {
        if (problem.code == ErrorCodes.NotFound) null else throw problem
    }

    private fun HttpRequestBuilder.signed(account: SyncAccount) = header(HttpHeaders.Authorization, "Bearer ${account.token}")

    private suspend fun <T> send(address: String, body: T, serializer: KSerializer<T>): SignedIn = read(SignedIn.serializer()) {
        http.post(address) { setBody(TextContent(json.encodeToString(serializer, body), ContentType.Application.Json)) }
    }

    // A call that answers with nothing, only whether it worked.
    private suspend fun done(call: suspend () -> HttpResponse) {
        answer(call)
    }

    private suspend fun <T> read(serializer: KSerializer<T>, call: suspend () -> HttpResponse): T {
        val text = answer(call)
        return runCatching { json.decodeFromString(serializer, text) }.getOrElse { problem -> throw SyncProblem(SyncProblem.BadAnswer, problem.message ?: "The answer was not understood.") }
    }

    // Sends the call and gives back the text of a good answer, or turns a failed one into a SyncProblem.
    private suspend fun answer(call: suspend () -> HttpResponse): String {
        val response = reach(call)
        if (!response.status.isSuccess()) failure(response)
        return response.bodyAsText()
    }

    // Sends the call, turning a server that could not be reached into a SyncProblem.
    private suspend fun reach(call: suspend () -> HttpResponse): HttpResponse = try {
        call()
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (problem: Exception) {
        val message = problem.message ?: problem::class.simpleName.orEmpty()
        // Android turns down plain http in the release build, which is a matter of the address, not of reaching the server.
        if ("CLEARTEXT" in message) throw SyncProblem(SyncProblem.NeedsHttps, message)
        throw SyncProblem(SyncProblem.Unreachable, message)
    }

    // A refusal, with the server's code and words when it sent them.
    private suspend fun failure(response: HttpResponse): Nothing {
        val error = runCatching { json.decodeFromString(ApiError.serializer(), response.bodyAsText()) }.getOrNull()
        throw SyncProblem(error?.code ?: SyncProblem.Unreachable, error?.message ?: "The server answered ${response.status.value}.")
    }

    companion object {
        // Turns what the person typed into the server's address. Without http or https in front, a home address gets http and any other https. Plain http is only taken for a home address.
        fun addressOf(typed: String): String {
            val text = typed.trim().trimEnd('/')
            if (text.isEmpty()) throw SyncProblem(SyncProblem.BadAddress, "Type the address of the server.")
            val withScheme = when {
                text.startsWith("http://", ignoreCase = true) || text.startsWith("https://", ignoreCase = true) -> text
                isHome(text.substringBefore('/').substringBefore(':')) -> "http://$text"
                else -> "https://$text"
            }
            val url = runCatching { Url(withScheme) }.getOrNull()
            if (url == null || url.host.isBlank()) throw SyncProblem(SyncProblem.BadAddress, "That is not a server address.")
            if (url.protocol.name == "http" && !isHome(url.host)) throw SyncProblem(SyncProblem.NeedsHttps, "Only a server on your home network can use http. Others need https.")
            return withScheme
        }

        // This computer, or an address only reachable inside a home network.
        fun isHome(host: String): Boolean {
            val name = host.lowercase().removePrefix("[").removeSuffix("]")
            if (name == "localhost" || name == "::1" || name.endsWith(".local") || name.endsWith(".lan") || name.endsWith(".home.arpa")) return true
            val parts = name.split('.').mapNotNull { it.toIntOrNull() }
            if (parts.size != 4) return false
            return parts[0] == 10 || parts[0] == 127 || (parts[0] == 192 && parts[1] == 168) || (parts[0] == 172 && parts[1] in 16..31) || (parts[0] == 100 && parts[1] in 64..127)
        }
    }
}

// A locked copy of a library as the server holds it, and the time that names it.
class LockedCopy(val bytes: ByteArray, val at: Long)
