package com.sumizuri.sync.server.auth

import com.sumizuri.sync.core.ErrorCodes
import com.sumizuri.sync.core.RecoverRequest
import com.sumizuri.sync.core.RecoveryKeyRequest
import com.sumizuri.sync.core.SetupRequest
import com.sumizuri.sync.core.SigninRequest
import com.sumizuri.sync.core.SignupRequest
import com.sumizuri.sync.core.V1
import com.sumizuri.sync.server.ApiException
import com.sumizuri.sync.server.LiveSettings
import com.sumizuri.sync.server.badRequest
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// The addresses anyone can reach: claiming the server, the salt of a name, making an account, signing in and recovering. Each counts wrong tries, and the database and Argon2 work runs away from the threads that take requests.
fun Route.authRoutes(live: LiveSettings, accounts: Accounts, limiter: TryLimiter) {
    post(V1.Setup) {
        val request = call.receive<SetupRequest>()
        val network = call.networkKey(limiter)
        limiter.check(network)
        call.respond(counted(limiter, arrayOf(network), ErrorCodes.WrongCode) { accounts.claimServer(request) })
    }

    get(V1.Salt) {
        val username =
            call.request.queryParameters["username"]?.takeIf { it.isNotBlank() } ?: badRequest("Give a username.")
        call.respond(withContext(Dispatchers.IO) { accounts.salt(username) })
    }

    post(V1.Signup) {
        val request = call.receive<SignupRequest>()
        val network = call.networkKey(limiter)
        limiter.check(network)
        call.respond(
            counted(limiter, arrayOf(network), ErrorCodes.InviteNeeded) { accounts.signup(request, live.signupMode) },
        )
    }

    post(V1.Signin) {
        val request = call.receive<SigninRequest>()
        val keys = arrayOf(limiter.accountKey(accounts.keyOf(request.username)), call.networkKey(limiter))
        limiter.check(*keys)
        call.respond(counted(limiter, keys, ErrorCodes.WrongSignIn) { accounts.signin(request) })
    }

    post(V1.RecoveryKey) {
        val request = call.receive<RecoveryKeyRequest>()
        val keys = arrayOf(limiter.accountKey(accounts.keyOf(request.username)), call.networkKey(limiter))
        limiter.check(*keys)
        // A right proof does not clear the count here, since the new password still has to follow.
        call.respond(counted(limiter, keys, ErrorCodes.WrongRecovery, clear = false) { accounts.recoveryKey(request) })
    }

    post(V1.Recover) {
        val request = call.receive<RecoverRequest>()
        val keys = arrayOf(limiter.accountKey(accounts.keyOf(request.username)), call.networkKey(limiter))
        limiter.check(*keys)
        call.respond(counted(limiter, keys, ErrorCodes.WrongRecovery) { accounts.recover(request) })
    }
}

private fun RoutingCall.networkKey(limiter: TryLimiter) = limiter.networkKey(request.origin.remoteHost)

// Runs the work, counting a wrong try when it fails with the given code or a wrong two-step code and clearing the count when it works.
private suspend fun <T> counted(
    limiter: TryLimiter,
    keys: Array<String>,
    wrongCode: String,
    clear: Boolean = true,
    work: () -> T,
): T = try {
    withContext(Dispatchers.IO) { work() }.also { if (clear) limiter.succeeded(keys.first()) }
} catch (problem: ApiException) {
    if (problem.code == wrongCode || problem.code == ErrorCodes.WrongTwoStep) limiter.failed(*keys)
    throw problem
}
