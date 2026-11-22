package com.sumizuri.app.data.net

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.ConnectionSpec
import okhttp3.Protocol
import java.net.Proxy
import java.security.MessageDigest
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

actual fun pinnedHttpClient(fingerprint: String, configure: HttpClientConfig<*>.() -> Unit): HttpClient {
    val trust = OnlyThisCertificate(fingerprint)
    val sockets = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trust), null) }.socketFactory
    return HttpClient(OkHttp) {
        engine {
            config {
                sslSocketFactory(sockets, trust)
                // The name of a home address proves nothing, so the certificate itself is what is compared.
                hostnameVerifier { _, session ->
                    runCatching { trust.matches(session.peerCertificates.first()) }.getOrDefault(false)
                }
                // No plain http, so a wrong address can never send the token unlocked.
                connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
                protocols(listOf(Protocol.HTTP_1_1))
                // A server at home is not reached through the connection the sites go through.
                proxy(Proxy.NO_PROXY)
            }
        }
        configure()
        // A redirect is never a reason to send the token somewhere else.
        followRedirects = false
    }
}

private class OnlyThisCertificate(fingerprint: String) : X509TrustManager {
    private val expected = fingerprint.lowercase().encodeToByteArray()

    fun matches(certificate: Certificate): Boolean {
        val found = MessageDigest
            .getInstance("SHA-256")
            .digest(certificate.encoded)
            .joinToString("") { "%02x".format(it) }
        return MessageDigest.isEqual(found.encodeToByteArray(), expected)
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        val first = chain.firstOrNull()
        if (first == null ||
            !matches(first)
        ) {
            throw CertificateException("This is not the computer whose code was scanned.")
        }
    }

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String): Unit =
        throw CertificateException("Client certificates are not used.")

    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}
