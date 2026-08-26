package com.tvremote.samsung.network.androidtv

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import javax.security.auth.x500.X500Principal

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"

// v2: the v1 alias only authorized PKCS1 signing, which throws a native "RSA routines: internal
// error" deep in conscrypt/BoringSSL the moment a TLS handshake needs an RSA-PSS signature
// instead (TLS 1.3's CertificateVerify requires PSS; some TLS 1.2 suites negotiate it too). A
// Keystore key's authorized paddings can't be widened after creation, so this bumps the alias to
// mint a fresh key under the broader spec below rather than leaving existing installs stuck.
private const val KEY_ALIAS = "samsung_tv_remote_androidtv_client_v2"

/**
 * The client identity Android TV pairing needs: a self-signed RSA cert the TV remembers after
 * pairing and never validates against a CA — it only reads the public key's modulus/exponent to
 * compute the pairing-secret hash (see [PairingSecret]), and later recognizes this exact cert on
 * reconnect to skip pairing. Generated once, in the Android Keystore, so the private key never
 * leaves hardware-backed storage and never needs writing to disk ourselves (unlike the reference
 * Python client, which has no keystore to lean on and writes a PEM file instead).
 */
object AndroidTvCertificate {

    fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    /** Creates the client keypair/cert on first use; a no-op if one already exists. */
    fun ensureGenerated() {
        val keyStore = keyStore()
        if (keyStore.containsAlias(KEY_ALIAS)) return

        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY,
        )
            .setKeySize(2048)
            .setCertificateSubject(X500Principal("CN=Samsung TV Remote"))
            // Broad enough to cover whichever signature scheme the TLS handshake actually
            // negotiates for CertificateVerify (varies by TLS version and the TV's cipher
            // support) — see the alias comment above for what happens if this is too narrow.
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512)
            .setSignaturePaddings(
                KeyProperties.SIGNATURE_PADDING_RSA_PKCS1,
                KeyProperties.SIGNATURE_PADDING_RSA_PSS,
            )
            .build()
        generator.initialize(spec)
        generator.generateKeyPair()
    }

    fun alias(): String = KEY_ALIAS

    fun certificate(): X509Certificate {
        ensureGenerated()
        return keyStore().getCertificate(KEY_ALIAS) as X509Certificate
    }

    fun modulusAndExponent(cert: X509Certificate): Pair<BigInteger, BigInteger> {
        val key = cert.publicKey as RSAPublicKey
        return key.modulus to key.publicExponent
    }
}
