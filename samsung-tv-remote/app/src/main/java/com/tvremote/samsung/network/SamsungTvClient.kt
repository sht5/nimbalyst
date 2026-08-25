package com.tvremote.samsung.network

import android.util.Base64
import android.util.Log
import com.tvremote.samsung.data.RemoteKey
import com.tvremote.samsung.data.TvPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

private const val TAG = "SamsungTvClient"

/** Name the TV shows on its "Allow this device to connect?" prompt. */
private const val REMOTE_APP_NAME = "Samsung TV Remote"

/** Plaintext port that also serves a small JSON device-info document (name, model, MAC). */
private const val INFO_PORT = 8001

/** How many times to check whether the TV is back on the network after a wake signal. */
private const val WAKE_RETRY_ATTEMPTS = 10

/** Delay between wake-retry checks. 10 attempts * 1.5s = TV gets ~15s to boot networking. */
private const val WAKE_RETRY_INTERVAL_MS = 1500L

private const val WAKE_PROBE_TIMEOUT_MS = 800

sealed interface TvConnectionState {
    data object Idle : TvConnectionState
    data object Connecting : TvConnectionState
    /** No saved pairing token yet — the TV is showing its on-screen Allow prompt. */
    data object AwaitingPairing : TvConnectionState
    data object Connected : TvConnectionState
    data object Disconnected : TvConnectionState
    /** A Wake-on-LAN packet was sent; waiting for the TV's network stack to come back up. */
    data object WakingUp : TvConnectionState
    data class Error(val message: String) : TvConnectionState
}

/**
 * Talks to a Samsung Smart TV's built-in local remote-control websocket
 * server (the same protocol the SmartThings app and Samsung's own "Smart
 * View" feature use — there is no cloud involved, everything happens over
 * the LAN the phone and TV both sit on).
 *
 * Modern TVs (2016+, Tizen) serve an encrypted channel on port 8002 with a
 * self-signed certificate, and require the token handshake below. Some
 * older or unusual configurations only expose the plaintext port 8001. We
 * try the encrypted port first and fall back automatically.
 */
class SamsungTvClient(private val prefs: TvPrefs) {

    private val _state = MutableStateFlow<TvConnectionState>(TvConnectionState.Idle)
    val state: StateFlow<TvConnectionState> = _state.asStateFlow()

    private var webSocket: WebSocket? = null
    private var currentIp: String? = null
    private var triedFallback = false

    /** Encrypted client trusts the TV's self-signed cert; this client talks to nothing else. */
    private val secureClient: OkHttpClient by lazy { buildClient(trustAllCerts = true) }
    private val plainClient: OkHttpClient by lazy { buildClient(trustAllCerts = false) }

    /** Short-timeout client for the one-shot device-info GET, never reused for the socket itself. */
    private val infoClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeJob: Job? = null

    fun connect(ip: String) {
        wakeJob?.cancel()
        currentIp = ip
        triedFallback = false
        openSocket(ip, secure = true)
    }

    /**
     * Sends a Wake-on-LAN magic packet to the TV's saved MAC and polls until its network
     * stack answers, then hands off to the normal pairing/connect flow. There is nothing to
     * "connect" to until the TV's port is actually open, so we probe a raw TCP connection
     * first rather than repeatedly driving the websocket through failed handshakes.
     */
    fun wake() {
        val ip = currentIp ?: return
        val mac = prefs.macFor(ip)
        if (mac == null) {
            _state.value = TvConnectionState.Error(
                "Don't have this TV's MAC address yet — connect once while it's on to capture it.",
            )
            return
        }

        wakeJob?.cancel()
        wakeJob = scope.launch {
            _state.value = TvConnectionState.WakingUp

            runCatching { WakeOnLan.send(mac) }
                .onFailure { Log.w(TAG, "WOL send failed: ${it.message}") }

            var reachable = false
            var attempt = 0
            while (attempt < WAKE_RETRY_ATTEMPTS && _state.value is TvConnectionState.WakingUp) {
                delay(WAKE_RETRY_INTERVAL_MS)
                if (isPortOpen(ip)) {
                    reachable = true
                    break
                }
                attempt++
            }

            if (_state.value !is TvConnectionState.WakingUp) return@launch // superseded meanwhile

            if (reachable) {
                openSocket(ip, secure = true)
            } else {
                _state.value = TvConnectionState.Error(
                    "TV didn't respond to the wake signal. Make sure \"Power on with mobile\" " +
                        "is on under the TV's Network settings.",
                )
            }
        }
    }

    private fun isPortOpen(ip: String): Boolean =
        runCatching {
            Socket().use { it.connect(InetSocketAddress(ip, 8002), WAKE_PROBE_TIMEOUT_MS) }
            true
        }.getOrDefault(false)

    fun sendKey(key: RemoteKey) {
        val socket = webSocket ?: run {
            Log.w(TAG, "sendKey(${key.code}) dropped: not connected")
            return
        }
        val payload = JSONObject().apply {
            put("method", "ms.remote.control")
            put(
                "params",
                JSONObject().apply {
                    put("Cmd", "Click")
                    put("DataOfCmd", key.code)
                    put("Option", "false")
                    put("TypeOfRemote", "SendRemoteKey")
                },
            )
        }
        socket.send(payload.toString())
    }

    /** User chose to leave this TV (e.g. "Switch TV") — a deliberate reset back to Idle. */
    fun disconnect() {
        wakeJob?.cancel()
        webSocket?.close(1000, "user disconnected")
        webSocket = null
        _state.value = TvConnectionState.Idle
    }

    /** Full teardown, called once when the owning ViewModel is cleared. */
    fun release() {
        disconnect()
        scope.cancel()
    }

    private fun openSocket(ip: String, secure: Boolean) {
        _state.value = if (prefs.tokenFor(ip) == null) {
            TvConnectionState.AwaitingPairing
        } else {
            TvConnectionState.Connecting
        }

        val name = Base64.encodeToString(REMOTE_APP_NAME.toByteArray(), Base64.NO_WRAP)
        val savedToken = prefs.tokenFor(ip)
        val scheme = if (secure) "wss" else "ws"
        val port = if (secure) 8002 else 8001
        val tokenParam = savedToken?.let { "&token=$it" } ?: ""
        val url = "$scheme://$ip:$port/api/v2/channels/samsung.remote.control?name=$name$tokenParam"

        val client = if (secure) secureClient else plainClient
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, Listener(ip, secure))
    }

    private inner class Listener(private val ip: String, private val secure: Boolean) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "socket open ($ip, secure=$secure)")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val json = runCatching { JSONObject(text) }.getOrNull() ?: return
            when (json.optString("event")) {
                "ms.channel.connect" -> {
                    val token = json.optJSONObject("data")?.optString("token")
                    if (!token.isNullOrBlank()) {
                        prefs.saveToken(ip, token)
                    }
                    _state.value = TvConnectionState.Connected
                    captureDeviceInfoIfNeeded(ip)
                }
                "ms.channel.timeOut" -> {
                    _state.value = TvConnectionState.Error("Pairing timed out. Try again and accept the prompt on your TV.")
                }
                "ms.error" -> {
                    prefs.clearToken(ip)
                    _state.value = TvConnectionState.Error(json.optString("data", "TV rejected the connection"))
                }
                else -> {
                    // Key-press acks and other channel chatter — nothing to do.
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "socket failure ($ip, secure=$secure): ${t.message}")
            if (secure && !triedFallback) {
                triedFallback = true
                openSocket(ip, secure = false)
            } else {
                _state.value = TvConnectionState.Error(t.message ?: "Could not reach the TV")
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (_state.value is TvConnectionState.Connected) {
                _state.value = TvConnectionState.Disconnected
            }
        }
    }

    /**
     * Reads the TV's small unauthenticated device-info document (the same one SmartThings
     * uses to identify the TV) to learn its MAC address and friendly name. The MAC is what
     * makes Wake-on-LAN possible later without asking the user to type it in; the name just
     * makes the remote screen say "Living Room TV" instead of a bare IP. No-ops once we
     * already have a MAC saved for this IP.
     */
    private fun captureDeviceInfoIfNeeded(ip: String) {
        if (prefs.macFor(ip) != null) return
        scope.launch {
            runCatching {
                val request = Request.Builder().url("http://$ip:$INFO_PORT/api/v2/").build()
                infoClient.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    val device = JSONObject(body).optJSONObject("device")
                    val mac = device?.optString("wifiMac")
                    if (!mac.isNullOrBlank()) {
                        prefs.saveMac(ip, mac)
                    }
                    val name = device?.optString("name")
                    if (!name.isNullOrBlank()) {
                        prefs.lastName = name
                    }
                }
            }.onFailure { Log.w(TAG, "Could not read TV device info: ${it.message}") }
        }
    }

    private fun buildClient(trustAllCerts: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // websockets stay open indefinitely
            .pingInterval(15, TimeUnit.SECONDS)

        if (trustAllCerts) {
            // The TV's port-8002 channel presents a self-signed certificate with no
            // public CA behind it — Samsung's own apps trust it unconditionally too.
            // This client is used exclusively for that one local-network TV
            // connection, never for any other request, so relaxing verification here
            // does not weaken anything else in the app.
            val trustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(trustManager), SecureRandom())
            }
            builder
                .sslSocketFactory(sslContext.socketFactory as SSLSocketFactory, trustManager)
                .hostnameVerifier { _, _ -> true }
        }

        return builder.build()
    }
}
