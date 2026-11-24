package com.sumizuri.sync.server.auth

import com.sumizuri.sync.core.DeviceInfo
import com.sumizuri.sync.core.ErrorCodes
import com.sumizuri.sync.core.PairClaim
import com.sumizuri.sync.core.PairJoined
import com.sumizuri.sync.core.PairOffer
import com.sumizuri.sync.core.PairOffered
import com.sumizuri.sync.core.Pairing
import com.sumizuri.sync.core.V1
import com.sumizuri.sync.server.ApiException
import com.sumizuri.sync.server.db.Devices
import com.sumizuri.sync.server.db.Users
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.concurrent.ConcurrentHashMap

// The codes being shown now, kept in memory only so a restart ends them. An account has one at a time, and one claim uses it up.
class PairOffers(private val accounts: Accounts) {
    private class Waiting(val userId: Long, val byDevice: Long, val sealedKey: String, val until: Long)

    private val waiting = ConcurrentHashMap<String, Waiting>()

    fun offer(session: Session, offer: PairOffer): PairOffered {
        bytes(offer.id, Pairing.IdBytes, "pair id")
        lockedKey(offer.sealedKey, "pair key")
        val now = System.currentTimeMillis()
        waiting.values.removeIf { it.userId == session.userId || it.until < now }
        val until = now + Pairing.OfferMs
        waiting[ServerCrypto.sha256Hex(offer.id)] = Waiting(session.userId, session.deviceId, offer.sealedKey, until)
        return PairOffered(until)
    }

    fun claim(id: String, device: DeviceInfo): PairJoined {
        checkDevice(device)
        bytes(id, Pairing.IdBytes, "pair id")
        val taken = waiting.remove(ServerCrypto.sha256Hex(id))?.takeIf { it.until >= System.currentTimeMillis() }
            ?: throw over()
        return transaction(accounts.db) {
            // The device that showed the code must still be signed in, and its account still usable.
            val row = Devices
                .join(Users, JoinType.INNER, Devices.userId, Users.id)
                .selectAll()
                .where { Devices.id eq taken.byDevice }
                .singleOrNull()
                ?.takeIf { it[Users.id] == taken.userId && !it[Users.disabled] && it[Users.deletedAt] == null }
                ?: throw over()
            PairJoined(accounts.signInPaired(row, device), taken.sealedKey)
        }
    }

    private fun over() = ApiException(
        HttpStatusCode.Forbidden,
        ErrorCodes.PairOver,
        "That code was used or is too old. Show a new one on the other device.",
    )
}

// Showing a code needs a signed in device, and taking one needs only what the code holds.
fun Route.pairRoutes(offers: PairOffers, sessions: Sessions, limiter: TryLimiter) {
    post(V1.Pair) {
        val session = sessions.of(call)
        val offer = call.receive<PairOffer>()
        call.respond(offers.offer(session, offer))
    }

    post(V1.PairClaim) {
        val request = call.receive<PairClaim>()
        val network = limiter.networkKey(call.request.origin.remoteHost)
        limiter.check(network)
        val joined = try {
            withContext(Dispatchers.IO) { offers.claim(request.id, request.device) }
        } catch (problem: ApiException) {
            if (problem.code == ErrorCodes.PairOver) limiter.failed(network)
            throw problem
        }
        call.respond(joined)
    }
}
