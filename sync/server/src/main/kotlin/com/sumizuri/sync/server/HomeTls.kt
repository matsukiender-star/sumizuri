package com.sumizuri.sync.server

import org.bouncycastle.asn1.ASN1EncodableVector
import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERBitString
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.asn1.x509.Time
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers
import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.spec.ECGenParameterSpec
import java.util.Date

private const val StoreName = "tls.p12"
private const val DayMs = 86_400_000L

// Long enough that the fingerprint a phone kept never has to change.
private const val ValidDays = 30 * 365L

// The certificate a server at home makes for itself. The QR code carries its fingerprint, which stands in for the authorities that cannot vouch for a home address.
class HomeTls private constructor(val store: KeyStore) {
    val fingerprint: String = MessageDigest
        .getInstance("SHA-256")
        .digest(store.getCertificate(Alias).encoded)
        .joinToString("") { "%02x".format(it) }

    companion object {
        const val Alias = "sumizuri"

        // PKCS12 wants a password, while the permissions of the file are what keeps the key.
        val Password: CharArray get() = "sumizuri".toCharArray()

        fun open(folder: File): HomeTls {
            val file = File(folder, StoreName)
            val store = KeyStore.getInstance("PKCS12")
            if (file.isFile) {
                file.inputStream().use { store.load(it, Password) }
            } else {
                store.load(null, null)
                make(store)
                write(store, file)
            }
            return HomeTls(store)
        }

        private fun make(store: KeyStore) {
            val pair = KeyPairGenerator
                .getInstance("EC")
                .apply { initialize(ECGenParameterSpec("secp256r1")) }
                .generateKeyPair()
            val algorithm = AlgorithmIdentifier(X9ObjectIdentifiers.ecdsa_with_SHA256)
            val name = X500Name("CN=Sumizuri")
            val now = System.currentTimeMillis()
            val body = V3TBSCertificateGenerator()
                .apply {
                    setSerialNumber(ASN1Integer(BigInteger(120, SecureRandom()).add(BigInteger.ONE)))
                    setSignature(algorithm)
                    setIssuer(name)
                    setSubject(name)
                    // A day early, for a phone whose clock is a little behind.
                    setStartDate(Time(Date(now - DayMs)))
                    setEndDate(Time(Date(now + ValidDays * DayMs)))
                    setSubjectPublicKeyInfo(SubjectPublicKeyInfo.getInstance(pair.public.encoded))
                }.generateTBSCertificate()
            val signature = Signature.getInstance("SHA256withECDSA").run {
                initSign(pair.private)
                update(body.getEncoded("DER"))
                sign()
            }
            val parts = ASN1EncodableVector().apply {
                add(body)
                add(algorithm)
                add(DERBitString(signature))
            }
            val certificate = CertificateFactory
                .getInstance("X.509")
                .generateCertificate(DERSequence(parts).getEncoded("DER").inputStream())
            store.setKeyEntry(Alias, pair.private, Password, arrayOf(certificate))
        }

        // The key is only ever in a file of its owner, and half a file is never left in its place.
        private fun write(store: KeyStore, file: File) {
            val part = File(file.parentFile, "$StoreName.tmp").toPath()
            Files.deleteIfExists(part)
            if ("posix" in part.fileSystem.supportedFileAttributeViews()) {
                Files.createFile(
                    part,
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")),
                )
            } else {
                Files.createFile(part)
            }
            Files.newOutputStream(part).use { store.store(it, Password) }
            Files.move(part, file.toPath(), StandardCopyOption.ATOMIC_MOVE)
        }
    }
}
