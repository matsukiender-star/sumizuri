package com.sumizuri.sync.server

import com.sumizuri.sync.core.ApiError
import com.sumizuri.sync.core.ErrorCodes
import com.sumizuri.sync.core.ServerInfo
import com.sumizuri.sync.core.ServerSettings
import com.sumizuri.sync.core.V1
import com.sumizuri.sync.server.account.accountRoutes
import com.sumizuri.sync.server.account.rowRoutes
import com.sumizuri.sync.server.account.snapshotRoutes
import com.sumizuri.sync.server.admin.adminRoutes
import com.sumizuri.sync.server.admin.serverSettingsRoutes
import com.sumizuri.sync.server.admin.userManagementRoutes
import com.sumizuri.sync.server.auth.Accounts
import com.sumizuri.sync.server.auth.Sessions
import com.sumizuri.sync.server.auth.TryLimiter
import com.sumizuri.sync.server.auth.authRoutes
import com.sumizuri.sync.server.db.DatabaseSetting
import com.sumizuri.sync.server.db.ServerDatabase
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlin.system.exitProcess

// When this server started, for the overview of the owner and admins.
val StartedAt: Long = System.currentTimeMillis()

// The version in the jar, or dev for a run from Gradle.
val ServerVersion: String = Config::class.java.`package`?.implementationVersion ?: "dev"

fun main() {
    val config = try {
        Config.load()
    } catch (problem: ConfigException) {
        System.err.println(problem.message)
        exitProcess(2)
    }
    config.dataDir.mkdirs()
    println("Sumizuri sync $ServerVersion on port ${config.port}, data in ${config.dataDir}")
    val database = try {
        ServerDatabase(config.database).also { it.migrate() }
    } catch (problem: Exception) {
        System.err.println("The database could not be opened: ${problem.message}")
        exitProcess(3)
    }
    println(
        when (val setting = config.database) {
            is DatabaseSetting.Sqlite -> "Database: SQLite in ${setting.file}"
            is DatabaseSetting.Postgres -> "Database: Postgres at ${setting.shown}"
        },
    )
    val setup = Setup(database.db)
    // Shown on every start until the server is claimed, each time a new code, so a lost log never locks the owner out.
    setup.newCode()?.let { code ->
        println()
        println("  This server has no owner yet. In the app, add this server and enter the setup code:")
        println()
        println("      $code")
        println()
    }
    val live = LiveSettings(database.db, config.pinned)
    val accounts = Accounts(database.db, setup)
    val sessions = Sessions(database.db)
    val limiter = TryLimiter(setup.secret, maxTries = { live.number(ServerSettings.WrongTries) })
    embeddedServer(Netty, port = config.port, host = config.host) {
        sumizuri(config, live, setup, accounts, limiter, sessions)
    }.start(wait = true)
}

// Everything the server answers, set up once at start.
fun Application.sumizuri(
    config: Config,
    live: LiveSettings,
    setup: Setup,
    accounts: Accounts,
    limiter: TryLimiter,
    sessions: Sessions,
) {
    // Values equal to their default are still sent, such as the version of the conversation, which the app checks.
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                encodeDefaults = true
            },
        )
    }
    install(CallLogging)
    // Behind a reverse proxy, the address of the person comes from the headers the proxy adds.
    if (config.trustProxy) install(XForwardedHeaders)
    install(StatusPages) {
        // A request turned down on purpose gets its own status and code.
        exception<ApiException> { call, problem ->
            call.respond(problem.status, ApiError(problem.code, problem.message.orEmpty()))
        }
        // A body that is not the JSON the address expects.
        exception<BadRequestException> { call, _ ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(ErrorCodes.BadRequest, "The request is not in the form this address expects."),
            )
        }
        // A request that breaks the server gets a plain answer, and the cause goes to the log only.
        exception<Throwable> { call, cause ->
            call.application.log.error("A request failed", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError(ErrorCodes.ServerError, "The server ran into a problem."),
            )
        }
    }
    startHousekeeping(accounts.db, live)
    routing {
        get(V1.Info) {
            call.respond(
                ServerInfo(
                    name = live.name,
                    version = ServerVersion,
                    needsSetup = !setup.isClaimed(),
                    signupMode = live.signupMode,
                    minPassword = live.number(ServerSettings.MinPassword),
                ),
            )
        }
        authRoutes(live, accounts, limiter)
        accountRoutes(accounts, sessions, limiter, live)
        adminRoutes(accounts.db, sessions)
        userManagementRoutes(accounts.db, sessions, config, live, StartedAt)
        serverSettingsRoutes(sessions, live)
        snapshotRoutes(accounts.db, sessions, config.maxUploadMb * 1_048_576L, live)
        rowRoutes(accounts.db, sessions, config.maxUploadMb * 1_048_576L, live)
    }
}
