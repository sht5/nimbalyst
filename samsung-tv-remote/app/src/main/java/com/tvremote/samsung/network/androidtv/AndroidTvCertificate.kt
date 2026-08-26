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
private const val KEY_ALIAS = "samsung_tv_remote_androidtv_client"

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
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
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
