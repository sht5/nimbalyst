package com.tvremote.samsung.network.androidtv

import android.content.Context
import android.util.Log
import com.tvremote.samsung.data.AndroidTvKey
import com.tvremote.samsung.data.AndroidTvPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

private const val TAG = "AndroidTvClient"
private const val PAIR_PORT = 6467
private const val API_PORT = 6466
private const val CLIENT_NAME = "Samsung TV Remote"
private const val CONNECT_TIMEOUT_MS = 5000
private const val PAIRING_READ_TIMEOUT_MS = 15000

sealed interface AndroidTvConnectionState {
    data object Idle : AndroidTvConnectionState
    data object Connecting : AndroidTvConnectionState
    /** The TV is showing a 6-digit hex code on screen; call [AndroidTvClient.submitPairingCode]. */
    data object AwaitingPairingCode : AndroidTvConnectionState
    data object Connected : AndroidTvConnectionState
    data object Disconnected : AndroidTvConnectionState
    data class Error(val message: String) : AndroidTvConnectionState
}

/**
 * Talks to an Android TV / Google TV device's built-in local remote-control service — the same
 * protocol the official Android TV Remote Control and Google Home apps use. Two phases, both
 * over TLS on their own ports:
 *
 * - **Pairing** (port 6467): a short exchange ending in the TV showing a 6-digit hex code, which
 *   the user types back in. The confirmation is a SHA-256 hash over both sides' certificate
 *   public keys plus that code (see [PairingSecret]) — not a bearer token like the Samsung side.
 * - **Control** (port 6466): a persistent connection, opened once, that carries key presses and
 *   responds to the TV's periodic pings to stay alive.
 *
 * Both channels reuse the same self-signed client certificate ([AndroidTvCertificate]); once the
 * TV has seen it during pairing, reconnecting never needs the code again.
 */
class AndroidTvClient(private val prefs: AndroidTvPrefs, private val context: Context) {

    private val _state = MutableStateFlow<AndroidTvConnectionState>(AndroidTvConnectionState.Idle)
    val state: StateFlow<AndroidTvConnectionState> = _state.asStateFlow()

    private var currentIp: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private var pairingChannel: FramedChannel? = null
    private var controlChannel: FramedChannel? = null

    fun connect(ip: String) {
        currentIp = ip
        job?.cancel()
        job = scope.launch {
            if (prefs.isPaired(ip)) openControlChannel(ip) else startPairing(ip)
        }
    }

    /** Call once the user has typed in the 6-digit hex code the TV displayed. */
    fun submitPairingCode(code: String) {
        val ip = currentIp ?: return
        val channel = pairingChannel ?: return
        job?.cancel()
        job = scope.launch { finishPairing(ip, channel, code) }
    }

    fun sendKey(key: AndroidTvKey) {
        val channel = controlChannel ?: run {
            Log.w(TAG, "sendKey(${key.name}) dropped: not connected")
            return
        }
        scope.launch {
            runCatching { channel.write(RemoteMessages.keyInject(key.code)) }
                .onFailure { Log.w(TAG, "sendKey(${key.name}) failed: ${it.message}") }
        }
    }

    fun disconnect() {
        job?.cancel()
        pairingChannel?.close()
        pairingChannel = null
        controlChannel?.close()
        controlChannel = null
        _state.value = AndroidTvConnectionState.Idle
    }

    fun release() {
        disconnect()
        scope.cancel()
    }

    private suspend fun startPairing(ip: String) {
        _state.value = AndroidTvConnectionState.Connecting
        val channel = try {
            openChannel(ip, PAIR_PORT, PAIRING_READ_TIMEOUT_MS)
        } catch (e: IOException) {
            _state.value = AndroidTvConnectionState.Error("Couldn't reach $ip:$PAIR_PORT — ${e.message}")
            return
        }

        try {
            channel.write(PoloMessages.pairingRequest(CLIENT_NAME))
            expect<PoloIncoming.PairingRequestAck>(channel, "pairing request")

            channel.write(PoloMessages.optionsResponse())
            expect<PoloIncoming.Options>(channel, "options")

            channel.write(PoloMessages.configurationResponse())
            expect<PoloIncoming.ConfigurationAck>(channel, "configuration")

            // The TV is now showing its 6-digit code — hand off to the UI.
            pairingChannel = channel
            _state.value = AndroidTvConnectionState.AwaitingPairingCode
        } catch (e: Exception) {
            channel.close()
            _state.value = AndroidTvConnectionState.Error(e.message ?: "Pairing failed")
        }
    }

    private suspend fun finishPairing(ip: String, channel: FramedChannel, code: String) {
        try {
            val clientCert = AndroidTvCertificate.certificate(context)
            val serverCert = channel.peerCertificate() ?: throw IOException("No certificate from the TV")
            val secret = PairingSecret.compute(clientCert, serverCert, code)
                ?: throw IllegalArgumentException("That code doesn't match — check it and try again")

            channel.write(PoloMessages.secret(secret))
            val reply = channel.read() ?: throw IOException("Connection closed while finishing pairing")
            if (PoloMessages.parse(reply) !is PoloIncoming.SecretAck) {
                throw IOException("TV rejected the pairing code")
            }

            prefs.markPaired(ip)
            channel.close()
            pairingChannel = null
            openControlChannel(ip)
        } catch (e: Exception) {
            channel.close()
            pairingChannel = null
            _state.value = AndroidTvConnectionState.Error(e.message ?: "Pairing failed")
        }
    }

    private suspend fun openControlChannel(ip: String) {
        _state.value = AndroidTvConnectionState.Connecting
        val channel = try {
            // Long-lived connection: block indefinitely on reads rather than timing out between
            // the TV's periodic pings (every ~5s, but not guaranteed to the millisecond).
            openChannel(ip, API_PORT, readTimeoutMs = 0)
        } catch (e: IOException) {
            _state.value = AndroidTvConnectionState.Error("Couldn't reach $ip:$API_PORT — ${e.message}")
            return
        }
        controlChannel = channel
        var negotiatedFeatures = RemoteMessages.SUPPORTED_FEATURES

        try {
            while (true) {
                val frame = channel.read() ?: break
                when (val msg = RemoteMessages.parse(frame)) {
                    is RemoteIncoming.Configure -> {
                        negotiatedFeatures = msg.code1 and RemoteMessages.SUPPORTED_FEATURES
                        channel.write(RemoteMessages.configureResponse(negotiatedFeatures))
                    }
                    RemoteIncoming.SetActive -> channel.write(RemoteMessages.setActiveResponse(negotiatedFeatures))
                    is RemoteIncoming.PingRequest -> channel.write(RemoteMessages.pingResponse(msg.val1))
                    is RemoteIncoming.Start -> _state.value = AndroidTvConnectionState.Connected
                    RemoteIncoming.Unknown -> Unit
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Control channel closed: ${e.message}")
        } finally {
            channel.close()
            if (controlChannel === channel) controlChannel = null
            if (_state.value is AndroidTvConnectionState.Connected) {
                _state.value = AndroidTvConnectionState.Disconnected
            }
        }
    }

    private inline fun <reified T : PoloIncoming> expect(channel: FramedChannel, step: String) {
        val raw = channel.read() ?: throw IOException("Connection closed during $step")
        val parsed = PoloMessages.parse(raw)
        if (parsed is PoloIncoming.StatusError) throw IOException("TV returned an error during $step (status ${parsed.status})")
        if (parsed !is T) throw IOException("Unexpected reply during $step")
    }

    private fun openChannel(ip: String, port: Int, readTimeoutMs: Int): FramedChannel {
        val socket = buildSslContext().socketFactory.createSocket() as SSLSocket
        socket.connect(InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS)
        socket.soTimeout = readTimeoutMs
        socket.startHandshake()
        return FramedChannel(socket)
    }

    /**
     * Presents our client certificate (so the TV can identify/remember us) and accepts whatever
     * self-signed certificate the TV presents back — the protocol never does real CA-chain trust
     * in either direction, only the modulus/exponent comparison in [PairingSecret]. Scoped to
     * this one local connection, same pattern as SamsungTvClient's trust-all TrustManager.
     */
    private fun buildSslContext(): SSLContext {
        val keyManagerFactory = AndroidTvCertificate.keyManagerFactory(context)

        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        return SSLContext.getInstance("TLS").apply {
            init(keyManagerFactory.keyManagers, arrayOf(trustManager), SecureRandom())
        }
    }
}

/** Frames protobuf messages with a varint length prefix over a live TLS socket. */
private class FramedChannel(private val socket: SSLSocket) {
    private val input = socket.getInputStream()
    private val output = socket.getOutputStream()

    fun write(message: ByteArray) {
        output.write(Proto.encodeVarint(message.size.toLong()))
        output.write(message)
        output.flush()
    }

    /** Blocks until a full framed message arrives, or returns null if the stream closed. */
    fun read(): ByteArray? {
        val length = readVarint() ?: return null
        val buffer = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val n = input.read(buffer, offset, length - offset)
            if (n < 0) return null
            offset += n
        }
        return buffer
    }

    fun peerCertificate(): X509Certificate? = socket.session.peerCertificates.firstOrNull() as? X509Certificate

    fun close() {
        runCatching { socket.close() }
    }

    private fun readVarint(): Int? {
        var result = 0
        var shift = 0
        while (true) {
            val b = input.read()
            if (b < 0) return null
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) break
            shift += 7
        }
        return result
    }
}
