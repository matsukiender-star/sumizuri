package com.sumizuri.sync.server

import com.sumizuri.sync.server.auth.ServerCrypto
import com.sumizuri.sync.server.db.ServerState
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import java.security.MessageDigest

// The server's own facts: the one time code that lets the first person claim it, who claimed it, and a secret of its own. Only the hash of the code is kept. Calls that end in In run inside a transaction the caller opened.
class Setup(private val db: Database) {
    // A secret made on the first start, for made up salts and network fingerprints. It never leaves the server.
    val secret: ByteArray = transaction(db) {
        read(SecretKey)?.let { ServerCrypto.fromBase64(it) }
            ?: ServerCrypto.randomBytes(32).also { write(SecretKey, ServerCrypto.base64(it)) }
    }

    fun isClaimed(): Boolean = transaction(db) { claimedIn() }

    fun claimedIn(): Boolean = read(OwnerKey) != null

    // Makes a new code while the server is not claimed, replacing any code from an earlier start, and gives it back to be shown once.
    fun newCode(): String? = transaction(db) {
        if (claimedIn()) return@transaction null
        val code = ServerCrypto.readableCode()
        write(CodeKey, hashOf(code))
        code
    }

    // Whether a code is the one shown, ignoring case, spaces and dashes.
    fun matchesIn(code: String): Boolean {
        val kept = read(CodeKey) ?: return false
        return MessageDigest.isEqual(kept.toByteArray(), hashOf(code).toByteArray())
    }

    // Marks the server as claimed by this account. The owner row is added, not replaced, so two people claiming at once cannot both win: the second fails on the key that is already there.
    fun claimIn(userId: Long) {
        ServerState.insert {
            it[key] = OwnerKey
            it[value] = userId.toString()
        }
        ServerState.deleteWhere { key eq CodeKey }
    }

    private fun read(key: String): String? = ServerState
        .selectAll()
        .where { ServerState.key eq key }
        .singleOrNull()
        ?.get(ServerState.value)

    private fun write(key: String, text: String) {
        ServerState.upsert {
            it[ServerState.key] = key
            it[value] = text
        }
    }

    private fun hashOf(code: String): String = ServerCrypto.codeHash(code)

    private companion object {
        const val OwnerKey = "owner"
        const val CodeKey = "setup_code_hash"
        const val SecretKey = "secret"
    }
}
