package com.sumizuri.app.data.net

import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelDirectTCPIP
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import com.sumizuri.app.core.log.AppLog
import java.security.MessageDigest
import java.util.Base64

private const val Tag = "network"
private const val ConnectMs = 15_000
private const val OpenMs = 15_000
private const val AliveEveryMs = 30_000
private const val WindowBytes = 2 * 1024 * 1024
private const val PacketBytes = 32 * 1024

// An open connection that can reach sites for the router.
internal interface Link {
    fun open(host: String, port: Int): Tube

    fun close()
}

// A proxy the person runs or rents. Every site is a new connection to it, so there is nothing to keep open.
internal class ProxyLink(private val connection: Connection) : Link {
    override fun open(host: String, port: Int): Tube {
        val server = connection["host"].trim().trim('[', ']')
        val serverPort = connection.number("port")
        val user = connection["user"].trim()
        val password = connection["password"]
        return if (connection["kind"] == "http") {
            Tube.httpConnect(server, serverPort, user, password, host, port)
        } else {
            Tube.socks5(server, serverPort, user, password, host, port)
        }
    }

    override fun close() = Unit
}

// An SSH connection to the person's server, which carries each site inside it, as ssh -D does. It signs in when first needed and again when it dropped. The fingerprint of the server is trusted the first time and kept, and another one after that is refused.
internal class SshLink(private val connection: Connection) : Link {
    private var session: Session? = null

    override fun open(host: String, port: Int): Tube {
        val channel = session().getStreamForwarder(host, port) as ChannelDirectTCPIP
        val inbox = Inbox { channel.disconnect() }
        channel.setOutputStream(inbox.sink)
        widen(channel)
        val output = channel.outputStream
        channel.connect(OpenMs)
        return Tube(inbox, output) {
            inbox.close()
            channel.disconnect()
        }
    }

    @Synchronized
    override fun close() {
        session?.disconnect()
        session = null
    }

    @Synchronized
    private fun session(): Session {
        session?.takeIf { it.isConnected }?.let { return it }
        // The fingerprint is read from the setting each time, so one kept since this link was made is checked too.
        val trusted = Network.setting.value
            .connection(connection.id)
            ?.get("hostKey")
            .orEmpty()
        val jsch = JSch()
        jsch.addIdentity(
            connection["key.name"].ifBlank { "key" },
            connection["key"].encodeToByteArray(),
            null,
            connection["passphrase"].takeIf { it.isNotEmpty() }?.encodeToByteArray(),
        )
        jsch.hostKeyRepository = KnownServer(trusted) { seen ->
            Network.learned(connection.id, "hostKey", seen)
            AppLog.info(Tag, "the server of ${connection.name} is trusted from now on, $seen")
        }
        val made = jsch.getSession(connection["user"].trim(), connection["host"].trim(), connection.number("port"))
        made.setConfig("StrictHostKeyChecking", "yes")
        made.setConfig("PreferredAuthentications", "publickey")
        made.userInfo = Quiet
        made.setServerAliveInterval(AliveEveryMs)
        made.serverAliveCountMax = 3
        made.connect(ConnectMs)
        AppLog.info(Tag, "signed in to the server of ${connection.name}")
        session = made
        return made
    }
}

// The fingerprint of a server key as OpenSSH writes it.
internal fun fingerprintOf(key: ByteArray): String =
    "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(key))

// JSch opens a forwarded channel with a window of 128 KB, which held a video to a third of what the line carries. The window is made bigger before the channel opens.
private fun widen(channel: Channel) {
    runCatching {
        for ((name, value) in listOf(
            "setLocalWindowSizeMax" to WindowBytes,
            "setLocalWindowSize" to WindowBytes,
            "setLocalPacketSize" to PacketBytes,
        )) {
            Channel::class.java
                .getDeclaredMethod(name, Int::class.javaPrimitiveType)
                .apply {
                    isAccessible = true
                }.invoke(channel, value)
        }
    }.onFailure { AppLog.warn(Tag, "the window of a channel could not be widened, ${it.message}") }
}

// Answers no to every question JSch would ask a person, since the app checks the server itself.
private object Quiet : UserInfo {
    override fun getPassphrase(): String? = null

    override fun getPassword(): String? = null

    override fun promptPassword(message: String?) = false

    override fun promptPassphrase(message: String?) = false

    override fun promptYesNo(message: String?) = false

    override fun showMessage(message: String?) = Unit
}

// The one server a link trusts. With no fingerprint kept yet, the first one is trusted and handed back to be kept.
private class KnownServer(private val trusted: String, private val onFirst: (String) -> Unit) : HostKeyRepository {
    override fun check(host: String?, key: ByteArray?): Int {
        val seen = key?.let(::fingerprintOf) ?: return HostKeyRepository.NOT_INCLUDED
        if (trusted.isEmpty()) {
            onFirst(seen)
            return HostKeyRepository.OK
        }
        return if (seen == trusted) HostKeyRepository.OK else HostKeyRepository.CHANGED
    }

    override fun add(hostkey: HostKey?, ui: UserInfo?) = Unit

    override fun remove(host: String?, type: String?) = Unit

    override fun remove(host: String?, type: String?, key: ByteArray?) = Unit

    override fun getKnownHostsRepositoryID(): String = "sumizuri"

    override fun getHostKey(): Array<HostKey> = emptyArray()

    override fun getHostKey(host: String?, type: String?): Array<HostKey> = emptyArray()
}
