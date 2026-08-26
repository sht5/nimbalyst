package com.tvremote.samsung.network.androidtv

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory

private const val KEY_FILE = "androidtv_client.key.der"
private const val CERT_FILE = "androidtv_client.cert.der"
private const val KEYSTORE_ALIAS = "androidtv-client"

/**
 * The client identity Android TV pairing needs: a self-signed RSA cert the TV remembers after
 * pairing and never validates against a CA — it only reads the public key's modulus/exponent to
 * compute the pairing-secret hash (see [PairingSecret]), and later recognizes this exact cert on
 * reconnect to skip pairing.
 *
 * This is generated in **software**, not the Android Keystore. AndroidKeyStore-backed RSA keys
 * look like the safer default, but they don't support the raw/pre-hashed signing operation TLS's
 * own CertificateVerify step needs during client-certificate authentication — every handshake
 * attempt fails deep in conscrypt with an opaque "RSA routines: internal error" and there's no
 * KeyGenParameterSpec option to authorize around it, since the Keystore only ever signs data it
 * hashes itself. A plain software key has no such restriction. The private key is written to this
 * app's private storage instead (never backed up, inaccessible to other apps) — a reasonable
 * tradeoff since this cert only identifies the app to a device on the local network; it protects
 * nothing sensitive the way the Samsung side's pairing token or account credentials would.
 */
object AndroidTvCertificate {

    private val NO_PASSWORD = CharArray(0)

    @Volatile private var cachedKey: PrivateKey? = null
    @Volatile private var cachedCert: X509Certificate? = null

    @Synchronized
    fun certificate(context: Context): X509Certificate {
        ensureGenerated(context)
        return cachedCert!!
    }

    /** A ready-to-use KeyManagerFactory presenting this identity for TLS client authentication. */
    @Synchronized
    fun keyManagerFactory(context: Context): KeyManagerFactory {
        ensureGenerated(context)
        val keyStore = KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry(KEYSTORE_ALIAS, cachedKey, NO_PASSWORD, arrayOf(cachedCert))
        }
        return KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, NO_PASSWORD)
        }
    }

    fun modulusAndExponent(cert: X509Certificate): Pair<BigInteger, BigInteger> {
        val key = cert.publicKey as RSAPublicKey
        return key.modulus to key.publicExponent
    }

    private fun ensureGenerated(context: Context) {
        if (cachedKey != null) return
        if (loadFromDisk(context)) return
        generateAndStore(context)
    }

    private fun loadFromDisk(context: Context): Boolean {
        val keyFile = File(context.filesDir, KEY_FILE)
        val certFile = File(context.filesDir, CERT_FILE)
        if (!keyFile.exists() || !certFile.exists()) return false

        val privateKey = KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(keyFile.readBytes()))
        val cert = certFile.inputStream().use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }
        cachedKey = privateKey
        cachedCert = cert
        return true
    }

    private fun generateAndStore(context: Context) {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048, SecureRandom()) }.generateKeyPair()

        val subject = X500Name("CN=Samsung TV Remote")
        val now = System.currentTimeMillis()
        val certBuilder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(1),
            Date(now - TimeUnit.DAYS.toMillis(1)),
            Date(now + TimeUnit.DAYS.toMillis(10 * 365)),
            subject,
            keyPair.public,
        )
        val signer = JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.private)
        val cert = JcaX509CertificateConverter().getCertificate(certBuilder.build(signer))

        File(context.filesDir, KEY_FILE).writeBytes(keyPair.private.encoded)
        File(context.filesDir, CERT_FILE).writeBytes(cert.encoded)

        cachedKey = keyPair.private
        cachedCert = cert
    }
}
