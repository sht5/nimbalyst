package com.tvremote.samsung

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tvremote.samsung.data.AndroidTvKey
import com.tvremote.samsung.data.AndroidTvPrefs
import com.tvremote.samsung.data.DeviceKind
import com.tvremote.samsung.data.RemoteKey
import com.tvremote.samsung.data.TvPrefs
import com.tvremote.samsung.network.DiscoveredTv
import com.tvremote.samsung.network.SamsungTvClient
import com.tvremote.samsung.network.TvConnectionState
import com.tvremote.samsung.network.TvDiscovery
import com.tvremote.samsung.network.androidtv.AndroidTvClient
import com.tvremote.samsung.network.androidtv.AndroidTvConnectionState
import com.tvremote.samsung.network.androidtv.AndroidTvDiscovery
import com.tvremote.samsung.network.androidtv.DiscoveredAndroidTv
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TvRemoteViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = TvPrefs(application)
    private val client = SamsungTvClient(prefs)

    val androidTvPrefs = AndroidTvPrefs(application)
    private val androidTvClient = AndroidTvClient(androidTvPrefs, application)

    val connectionState: StateFlow<TvConnectionState> = client.state
    val androidTvState: StateFlow<AndroidTvConnectionState> = androidTvClient.state

    private val _activeDevice = MutableStateFlow(DeviceKind.TV)
    val activeDevice: StateFlow<DeviceKind> = _activeDevice.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTv>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTv>> = _discoveredTvs.asStateFlow()

    private val _isDiscoveringAndroidTv = MutableStateFlow(false)
    val isDiscoveringAndroidTv: StateFlow<Boolean> = _isDiscoveringAndroidTv.asStateFlow()

    private val _discoveredAndroidTvs = MutableStateFlow<List<DiscoveredAndroidTv>>(emptyList())
    val discoveredAndroidTvs: StateFlow<List<DiscoveredAndroidTv>> = _discoveredAndroidTvs.asStateFlow()

    var connectedIp: String? = null
        private set

    var connectedAndroidTvIp: String? = null
        private set

    fun connect(ip: String) {
        val trimmed = ip.trim()
        if (trimmed.isEmpty()) return
        connectedIp = trimmed
        prefs.lastIp = trimmed
        client.connect(trimmed)
    }

    fun sendKey(key: RemoteKey) = client.sendKey(key)

    /**
     * The remote screen's single Power button: while connected it's a normal remote key, but
     * once the TV has dropped off the network there's nothing to send a key to — the only way
     * back on is a Wake-on-LAN packet (or, if we never captured a MAC for this TV, a plain
     * reconnect attempt in case it's just a network blip).
     */
    fun onPowerTap() {
        if (connectionState.value is TvConnectionState.Connected) {
            client.sendKey(RemoteKey.POWER)
            return
        }
        val ip = connectedIp ?: prefs.lastIp ?: return
        if (prefs.macFor(ip) != null) {
            client.wake()
        } else {
            connect(ip)
        }
    }

    /** True once we know how to Wake-on-LAN this TV, so the UI can offer that instead of a dead retry. */
    fun canWake(): Boolean {
        val ip = connectedIp ?: prefs.lastIp ?: return false
        return prefs.macFor(ip) != null
    }

    fun disconnect() {
        client.disconnect()
        connectedIp = null
    }

    fun discover() {
        if (_isDiscovering.value) return
        viewModelScope.launch {
            _isDiscovering.value = true
            _discoveredTvs.value = emptyList()
            _discoveredTvs.value = TvDiscovery.discover(getApplication())
            _isDiscovering.value = false
        }
    }

    // ---- Android TV (streamer) ----

    /** True once a streamer has been paired before, so the switcher can reconnect directly. */
    fun hasPairedAndroidTv(): Boolean {
        val ip = androidTvPrefs.lastIp ?: return false
        return androidTvPrefs.isPaired(ip)
    }

    /** Switches which device the remote screen's buttons address. Doesn't start pairing. */
    fun selectDevice(kind: DeviceKind) {
        if (kind == _activeDevice.value) return
        _activeDevice.value = kind
        if (kind == DeviceKind.ANDROID_TV) {
            // Mirrors pressing Source on the physical remote: follow the TV's HDMI input over
            // to whichever port the streamer is on. Only meaningful if the TV is actually up.
            if (connectionState.value is TvConnectionState.Connected) {
                client.sendKey(RemoteKey.SOURCE)
            }
            val ip = androidTvPrefs.lastIp
            val alreadyConnected = androidTvState.value is AndroidTvConnectionState.Connected
            if (ip != null && androidTvPrefs.isPaired(ip) && !alreadyConnected) {
                connectedAndroidTvIp = ip
                androidTvClient.connect(ip)
            }
        }
    }

    fun connectAndroidTv(ip: String) {
        val trimmed = ip.trim()
        if (trimmed.isEmpty()) return
        connectedAndroidTvIp = trimmed
        androidTvPrefs.lastIp = trimmed
        androidTvClient.connect(trimmed)
    }

    fun submitAndroidTvPairingCode(code: String) = androidTvClient.submitPairingCode(code)

    fun sendAndroidTvKey(key: AndroidTvKey) = androidTvClient.sendKey(key)

    fun discoverAndroidTv() {
        if (_isDiscoveringAndroidTv.value) return
        viewModelScope.launch {
            _isDiscoveringAndroidTv.value = true
            _discoveredAndroidTvs.value = emptyList()
            _discoveredAndroidTvs.value = AndroidTvDiscovery.discover(getApplication())
            _isDiscoveringAndroidTv.value = false
        }
    }

    override fun onCleared() {
        client.release()
        androidTvClient.release()
        super.onCleared()
    }
}
