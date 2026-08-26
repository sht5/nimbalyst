package com.tvremote.samsung.network.androidtv

import java.security.MessageDigest
import java.security.cert.X509Certificate

/**
 * Computes and checks the pairing-secret hash the Android TV Remote protocol uses to confirm the
 * 6-hex-digit code the TV shows on screen. Ported field-for-field from the reference Python
 * client (tronikos/androidtvremote2's pairing.py) — this exact byte layout, including the
 * deliberate single "0" nibble prefixed onto each RSA exponent, is what the TV expects. Get it
 * wrong and pairing just fails with no useful error from the TV side.
 */
object PairingSecret {

    /** @return the 32-byte SHA-256 secret, or null if [pairingCode] fails its own leading checksum byte. */
    fun compute(clientCert: X509Certificate, serverCert: X509Certificate, pairingCode: String): ByteArray? {
        if (pairingCode.length != 6) return null
        val (clientModulus, clientExponent) = AndroidTvCertificate.modulusAndExponent(clientCert)
        val (serverModulus, serverExponent) = AndroidTvCertificate.modulusAndExponent(serverCert)

        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(hexToBytes(clientModulus.toString(16)))
        digest.update(hexToBytes(clientExponent.toString(16)))
        digest.update(hexToBytes(serverModulus.toString(16)))
        digest.update(hexToBytes(serverExponent.toString(16)))
        digest.update(hexToBytes(pairingCode.substring(2)))
        val result = digest.digest()

        val expectedChecksum = pairingCode.substring(0, 2).toIntOrNull(16) ?: return null
        return if ((result[0].toInt() and 0xFF) == expectedChecksum) result else null
    }

    /**
     * A big integer's hex string (as `BigInteger.toString(16)` produces it) has no leading zero
     * padding, so an odd-length string — always true for a typical 5-hex-digit RSA exponent like
     * 65537 (0x10001) — needs one leading zero nibble before it's a whole number of bytes. RSA
     * moduli are always an even number of hex digits in practice (key sizes are multiples of 8
     * bits), so this only ever bites the exponent, but applying it uniformly matches what the
     * reference client does for both.
     */
    private fun hexToBytes(hexIn: String): ByteArray {
        val hex = if (hexIn.length % 2 != 0) "0$hexIn" else hexIn
        return ByteArray(hex.length / 2) { i -> hex.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
    }
}
